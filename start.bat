@echo off
setlocal
cd /d "%~dp0"

set "JAVA_EXE=%~dp0jdk-25.0.2_windows\bin\java.exe"
set "CP=.;lib\*"

if not exist "%JAVA_EXE%" (
    echo Error: JDK not found at jdk-25.0.2_windows\bin\java.exe
    pause
    exit /b 1
)

"%JAVA_EXE%" -cp "%CP%" Medico_Ausfall_BarcodeGenerator %*
pause
