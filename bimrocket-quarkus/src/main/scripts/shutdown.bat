@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "RUNNING_FILE=%SCRIPT_DIR%running"
set "TIMEOUT=30"

if not exist "%RUNNING_FILE%" (
    echo Is application running?
    exit /b 1
)

del "!RUNNING_FILE!"

echo Application stopped^.

endlocal