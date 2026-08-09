#!/usr/bin/env python3
"""Convert AgroYar's canonical Word source into the Android JSON catalog.

The importer uses only Python's standard library. It reads tables from a .docx
file and maps Persian/English column headings to the pesticide schema used by
the Android app.

Usage:
    python tools/import_docx.py data/source/AgroYar-source.docx \
        app/src/main/assets/pesticides.json
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS}

FIELD_ALIASES = {
    "id": ["id", "شناسه"],
    "scientificName": ["scientific name", "نام علمی", "اسم علمی"],
    "tradeNames": ["trade name", "trade names", "brand name", "نام تجاری", "اسم تجاری"],
    "activeIngredient": ["active ingredient", "ماده موثره", "ماده مؤثره"],
    "concentration": [
        "concentration", "active concentration", "درصد ماده موثره", "درصد ماده مؤثره",
        "غلظت", "درصد/غلظت ماده موثره", "درصد/غلظت ماده مؤثره"
    ],
    "formulation": ["formulation", "فرمولاسیون", "نوع فرمولاسیون"],
    "category": ["category", "group", "گروه", "نوع سم"],
    "target": ["target", "target pest", "هدف", "هدف مصرف", "آفت هدف"],
    "modeOfAction": ["mode of action", "نحوه اثر", "مکانیسم اثر", "مکانیزم اثر"],
    "registeredCrops": [
        "registered crops", "crops", "محصولات مجاز", "محصولات قابل استفاده",
        "در چه محصولاتی قابل استفاده است", "محصول"
    ],
    "doseGuidance": [
        "dose", "rate", "dose and application", "دز", "دوز", "دز مصرف", "دوز مصرف",
        "دز و نحوه مصرف", "میزان مصرف"
    ],
    "restrictions": [
        "restrictions", "prohibited uses", "محدودیت", "محدودیت ها", "محدودیت‌ها",
        "منع مصرف", "موارد منع مصرف"
    ],
    "weatherCautions": [
        "weather cautions", "weather", "شرایط آب و هوایی", "شرایط آب‌وهوایی",
        "محدودیت آب و هوایی"
    ],
    "waterStressCautions": [
        "water stress", "water-stress cautions", "تنش آبی", "تنش خشکی"
    ],
    "phi": [
        "phi", "pre harvest interval", "pre-harvest interval", "کارنس", "دوره کارنس",
        "فاصله تا برداشت"
    ],
    "sourceStatus": ["source", "source status", "منبع", "وضعیت منبع"],
}

REQUIRED_FOR_RECORD = ("scientificName", "tradeNames", "activeIngredient")


def normalize(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "")
    value = value.replace("ي", "ی").replace("ك", "ک")
    value = value.replace("\u200c", " ")
    value = re.sub(r"[^\w\u0600-\u06ff%/+-]+", " ", value, flags=re.UNICODE)
    return re.sub(r"\s+", " ", value).strip().casefold()


NORMALIZED_ALIASES = {
    field: {normalize(alias) for alias in aliases}
    for field, aliases in FIELD_ALIASES.items()
}


def text_from_cell(cell: ET.Element) -> str:
    paragraphs = []
    for paragraph in cell.findall(".//w:p", NS):
        chunks = [node.text or "" for node in paragraph.findall(".//w:t", NS)]
        text = "".join(chunks).strip()
        if text:
            paragraphs.append(text)
    return "\n".join(paragraphs).strip()


def read_tables(docx_path: Path) -> list[list[list[str]]]:
    with zipfile.ZipFile(docx_path) as archive:
        xml = archive.read("word/document.xml")
    root = ET.fromstring(xml)
    tables: list[list[list[str]]] = []
    for table in root.findall(".//w:tbl", NS):
        rows: list[list[str]] = []
        for row in table.findall("./w:tr", NS):
            cells = [text_from_cell(cell) for cell in row.findall("./w:tc", NS)]
            if any(cell.strip() for cell in cells):
                rows.append(cells)
        if rows:
            tables.append(rows)
    return tables


def map_header(header: list[str]) -> dict[int, str]:
    mapping: dict[int, str] = {}
    for index, raw in enumerate(header):
        key = normalize(raw)
        if not key:
            continue
        best_field = None
        best_score = 0
        for field, aliases in NORMALIZED_ALIASES.items():
            for alias in aliases:
                if key == alias:
                    best_field, best_score = field, 3
                    break
                if alias and (alias in key or key in alias) and best_score < 2:
                    best_field, best_score = field, 2
            if best_score == 3:
                break
        if best_field:
            mapping[index] = best_field
    return mapping


def split_trade_names(value: str) -> list[str]:
    return [
        part.strip()
        for part in re.split(r"[;؛,،\n]+", value or "")
        if part.strip()
    ]


def slugify(value: str, fallback: str) -> str:
    slug = normalize(value)
    slug = re.sub(r"\s+", "-", slug)
    slug = re.sub(r"[^\w\u0600-\u06ff-]+", "", slug, flags=re.UNICODE).strip("-")
    return slug or fallback


def record_from_row(row: list[str], mapping: dict[int, str], ordinal: int) -> dict[str, object]:
    record: dict[str, object] = {field: "" for field in FIELD_ALIASES}
    record["tradeNames"] = []
    for index, field in mapping.items():
        value = row[index].strip() if index < len(row) else ""
        if field == "tradeNames":
            record[field] = split_trade_names(value)
        else:
            record[field] = value
    if not record["id"]:
        seed = str(record["scientificName"] or (record["tradeNames"][0] if record["tradeNames"] else ""))
        record["id"] = slugify(seed, f"word-record-{ordinal}")
    if not record["sourceStatus"]:
        record["sourceStatus"] = "Imported from canonical AgroYar Word source"
    return record


def meaningful(record: dict[str, object]) -> bool:
    return any(record.get(field) for field in REQUIRED_FOR_RECORD)


def extract_records(docx_path: Path) -> list[dict[str, object]]:
    records: list[dict[str, object]] = []
    tables = read_tables(docx_path)
    for table_index, rows in enumerate(tables, start=1):
        if len(rows) < 2:
            continue
        mapping = map_header(rows[0])
        if not mapping:
            continue
        # Require at least two recognized columns so arbitrary formatting tables
        # are not mistaken for pesticide data.
        if len(mapping) < 2:
            continue
        for row_index, row in enumerate(rows[1:], start=1):
            record = record_from_row(row, mapping, len(records) + 1)
            if meaningful(record):
                record["sourceStatus"] = (
                    f"Imported from canonical AgroYar Word source; table {table_index}, row {row_index}"
                )
                records.append(record)
    return records


def main() -> int:
    if len(sys.argv) not in (2, 3):
        print("Usage: import_docx.py SOURCE.docx [OUTPUT.json]", file=sys.stderr)
        return 2

    source = Path(sys.argv[1])
    output = Path(sys.argv[2]) if len(sys.argv) == 3 else Path("app/src/main/assets/pesticides.json")

    if not source.exists():
        print(f"Source file not found: {source}", file=sys.stderr)
        return 2
    if source.suffix.lower() != ".docx":
        print("Source must be a .docx file", file=sys.stderr)
        return 2

    try:
        records = extract_records(source)
    except (zipfile.BadZipFile, KeyError, ET.ParseError) as exc:
        print(f"Could not read DOCX: {exc}", file=sys.stderr)
        return 1

    if not records:
        print(
            "No pesticide records were found in recognized Word tables. "
            "Inspect the document headings and extend FIELD_ALIASES if needed.",
            file=sys.stderr,
        )
        return 1

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps(records, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Imported {len(records)} records -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
