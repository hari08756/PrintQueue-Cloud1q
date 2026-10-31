@echo off
setlocal
cd /d "%~dp0dashboard"
where npm >nul 2>&1
if errorlevel 1 (
 echo [ERROR] npm is not installed or missing from PATH.
 pause
 exit /b 1
)
if not exist node_modules call npm install
if errorlevel 1 (
 echo [ERROR] npm install failed.
 pause
 exit /b 1
)
call npm run dev
pause
