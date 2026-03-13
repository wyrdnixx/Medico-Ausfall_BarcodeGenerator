# Medico-Ausfall_BarcodeGenerator

Dieses Tool durchsucht eine Ordnerstruktur mit Patienten-PDF-Dateien (OfflineDoku) und erzeugt pro Patient eine **Barcodes.pdf** mit druckfertigen Aufklebern. Jeder Aufkleber enthält einen CODE-128-Barcode mit der Fallnummer sowie Name, Geburtsdatum, Fallnummer und Station als lesbaren Text.

---

## Voraussetzungen

- Java-Runtime ist im Projektverzeichnis mitgeliefert:
  - **Linux:** `jdk-25.0.2_linux/`
  - **Windows:** `jdk-25.0.2_windows/`
- Benötigte Bibliotheken liegen im Ordner `lib/`

---

## Starten

### Linux
```bash
./start.sh
```

### Windows
```
start.bat
```

Die Skripte starten das Programm mit der mitgelieferten Java-Runtime. Eine alternative Konfigurationsdatei kann als Argument übergeben werden:
```bash
./start.sh meine-config.json
```

---

## Kompilieren

Nur notwendig, wenn der Quellcode geändert wurde:

```bash
# Linux
./jdk-25.0.2_linux/bin/javac -encoding UTF-8 -cp "lib/*" -d . Medico_Ausfall_BarcodeGenerator.java

# Windows
jdk-25.0.2_windows\bin\javac.exe -encoding UTF-8 -cp "lib\*" -d . Medico_Ausfall_BarcodeGenerator.java
```

---

## Konfiguration (config.json)

Alle Einstellungen werden in der `config.json` im Projektverzeichnis vorgenommen.

```json
{
  "offlinedokuPath": "files/OFFLINEDOKU",
  "barcodeType": "CODE_128",
  "barcodeWidth": 180,
  "barcodeHeight": 40,
  "barcodeDisplayHeight": 40,
  "pageSize": "A4",
  "barcodesPerPage": 40,
  "columns": 4,
  "rows": 10,
  "marginLeft": 0,
  "marginTop": 0,
  "marginRight": 0,
  "marginBottom": 0,
  "labelWidth": 148.82,
  "labelHeight": 84.19,
  "barcodeOutputFilename": "Barcodes.pdf",
  "nameFontSize": 8,
  "nameFontBold": true,
  "infoFontSize": 8,
  "infoFontBold": false,
  "errorLogPath": "errors.log"
}
```

| Parameter | Beschreibung |
|-----------|-------------|
| `offlinedokuPath` | Pfad zum Wurzelverzeichnis der OfflineDoku-Ordnerstruktur |
| `barcodeType` | Barcode-Format, z.B. `CODE_128`, `QR_CODE`, `EAN_13` |
| `barcodeWidth` | Breite des Barcodes in Pixeln (für die interne ZXing-Generierung) |
| `barcodeHeight` | Pixelhöhe bei der internen ZXing-Generierung (beeinflusst Qualität) |
| `barcodeDisplayHeight` | Anzeigehöhe des Barcodes im PDF in Punkten (1 pt ≈ 0,35 mm). Optional — Fallback auf `barcodeHeight` wenn nicht gesetzt. |
| `pageSize` | Seitengröße des PDFs, z.B. `A4` |
| `barcodesPerPage` | Anzahl der Etiketten pro Seite (wird durch `columns × rows` überschrieben) |
| `columns` | Anzahl der Spalten pro Seite |
| `rows` | Anzahl der Zeilen pro Seite |
| `marginLeft` | Linker Seitenrand in Punkten |
| `marginTop` | Oberer Seitenrand in Punkten |
| `marginRight` | Rechter Seitenrand in Punkten |
| `marginBottom` | Unterer Seitenrand in Punkten |
| `labelWidth` | Etikettenbreite in Punkten (52,5 mm = 148,82 pt) |
| `labelHeight` | Etikettenhöhe in Punkten (29,7 mm = 84,19 pt) |
| `barcodeOutputFilename` | Name der erzeugten PDF-Datei je Patient |
| `nameFontSize` | Schriftgröße für Name und Geburtsdatum |
| `nameFontBold` | Fettschrift für Name und Geburtsdatum (`true`/`false`) |
| `infoFontSize` | Schriftgröße für Fallnummer und Station |
| `infoFontBold` | Fettschrift für Fallnummer und Station (`true`/`false`) |
| `errorLogPath` | Pfad zur Fehler-Log-Datei (relativ oder absolut). Fehler werden mit Zeitstempel angehängt. Leer lassen um Logging zu deaktivieren. |

> **Tipp:** Umrechnung mm → Punkte: `mm × (72 / 25,4)`
> Beispiel: 52,5 mm × 2,8346 = **148,82 pt**

---

## Etikett-Aufbau

Jedes Etikett enthält:
1. **Barcode** (CODE-128) mit der Fallnummer
2. **Name + Geburtsdatum** — z.B. `Müller, Hans 01.09.1999`
3. **Fallnummer + Station** — z.B. `250066138 - F-10`

---

## Ordnerstruktur (Beispiel)

```
files/OFFLINEDOKU/
└── 0/
    └── F-10 (Pflegegruppe 10)/
        └── Mustermann,Max 01.09.1999/
            ├── Mustermann, Max-07.04.2011-250066138-...-OFFLINECOMMONDATA.pdf
            └── Barcodes.pdf   ← wird erzeugt
```

Die Fallnummer wird automatisch aus dem Dateinamen der Patienten-PDFs extrahiert (6–12-stellige Zahl). Die Stations-Bezeichnung wird aus dem übergeordneten Ordnernamen gelesen — Klammerausdrücke wie `(Pflegegruppe 10)` werden dabei automatisch entfernt.
