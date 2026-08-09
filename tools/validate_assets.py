#!/usr/bin/env python3
"""Validate the Word-derived AgroYar runtime assets before Android build.

The current runtime package intentionally uses a validated preview subset while
full 347/389-record assets are rebuilt. The source totals remain documented so
CI cannot accidentally present the preview as the complete database.
"""

from __future__ import annotations

import base64
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"

DATASETS = {
    "pesticides-preview": {
        "assets": ["pesticides.preview.b64"],
        "expected": 15,
        "source_total": 347,
        "required": {"id", "activeIngredient", "formulation", "sourceStatus"},
    },
    "recommendations-preview": {
        "assets": ["recommendations.preview.b64"],
        "expected": 25,
        "source_total": 389,
        "required": {"id", "crop", "target", "recommendedPesticides", "dose", "timing", "sourceNotes"},
    },
}


def load_dataset(asset_names: list[str]) -> list[dict]:
    encoded = "".join(
        "".join((ASSETS / name).read_text(encoding="ascii").split())
        for name in asset_names
    )
    compressed = base64.b64decode(encoded, validate=True)
    decoded = gzip.decompress(compressed).decode("utf-8")
    data = json.loads(decoded)
    if not isinstance(data, list):
        raise AssertionError("dataset root must be a JSON array")
    return data


def validate(name: str, spec: dict) -> None:
    data = load_dataset(spec["assets"])
    expected = spec["expected"]
    if len(data) != expected:
        raise AssertionError(f"{name}: expected {expected} records, found {len(data)}")

    ids = [str(row.get("id", "")).strip() for row in data]
    if any(not value for value in ids):
        raise AssertionError(f"{name}: one or more records have an empty id")
    if len(set(ids)) != len(ids):
        raise AssertionError(f"{name}: duplicate ids detected")

    missing_columns = set()
    for row in data:
        missing_columns.update(spec["required"].difference(row.keys()))
    if missing_columns:
        raise AssertionError(f"{name}: missing required keys: {sorted(missing_columns)}")

    print(
        f"OK {name}: {len(data)} validated runtime records "
        f"from canonical source total {spec['source_total']}"
    )


def main() -> None:
    for name, spec in DATASETS.items():
        validate(name, spec)
    print("AgroYar preview database assets validated successfully.")


if __name__ == "__main__":
    main()
