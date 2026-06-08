@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "APP_JAR=%SCRIPT_DIR%quarkus-run.jar"
set "RUNNING_FILE=%SCRIPT_DIR%running"

echo =================
echo B I M R O C K E T
echo =================
echo(

REM ------------------------------------
REM 1. Check existing running file
REM ------------------------------------

if exist "%RUNNING_FILE%" (
    echo Application is already running:
    type %RUNNING_FILE%
    exit /b 1
)

REM --------------------------------------------------
REM 2. Setup JVM
REM --------------------------------------------------

call .\jvm.cmd

if "%JAVA_EXEC%"=="" (
    echo Java not installed^.
    exit /b 1
)

REM ------------------------------------
REM 3. Start application
REM ------------------------------------

echo Starting application^.^.^.

REM Start Quarkus in background (no wait)
powershell -command "Start-Process '%JAVA_EXEC%' -ArgumentList @('-jar','%APP_JAR%') -WindowStyle Hidden -PassThru " >nul

REM ------------------------------------
REM 4. Display startup info from Quarkus
REM ------------------------------------

set "TIMEOUT=60"
set "INTERVAL=1"
set "ELAPSED=0"

:wait_loop
if exist "%RUNNING_FILE%" goto file_found

if %ELAPSED% GEQ %TIMEOUT% goto timeout

timeout /t %INTERVAL% /nobreak > nul
set /a ELAPSED+=INTERVAL

goto wait_loop

:file_found
echo Application started^.
type "%RUNNING_FILE%"
goto end

:timeout
exit /b 1

:end
endlocal