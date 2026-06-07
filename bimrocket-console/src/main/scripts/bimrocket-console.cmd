@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "APP_JAR=%SCRIPT_DIR%bimrocket-console.jar"

REM --------------------------------------------------
REM 1. Setup JVM
REM --------------------------------------------------

call .\jvm.cmd

REM ------------------------------------
REM 2. Start application
REM ------------------------------------

%JAVA_EXEC% -Xmx4g -jar %APP_JAR%
