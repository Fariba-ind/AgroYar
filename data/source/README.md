# Canonical AgroYar data source

The pesticide catalog used by the Android app is generated from the project's canonical Word document.

## Source of truth

Place the approved project Word document here as:

`data/source/AgroYar-source.docx`

The `.docx` is the human-maintained source. The Android app does **not** parse Word at runtime. Instead, the importer converts recognized Word tables to:

`app/src/main/assets/pesticides.json`

Run:

```bash
python tools/import_docx.py data/source/AgroYar-source.docx app/src/main/assets/pesticides.json
```

## Recognized fields

The importer recognizes Persian and English aliases for:

- scientific name
- trade name(s)
- active ingredient
- concentration / percentage
- formulation (EC, SC, WP, WG, SL, etc.)
- category
- target pest / disease / weed
- mode of action
- registered crops
- dose / application guidance
- restrictions / prohibited uses
- weather cautions
- water-stress cautions
- PHI / pre-harvest interval
- source status

The importer deliberately requires structured tables and at least two recognized columns. This prevents unrelated layout tables in Word from being silently interpreted as pesticide records.

## Data governance

Before a generated catalog is treated as field-use guidance, every dose, crop registration, PHI/REI, restriction, compatibility statement, and weather limitation should be traceable to the exact authoritative source used in the project Word document.
