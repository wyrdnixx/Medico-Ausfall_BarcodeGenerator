# [Medico-Ausfall_BarcodeGenerator]

[Generiert aus einer Dateistruktur von Patientendaten ein Barcode.pdf die als Aufkleber gedruckt werden können]

## Tech Layers
- **Framework**: [java]
- **Sprache**: [java]

## Projektstruktur
```
➜  Medico-Ausfall_BarcodeGenerator git:(main) ✗ tree
├── config.json                         # Konfigurationsdatei
├── files                               # Beispiel Ordnerstruktur mit Patientendateien
│   └── OFFLINEDOKU
│       ├── 0
│       │   ├── F-10 (Pflegegruppe 10)
│       │   │   └── Mustermann,Max 01.09.1999
│       │   │       ├── Barcodes.pdf
│       │   │       └── Mustermann, Max -07.04.2011-250066138-Allgemeine Patientendaten-OFFLINECOMMONDATA.pdf
│       │   └── F-12 (Pflegegruppe 12)
│       │       └── Musterfrau, Susi-31.03.1955
│       │           ├── Susi, Musterfrau-07.04.2011-250012345-Allgemeine BLA-OFFLINECOMMONDATA.pdf

│       │           └── Barcodes.pdf
│       └── 2
│           └── T-1
│               └── X,b 83.34.1344
│                   ├── Barcodes.pdf
│                   ├── X,b-07.04.2011-252077123-Allgemeine BLA-OFFLINECOMMONDATA.pdf
│                   └── X,b-07.04.2011-252077123-Allgemeine Patientendaten-OFFLINECOMMONDATA.pdf
├── Medico_Ausfall_BarcodeGenerator.class       # Compilierte Datei
├── Medico_Ausfall_BarcodeGenerator.java        # Sourcecode
├── README.md                                   # Readme Datei
├── start.bat                                   # Batch Datei zum start des Java Programmes unter Windows
└── start.sh                                    # Batch Datei zum start des Java Programmes unter Linux
├── lib                                         # benötigte java librarys
├── jdk-25.0.2_windows                          # java compiler und runtimes für Windows
├── jdk-25.0.2_linux                            # java compiler und runtimes für Windows


```

## Entwicklung
```bash
[build]          # Production Build
./jdk-25.0.2+10/bin/javac -encoding UTF-8 -cp "lib/*" -d . Medico_Ausfall_BarcodeGenerator.java

[run]           # programm starten
./jdk-25.0.2+10/bin/java -cp ".:lib/*" Medico_Ausfall_BarcodeGenerator
```

## Projektspezifische Regeln
- [Regel 1, es sollen immer die java runtimes aus dem mitgelieferten Verzeichnis verwendet werden."]
- [Regel 2, verändere nie die Patienten-PDF Dateien]
- [Regel 3, beschreibe den Code mit Kommentaren]
