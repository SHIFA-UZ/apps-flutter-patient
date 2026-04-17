@echo off
REM Run Shifa Patient App in emulator with Railway backend
REM Usage: scripts\run_emulator_railway.bat [RAILWAY_BASE_URL]
REM Example: scripts\run_emulator_railway.bat
REM Example: scripts\run_emulator_railway.bat https://your-app.up.railway.app
REM NOTE: Do NOT include /api in the URL - the app adds it automatically

setlocal

set "API_BASE_URL=%~1"
if "%API_BASE_URL%"=="" set "API_BASE_URL=https://shifa-doc-backend-mvp-production.up.railway.app"

echo.
echo ========================================
echo Patient App - Emulator (Railway backend)
echo API Base: %API_BASE_URL%
echo ========================================
echo.

cd /d "%~dp0.."
call flutter pub get
flutter run --dart-define=API_BASE_URL=%API_BASE_URL%

endlocal
