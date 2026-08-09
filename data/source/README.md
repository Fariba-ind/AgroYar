# Canonical AgroYar data source

AgroYar's pesticide data is generated from the project Word document:

**بانک پایه آفتکش‌های توصیه‌شده در ایران**  
Compiled: 15 Mordad 1405 / 6 August 2026.

The current imported document contains two structured layers used by the project:

- **347** active ingredients or mixtures in the alphabetical catalog.
- **389** detailed crop / pest-disease-weed recommendation rows.

The document itself reports broader coverage of **486 trade-name/formulation records**.

## Source of truth

The approved `.docx` remains the human-readable canonical source. Because this repository is public, the Word file is intentionally excluded by `.gitignore`; generated app assets are committed instead.

The Android application does **not** parse Word at runtime. Run the reproducible importer whenever the approved Word bank changes:

```bash
python tools/import_docx.py data/source/AgroYar-source.docx app/src/main/assets
```

The importer writes gzip-compressed, base64 text chunks:

- `pesticides.b64.*` — searchable material catalog.
- `recommendations.b64.*` — detailed recommendation rows.

## Catalog fields

For each material the importer preserves or derives only what the Word tables support:

- Persian/common active-ingredient name
- Latin/English name when supplied
- trade names
- formulation and concentration
- pesticide category
- resistance group / mode of action
- crops and targets present in source recommendations
- the complete source recommendation text containing crop-specific dose, formulation and timing
- mixing/restriction warning text
- WHO/LD50/source-page/status metadata where present

Separate weather, water-stress, and PHI fields remain empty when the Word table does not provide dedicated values. The importer does not invent them.

The detailed layer preserves the Word columns for crop, target pest/disease/weed, recommended pesticide, formulation, dose, application timing, source notes, and PDF page.

## Safety and legal status

The Word document defines itself as a research/base bank, not a replacement for the official product label, a plant-protection prescription, or the latest decisions of Iran's pesticide supervisory authority. Before field use, trade name, concentration, crop, target, dose, PHI, re-entry interval, and registration status must be checked against the current product label and official system.

The import pipeline therefore preserves source wording and does not silently fill missing agronomic or regulatory fields from general knowledge.
