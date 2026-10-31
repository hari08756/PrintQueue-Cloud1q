@echo off
setlocal EnableExtensions
set "DEST=%LOCALAPPDATA%\PrintQueue\uploads"
if not exist "%DEST%" mkdir "%DEST%"
echo.
echo ================= PrintQueue Upload Recovery =================
echo Current shared uploads folder: "%DEST%"
echo.
echo If old orders show in dashboard but printing/downloading fails,
echo copy files from the previous working project's backend\private-uploads.
echo You can drag that private-uploads folder into this CMD window.
echo.
set /p "SOURCE=Paste old private-uploads folder path: "
set "SOURCE=%SOURCE:"=%"
if not exist "%SOURCE%\" (
 echo [ERROR] Cannot find source folder. No files were changed.
 pause
 exit /b 1
)
robocopy "%SOURCE%" "%DEST%" /E /R:1 /W:1
set "RC=%ERRORLEVEL%"
if %RC% GEQ 8 (
 echo [ERROR] Copy failed, robocopy code %RC%.
 pause
 exit /b %RC%
)
echo.
echo Copy completed. Restart the backend and test Download and Print.
echo No existing files were deleted.
pause
