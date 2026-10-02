@echo off
cd /d "%~dp0"
set G=.codex-tools\mingit\cmd\git.exe -c safe.directory=C:/Users/Pavel/Desktop/for_mode/regnum
if exist .git\HEAD.lock del .git\HEAD.lock
if exist .git\index.lock del .git\index.lock
%G% push origin main
pause
