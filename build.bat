@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
cd /d "%~dp0"
echo ============================================
echo   Regnum: сборка мода (лог: build.log)
echo ============================================

rem --- 1. Ищем Java 17+ ---
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
  for %%D in (
    "%~dp0.jdk"
    "%LOCALAPPDATA%\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-delta\windows-x64\java-runtime-delta"
    "%ProgramFiles(x86)%\Minecraft Launcher\runtime\java-runtime-delta\windows-x64\java-runtime-delta"
    "%APPDATA%\.minecraft\runtime\java-runtime-delta\windows-x64\java-runtime-delta"
    "%ProgramFiles%\Eclipse Adoptium"
    "%ProgramFiles%\Java"
    "%ProgramFiles%\Microsoft"
  ) do (
    if not defined JAVA_EXE (
      for /f "delims=" %%J in ('dir /b /s "%%~D\java.exe" 2^>nul') do (
        if not defined JAVA_EXE (
          "%%J" -version 2>&1 | findstr /r /c:"version \"1[7-9]" /c:"version \"2[0-9]" >nul && set "JAVA_EXE=%%J"
        )
      )
    )
  )
)
if not defined JAVA_EXE (
  where java >nul 2>&1 && (
    java -version 2>&1 | findstr /r /c:"version \"1[7-9]" /c:"version \"2[0-9]" >nul && for /f "delims=" %%J in ('where java') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
  )
)

rem --- 2. Если Java нет — скачиваем портативный JDK 21 (Temurin) в папку .jdk ---
if not defined JAVA_EXE (
  echo Java 17+ не найдена. Скачиваю портативный JDK 21 ^(~190 МБ^) в папку .jdk ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '%~dp0jdk21.zip'; Expand-Archive '%~dp0jdk21.zip' '%~dp0.jdk' -Force; Remove-Item '%~dp0jdk21.zip'"
  for /f "delims=" %%J in ('dir /b /s "%~dp0.jdk\java.exe" 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
)
if not defined JAVA_EXE (
  echo ОШИБКА: не удалось найти или скачать Java. > build.log
  echo ОШИБКА: не удалось найти или скачать Java.
  pause
  exit /b 1
)
for %%A in ("!JAVA_EXE!") do set "JAVA_BIN=%%~dpA"
for %%A in ("!JAVA_BIN!..") do set "JAVA_HOME=%%~fA"
echo Java: !JAVA_HOME!

rem --- 3. Сборка ---
echo Сборка идёт, первый раз это 5-15 минут (Gradle скачивает Minecraft и NeoForge)...
call gradlew.bat build --no-daemon --console=plain > build.log 2>&1
set RC=%ERRORLEVEL%
echo EXIT_CODE=%RC%>> build.log
if "%RC%"=="0" (
  echo.
  echo ГОТОВО! Мод: build\libs\regnum-0.1.0.jar
) else (
  echo.
  echo Сборка завершилась с ошибкой. Claude прочитает build.log и исправит.
)
echo Окно можно закрыть.
pause
