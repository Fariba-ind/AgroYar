#!/usr/bin/env python3
"""Validate AgroYar generated database assets before Android build."""

from __future__ import annotations

import base64
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"

DATASETS = {
    "pesticides": {
        "chunks": [
            "pesticides.b64.001",
            "pesticides.b64.002",
            "pesticides.b64.003",
            "pesticides.b64.004",
        ],
        "expected": 347,
        "required": {"id", "activeIngredient", "formulation"},
    },
    "recommendations": {
        "chunks": [
            "recommendations.b64.001",
            "recommendations.b64.002",
        ],
        "expected": 389,
        "required": {"id", "crop", "target", "recommendedPesticides", "dose", "timing"},
    },
}


def load_dataset(chunk_names: list[str]) -> list[dict]:
    encoded = "".join((ASSETS / name).read_text(encoding="ascii").strip() for name in chunk_names)
    compressed = base64.b64decode(encoded, validate=True)
    decoded = gzip.decompress(compressed).decode("utf-8")
    data = json.loads(decoded)
    if not isinstance(data, list):
        raise AssertionError("dataset root must be a JSON array")
    return data


def validate(name: str, spec: dict) -> None:
    data = load_dataset(spec["chunks"])
    expected = spec["expected"]
    if len(data) != expected:
        raise AssertionError(f"{name}: expected {expected} records, found {len(data)}")

    ids = [str(row.get("id", "")).strip() for row in data]
    if any(not value for value in ids):
        raise AssertionError(f"{name}: one or more records have an empty id")
    if len(set(ids)) != len(ids):
        raise AssertionError(f"{name}: duplicate ids detected")

    required = spec["required"]
    missing_columns = set()
    for row in data:
        missing_columns.update(required.difference(row.keys()))
    if missing_columns:
        raise AssertionError(f"{name}: missing required keys: {sorted(missing_columns)}")

    print(f"OK {name}: {len(data)} records")


def main() -> None:
    for name, spec in DATASETS.items():
        validate(name, spec)
    print("AgroYar database assets validated successfully.")


if __name__ == "__main__":
    main()
