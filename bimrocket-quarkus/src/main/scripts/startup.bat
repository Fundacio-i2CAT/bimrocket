@echo off
setlocal enabledelayedexpansion

REM --------------------------------------------------
REM Resolve script directories
REM --------------------------------------------------

set "SCRIPT_DIR=%~dp0"
set "APP_JAR=%SCRIPT_DIR%quarkus-run.jar"
set "RUNNING_FILE=%SCRIPT_DIR%running"
set "LOCAL_JVM_DIR=%SCRIPT_DIR%jvm"

REM Default Java version (LTS)
if "%JAVA_VERSION%"=="" set "JAVA_VERSION=21"

echo =================
echo B I M R O C K E T
echo =================
echo(

REM --------------------------------------------------
REM 1. Check existing running process
REM --------------------------------------------------

if exist "%RUNNING_FILE%" (
    echo Application is already running:
    type %RUNNING_FILE%
    exit /b 1
)

REM --------------------------------------------------
REM 2. Resolve Java executable
REM --------------------------------------------------

set "JAVA_EXEC="

REM Check bundled JVM first
if exist "%LOCAL_JVM_DIR%\bin\java.exe" (
    echo Local JVM found in %LOCAL_JVM_DIR%^, using it^.
    set "JAVA_EXEC=%LOCAL_JVM_DIR%\bin\java.exe"
) else (

    echo Checking system Java^.^.^.

    where java >nul 2>&1

    if not errorlevel 1 (

        for /f "tokens=3 delims== " %%v in (
            'java -XshowSettings:properties -version 2^>^&1 ^| findstr "java.specification.version"'
        ) do (
            set "SYS_JAVA_VER=%%v"
        )

        REM Remove spaces
        set "SYS_JAVA_VER=!SYS_JAVA_VER: =!"

        REM Convert 1.8 -> 8 if needed
        if "!SYS_JAVA_VER:~0,2!"=="1." (
            set "SYS_JAVA_VER=!SYS_JAVA_VER:~2!"
        )

        if defined SYS_JAVA_VER (
            if !SYS_JAVA_VER! EQU 21 (
                echo System Java found ^(version !SYS_JAVA_VER!^)^. Using system Java^.
                set "JAVA_EXEC=java"
            ) else (
                echo System Java version ^(!SYS_JAVA_VER!^) is not supported ^(requires 21^).
            )
        )
    ) else (
        echo Java is not available in system PATH^.
    )

    REM --------------------------------------------------
    REM 3. Download JVM only if required
    REM --------------------------------------------------

    if "!JAVA_EXEC!"=="" (

        echo No valid Java found^. Preparing JVM download^.^.^.

        REM Detect architecture
        set "ARCH=%PROCESSOR_ARCHITECTURE%"

        if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
            set "ARCH=x64"
        )

        if /I "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
            set "ARCH=aarch64"
        )

        echo Resolving Temurin JVM for windows/!ARCH! ^(Java %JAVA_VERSION%^)^.^.^.

        REM Query Adoptium API
        powershell -NoProfile -Command ^
            "$ProgressPreference='SilentlyContinue';" ^
            "$url='https://api.adoptium.net/v3/assets/latest/%JAVA_VERSION%/hotspot?image_type=jdk^&os=windows^&architecture=!ARCH!^&heap_size=normal^&archive_type=zip';" ^
            "$json=Invoke-RestMethod -Uri $url;" ^
            "$json[0].binary.package.link" > "%TEMP%\jvm_url.txt"

        set /p JVM_DOWNLOAD_URL=<"%TEMP%\jvm_url.txt"

        if "!JVM_DOWNLOAD_URL!"=="" (
            echo Error^: Unable to resolve JVM download URL
            exit /b 1
        )

        echo Downloading JVM from^:
        echo !JVM_DOWNLOAD_URL!

        if not exist "%LOCAL_JVM_DIR%" (
            mkdir "%LOCAL_JVM_DIR%"
        )

        set "JVM_ZIP=%TEMP%\temurin-jdk.zip"

        REM Download JVM ZIP
        powershell -NoProfile -Command ^
            "Start-BitsTransfer -Source '!JVM_DOWNLOAD_URL!' -Destination '!JVM_ZIP!'"

        REM Extract ZIP
        powershell -NoProfile -Command ^
            "Expand-Archive -Path '!JVM_ZIP!' -DestinationPath '%LOCAL_JVM_DIR%-tmp' -Force"

        REM Move extracted folder contents
        for /d %%d in ("%LOCAL_JVM_DIR%-tmp\*") do (
            xcopy "%%d\*" "%LOCAL_JVM_DIR%\" /E /I /Y >nul
        )

        REM Cleanup
        rmdir /S /Q "%LOCAL_JVM_DIR%-tmp"
        del "!JVM_ZIP!"

        if exist "%LOCAL_JVM_DIR%\bin\java.exe" (
            echo JVM successfully downloaded and installed^.
            set "JAVA_EXEC=%LOCAL_JVM_DIR%\bin\java.exe"
        ) else (
            echo Error^: Unexpected JVM archive structure^.
            exit /b 1
        )
    )
)

REM ------------------------------------
REM 4. Start application
REM ------------------------------------
echo Starting application^.^.^.

REM Start Quarkus in background (no wait)
powershell -command "Start-Process '%JAVA_EXEC%' -ArgumentList @('-jar','%APP_JAR%') -WindowStyle Hidden -PassThru " >nul

REM ------------------------------------
REM 5. Display startup info from Quarkus
REM ------------------------------------

set "TIMEOUT=20"
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