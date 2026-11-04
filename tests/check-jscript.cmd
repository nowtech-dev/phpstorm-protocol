@echo off
setlocal
rem Uso manual no Windows (Explorer ou cmd nativo). O ./builder no WSL chama cscript via wslpath/Temp.
set "DIR=%~dp0..\src\main\resources\protocol"
cd /d "%DIR%"
if errorlevel 1 (
  echo Falha ao entrar em %DIR%
  exit /b 1
)
cscript //Nologo //E:JScript "open-url.js" "phpstorm://open?file=C:\Windows\win.ini&line=1"
set "EC=%ERRORLEVEL%"
if %EC% GEQ 2 exit /b 1
exit /b 0
