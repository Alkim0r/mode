@echo off
cd /d "%~dp0"
title Regnum - git push по флагу (не закрывайте окно)
set G=.codex-tools\mingit\cmd\git.exe -c safe.directory=C:/Users/Pavel/Desktop/for_mode/regnum
:loop
if exist _push.flag (
  del /q _push.flag >nul 2>&1
  if exist .git\HEAD.lock del .git\HEAD.lock >nul 2>&1
  if exist .git\index.lock del .git\index.lock >nul 2>&1
  echo [%time%] push...> push.log
  %G% push origin main >> push.log 2>&1
  echo EXIT_CODE=%ERRORLEVEL%>> push.log
)
timeout /t 5 /nobreak >nul
goto loop
