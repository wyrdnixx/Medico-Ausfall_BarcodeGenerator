#!/bin/bash
cd "$(dirname "$0")"

JAVA_EXE="./jdk-25.0.2+10/bin/java"
CP=".:lib/*"

if [[ ! -x "$JAVA_EXE" ]]; then
    echo "Error: JDK not found or not executable at $JAVA_EXE"
    exit 1
fi

exec "$JAVA_EXE" -cp "$CP" Medico-Ausfall_BarcodeGenerator "$@"
