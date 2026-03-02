@echo off
setlocal
cd /d "%~dp0"

set "JAVA_EXE=%~dp0jdk-25.0.2+10\bin\java.exe"
set "CP=.;lib\*"

if not exist "%JAVA_EXE%" (
    echo Error: JDK not found at jdk-25.0.2+10\bin\java.exe
    pause
    exit /b 1
)

"%JAVA_EXE%" -cp "%CP%" Medico-Ausfall_BarcodeGenerator %*
pause
