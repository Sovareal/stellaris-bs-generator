@echo off
REM Bundle a minimal JRE for Stellaris BS Generator using jlink
REM Output: frontend\src-tauri\jre\

setlocal

set "OUTPUT_DIR=%~dp0..\frontend\src-tauri\jre"

REM Clean previous build
if exist "%OUTPUT_DIR%" (
    echo Removing previous JRE bundle...
    rmdir /s /q "%OUTPUT_DIR%"
)

REM Modules required by Spring Boot + our application
set MODULES=java.base,java.logging,java.sql,java.naming,java.management,java.instrument,java.desktop,java.net.http,java.security.jgss,java.compiler,java.datatransfer,java.prefs,java.rmi,java.scripting,java.xml,java.xml.crypto,jdk.unsupported,jdk.crypto.ec,jdk.zipfs

echo Creating minimal JRE with jlink...
echo Modules: %MODULES%

jlink ^
    --add-modules %MODULES% ^
    --output "%OUTPUT_DIR%" ^
    --strip-debug ^
    --no-man-pages ^
    --no-header-files ^
    --compress zip-6

if %ERRORLEVEL% neq 0 (
    echo ERROR: jlink failed with exit code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

echo.
echo JRE bundled successfully to: %OUTPUT_DIR%

REM Show size
for /f "tokens=3" %%a in ('dir /s "%OUTPUT_DIR%" ^| findstr "File(s)"') do set SIZE=%%a
echo Bundle size: %SIZE% bytes

endlocal
