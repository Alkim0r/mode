@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
cd /d "%~dp0"
title Regnum - автосборка (не закрывайте окно)
set "JAVA_EXE="
for /f "delims=" %%J in ('dir /b /s "%~dp0.jdk\java.exe" 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
if not defined JAVA_EXE (
  echo Сначала один раз запустите build.bat - он скачает Java.
  pause
  exit /b 1
)
for %%A in ("!JAVA_EXE!") do set "JAVA_BIN=%%~dpA"
for %%A in ("!JAVA_BIN!..") do set "JAVA_HOME=%%~fA"
echo ==========================================================
echo  Regnum: режим автосборки. Claude сам запускает сборку,
echo  когда обновляет код. Окно можно свернуть, но не закрывать.
echo ==========================================================
echo build> _rebuild.flag
:loop
if exist _rebuild.flag (
  del /q _rebuild.flag >nul 2>&1
  echo [%time%] Сборка...
  echo BUILD_STARTED> build.log
  call gradlew.bat build --console=plain > build.log 2>&1
  echo EXIT_CODE=!ERRORLEVEL!>> build.log
  echo [%time%] Готово, код !ERRORLEVEL!
)
timeout /t 5 /nobreak >nul
goto loop
