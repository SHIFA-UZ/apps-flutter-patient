@echo off
REM Build Flutter web patient app and deploy to Firebase Hosting (shifa-bemor).
REM Usage: scripts\build_web.bat API_BASE_URL [ENVIRONMENT]
REM Example: scripts\build_web.bat https://shifa-doc-backend-mvp-production.up.railway.app production

setlocal
cd /d "%~dp0\.."

if "%1"=="" (
    echo Usage: %~nx0 API_BASE_URL [ENVIRONMENT]
    echo Example: %~nx0 https://shifa-doc-backend-mvp-production.up.railway.app production
    exit /b 1
)

set API_BASE_URL=%1
for /f "delims=" %%i in ('powershell -command "$url='%API_BASE_URL%'; $url -replace '/api/?$', ''"') do set API_BASE_URL=%%i

if "%2"=="" (
    set ENVIRONMENT=production
) else (
    set ENVIRONMENT=%2
)

echo Cleaning previous build...
call flutter clean

echo Getting dependencies...
call flutter pub get

echo Building web release for Shifa Bemor...
call flutter build web --release --pwa-strategy=none --no-wasm-dry-run --dart-define=API_BASE_URL=%API_BASE_URL% --dart-define=ENVIRONMENT=%ENVIRONMENT% --base-href=/
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo.
echo Build complete. Deploying to Firebase Hosting (patient: shifa-bemor)...
call firebase deploy --only hosting:patient --project staging
if errorlevel 1 (
    echo Firebase deploy failed. Ensure hosting site "shifa-bemor" exists:
    echo   firebase hosting:sites:create shifa-bemor --project shifa-doctor-staging
    exit /b 1
)

echo.
echo Done. Patient web app deployed.
endlocal
