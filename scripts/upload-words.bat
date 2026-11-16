@echo off
cd /d "%~dp0"
echo ==== Vocabox word upload ====
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js is not installed. Install the LTS version from https://nodejs.org then run this file again.
  pause
  exit /b
)
if not exist serviceAccountKey.json (
  echo serviceAccountKey.json not found in this folder: %cd%
  echo Put the key file here with exactly this name, then run again.
  pause
  exit /b
)
if not exist upload-words.mjs (
  echo upload-words.mjs not found. Put this .bat file inside the scripts folder.
  pause
  exit /b
)
call npm install
echo.
echo ---- Dry run, nothing is uploaded yet ----
call node upload-words.mjs --dry
echo.
set /p ok=Looks fine? Type Y and press Enter to upload now: 
if /i "%ok%"=="Y" (
  call node upload-words.mjs
) else (
  echo Cancelled.
)
echo.
pause
