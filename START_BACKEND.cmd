@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "PQ_ROOT=%CD%"
echo.
echo ===== PrintQueue Phase 5 - Backend =====
echo.
set "JAVA_HOME=C:\Users\hp\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
 echo [ERROR] JDK 21 not found at %JAVA_HOME%
 echo Edit JAVA_HOME in START_BACKEND.cmd to your real JDK 21 folder.
 pause
 exit /b 1
)
set "PATH=%JAVA_HOME%\bin;C:\Tools\apache-maven-3.10.0\bin;%PATH%"
where mvn >nul 2>&1
if errorlevel 1 (
 echo [ERROR] Maven missing. Update Maven bin path in this script.
 pause
 exit /b 1
)
echo Checking PostgreSQL on port 5432...
powershell -NoProfile -Command "if (-not (Test-NetConnection 127.0.0.1 -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue)) { exit 1 }" >nul 2>&1
if errorlevel 1 (
 echo [ERROR] PostgreSQL not reachable on 5432.
 echo Open Docker Desktop and run docker compose up -d in your ORIGINAL database folder.
 pause
 exit /b 1
)
echo Checking whether backend port 8080 is already occupied...
powershell -NoProfile -Command "if (Test-NetConnection 127.0.0.1 -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) {exit 1}" >nul 2>&1
if errorlevel 1 (
 echo [ERROR] Port 8080 already occupied. Stop old backend first, then retry.
 pause
 exit /b 1
)
set "OWNER_USERNAME=owner"
echo Owner username: owner
set /p "OWNER_PASSWORD=Enter your owner password (visible while typing): "
if not defined OWNER_PASSWORD (
 echo [ERROR] Password cannot be blank.
 pause
 exit /b 1
)
set "OWNER_RESET_PASSWORD=true"
echo.
rem Use a stable storage folder shared across all future extracted project versions.
set "UPLOAD_DIR=%LOCALAPPDATA%\PrintQueue\uploads"
if not exist "%UPLOAD_DIR%" mkdir "%UPLOAD_DIR%"
echo File storage location: %UPLOAD_DIR%
echo IMPORTANT: Old orders require copying their original uploaded files here.
echo Starting Spring Boot. Leave this CMD window open.
echo For local development this run will update the owner's stored password.
echo No database tables or orders are deleted.
cd /d "%PQ_ROOT%\backend"
call mvn spring-boot:run
if errorlevel 1 echo [ERROR] Backend stopped or Maven failed. Read the error above.
pause
