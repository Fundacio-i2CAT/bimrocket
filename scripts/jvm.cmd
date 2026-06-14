REM BIMROCKET-JVM

REM Default Java LTS version
set "JAVA_VERSION=21"

REM Local JVM path
set "LOCAL_JVM_DIR=%USERPROFILE%\bimrocket-jvm-%JAVA_VERSION%"

set "JAVA_EXEC="

REM --------------------------------------------------
REM 1. Check bundled JVM first
REM --------------------------------------------------

if exist "%LOCAL_JVM_DIR%\bin\java.exe" (
    echo Local JVM found in %LOCAL_JVM_DIR%^, using it^.
    set "JAVA_EXEC=%LOCAL_JVM_DIR%\bin\java.exe"
) else (

REM --------------------------------------------------
REM 2. Check System JVM
REM --------------------------------------------------

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
            if !SYS_JAVA_VER! EQU !JAVA_VERSION! (
                echo System Java found ^(version !SYS_JAVA_VER!^)^. Using system Java^.
                set "JAVA_EXEC=java"
            ) else (
                echo System Java version ^(!SYS_JAVA_VER!^) is not supported ^(requires !JAVA_VERSION!^).
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
            "$url='https://api.adoptium.net/v3/assets/latest/%JAVA_VERSION%/hotspot?image_type=jre^&os=windows^&architecture=!ARCH!^&heap_size=normal^&archive_type=zip';" ^
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
        curl -L "!JVM_DOWNLOAD_URL!" -o "!JVM_ZIP!"

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