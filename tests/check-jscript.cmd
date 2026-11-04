@echo off
setlocal
set "DIR=%~dp0..\src\main\resources\protocol"
cd /d "%DIR%"
cscript //Nologo //E:JScript "open-url.js" "phpstorm://open?file=C:\Windows\win.ini&line=1"
set "EC=%ERRORLEVEL%"
if %EC% GEQ 2 exit /b 1
exit /b 0
