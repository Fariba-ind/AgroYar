#!/usr/bin/env python3
"""Build AgroYar Android data assets from the canonical Word document.

The canonical Word bank contains two structured data layers:
1. the active-ingredient / mixture catalog (one row per material), and
2. the detailed crop-target recommendation table.

This importer deliberately preserves source text instead of inventing missing
PHI, weather, water-stress, incompatibility, dose, or registration data.
Generated JSON is gzip-compressed, base64-encoded, and split into text chunks
that Android can package as assets.

Usage:
    python tools/import_docx.py SOURCE.docx [ASSETS_DIR]

Default ASSETS_DIR: app/src/main/assets
"""

from __future__ import annotations

import base64
import gzip
import json
import re
import sys
import unicodedata
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS}
CHUNK_SIZE = 60_000


def clean_text(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "")
    value = value.replace("ي", "ی").replace("ك", "ک")
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r" *\n *", "\n", value)
    return value.strip()


def normalized(value: str) -> str:
    value = clean_text(value).replace("\u200c", " ")
    value = re.sub(r"\s+", " ", value)
    return value.casefold().strip()


def text_from_cell(cell: ET.Element) -> str:
    paragraphs: list[str] = []
    for paragraph in cell.findall(".//w:p", NS):
        chunks = [node.text or "" for node in paragraph.findall(".//w:t", NS)]
        text = clean_text("".join(chunks))
        if text:
            paragraphs.append(text)
    return clean_text("\n".join(paragraphs))


def read_tables(docx_path: Path) -> list[list[list[str]]]:
    with zipfile.ZipFile(docx_path) as archive:
        xml = archive.read("word/document.xml")
    root = ET.fromstring(xml)
    tables: list[list[list[str]]] = []
    for table in root.findall(".//w:tbl", NS):
        rows: list[list[str]] = []
        for row in table.findall("./w:tr", NS):
            cells = [text_from_cell(cell) for cell in row.findall("./w:tc", NS)]
            if any(cell for cell in cells):
                rows.append(cells)
        if rows:
            tables.append(rows)
    return tables


def find_table(tables: list[list[list[str]]], required_headers: list[str]) -> list[list[str]]:
    required = [normalized(item) for item in required_headers]
    for table in tables:
        if not table:
            continue
        header = " | ".join(normalized(cell) for cell in table[0])
        if all(item in header for item in required):
            return table
    raise ValueError(f"Could not find table with headers: {required_headers}")


def unique(values: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        value = clean_text(value)
        key = normalized(value)
        if value and key not in seen:
            seen.add(key)
            result.append(value)
    return result


def marker_slice(text: str, start_pattern: str, end_patterns: tuple[str, ...] = ()) -> str:
    start = re.search(start_pattern, text, flags=re.IGNORECASE)
    if not start:
        return ""
    end_index = len(text)
    tail = text[start.end():]
    for pattern in end_patterns:
        match = re.search(pattern, tail, flags=re.IGNORECASE)
        if match:
            end_index = min(end_index, start.end() + match.start())
    return clean_text(text[start.end():end_index])


def parse_identity(cell: str, ordinal: int) -> tuple[str, str, str]:
    text = clean_text(cell)
    row_match = re.match(r"^\s*(\d+)\s*[.．]?\s*", text)
    row_number = row_match.group(1) if row_match else str(ordinal)
    body = text[row_match.end():] if row_match else text

    latin_match = re.search(r"Latin\s*/\s*English\s*:\s*(.*)$", body, flags=re.I | re.S)
    if latin_match:
        native = clean_text(body[:latin_match.start()])
        latin = clean_text(latin_match.group(1))
        if latin in {"—", "-"}:
            latin = ""
    else:
        native = clean_text(body)
        latin = ""

    scientific = latin or native
    return row_number, native, scientific


def parse_product_cell(cell: str) -> tuple[str, list[str], str, str]:
    text = clean_text(cell)
    category = marker_slice(
        text,
        r"طبقه\s*:\s*",
        (r"نام(?:\u200c|\s)*های\s*تجاری\s*:", r"فرمولاسیون(?:\u200c|\s)*ها\s*:")
    )
    trade_raw = marker_slice(
        text,
        r"نام(?:\u200c|\s)*های\s*تجاری\s*:\s*",
        (r"فرمولاسیون(?:\u200c|\s)*ها\s*:",)
    )
    formulation = marker_slice(text, r"فرمولاسیون(?:\u200c|\s)*ها\s*:\s*")

    if normalized(trade_raw).startswith("نام تجاری در منبع درج نشده"):
        trade_names: list[str] = []
    else:
        trade_names = unique(re.split(r"[،,؛;\n]+", trade_raw))

    concentrations = unique(
        re.findall(r"\d+(?:[./]\d+)?\s*%", formulation)
    )
    concentration = "، ".join(concentrations)
    return category, trade_names, formulation, concentration


def parse_usage_summary(cell: str) -> tuple[str, str]:
    text = clean_text(cell)
    crops: list[str] = []
    targets: list[str] = []

    # Each use normally starts with: 1) crop | target | dose ...
    starts = list(re.finditer(r"(?m)(?:^|\n)\s*\d+\s*\)\s*", text))
    for index, match in enumerate(starts):
        start = match.end()
        end = starts[index + 1].start() if index + 1 < len(starts) else len(text)
        block = clean_text(text[start:end])
        parts = [clean_text(part) for part in block.split("|")]
        if parts and parts[0]:
            crops.append(parts[0])
        if len(parts) > 1 and parts[1]:
            targets.append(parts[1])

    return "، ".join(unique(crops)), "؛ ".join(unique(targets))


def extract_catalog(table: list[list[str]]) -> list[dict[str, object]]:
    records: list[dict[str, object]] = []
    for ordinal, row in enumerate(table[1:], start=1):
        if len(row) < 6 or not any(clean_text(cell) for cell in row):
            continue

        row_number, native_name, scientific_name = parse_identity(row[0], ordinal)
        category, trade_names, formulation, concentration = parse_product_cell(row[1])
        registered_crops, targets = parse_usage_summary(row[3])

        records.append({
            "id": f"word-{row_number}",
            "scientificName": scientific_name,
            "tradeNames": trade_names,
            "activeIngredient": native_name,
            "concentration": concentration,
            "formulation": clean_text(formulation),
            "category": clean_text(category),
            "target": targets,
            "modeOfAction": clean_text(row[2]),
            "registeredCrops": registered_crops,
            # Preserve the complete source column so crop-specific rate,
            # formulation and timing are not detached from one another.
            "doseGuidance": clean_text(row[3]),
            "restrictions": clean_text(row[4]),
            # These are intentionally blank unless the Word bank supplies
            # dedicated fields. The importer does not infer them.
            "weatherCautions": "",
            "waterStressCautions": "",
            "phi": "",
            "sourceStatus": clean_text(row[5]),
        })
    return records


def extract_recommendations(table: list[list[str]]) -> list[dict[str, str]]:
    records: list[dict[str, str]] = []
    for ordinal, row in enumerate(table[1:], start=1):
        cells = [clean_text(cell) for cell in row]
        if not any(cells):
            continue
        cells += [""] * (9 - len(cells))
        source_id = cells[0] or str(ordinal)
        records.append({
            "id": f"recommendation-{source_id}",
            "crop": cells[1],
            "target": cells[2],
            "recommendedPesticides": cells[3],
            "formulation": cells[4],
            "dose": cells[5],
            "timing": cells[6],
            "sourceNotes": cells[7],
            "pdfPage": cells[8],
        })
    return records


def write_chunked_asset(records: list[dict[str, object]], assets_dir: Path, prefix: str) -> list[Path]:
    raw = (json.dumps(records, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    compressed = gzip.compress(raw, compresslevel=9, mtime=0)
    encoded = base64.b64encode(compressed).decode("ascii")

    assets_dir.mkdir(parents=True, exist_ok=True)
    for stale in assets_dir.glob(f"{prefix}.b64.*"):
        stale.unlink()

    paths: list[Path] = []
    for index, offset in enumerate(range(0, len(encoded), CHUNK_SIZE), start=1):
        path = assets_dir / f"{prefix}.b64.{index:03d}"
        path.write_text(encoded[offset:offset + CHUNK_SIZE], encoding="ascii")
        paths.append(path)
    return paths


def main() -> int:
    if len(sys.argv) not in (2, 3):
        print("Usage: import_docx.py SOURCE.docx [ASSETS_DIR]", file=sys.stderr)
        return 2

    source = Path(sys.argv[1])
    assets_dir = Path(sys.argv[2]) if len(sys.argv) == 3 else Path("app/src/main/assets")

    if not source.exists() or source.suffix.lower() != ".docx":
        print(f"Canonical DOCX not found: {source}", file=sys.stderr)
        return 2

    try:
        tables = read_tables(source)
        catalog_table = find_table(
            tables,
            ["ردیف و نام عمومی/علمی", "طبقه، نام تجاری و فرمولاسیون", "نحوه اثر و گروه مقاومت"]
        )
        recommendation_table = find_table(
            tables,
            ["محصول", "آفت/بیماری/علف هرز", "آفت کش های توصیه شده", "دوز/میزان مصرف"]
        )
        catalog = extract_catalog(catalog_table)
        recommendations = extract_recommendations(recommendation_table)
    except (zipfile.BadZipFile, KeyError, ET.ParseError, ValueError) as exc:
        print(f"Could not import canonical Word source: {exc}", file=sys.stderr)
        return 1

    if not catalog or not recommendations:
        print("Canonical Word tables were found but yielded no data.", file=sys.stderr)
        return 1

    catalog_paths = write_chunked_asset(catalog, assets_dir, "pesticides")
    recommendation_paths = write_chunked_asset(recommendations, assets_dir, "recommendations")

    print(f"Imported {len(catalog)} active-ingredient/mixture records")
    print(f"Imported {len(recommendations)} detailed recommendations")
    print(f"Catalog chunks: {len(catalog_paths)}")
    print(f"Recommendation chunks: {len(recommendation_paths)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
