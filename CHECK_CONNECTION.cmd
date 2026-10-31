@echo off
echo ===== PrintQueue connection check =====
echo.
echo Docker:
docker compose version
echo.
echo Port 5432:
powershell -NoProfile -Command "Test-NetConnection 127.0.0.1 -Port 5432 -InformationLevel Quiet"
echo.
echo Port 8080:
powershell -NoProfile -Command "Test-NetConnection 127.0.0.1 -Port 8080 -InformationLevel Quiet"
echo.
echo Backend health:
curl.exe --max-time 5 http://localhost:8080/api/health
echo.
pause
