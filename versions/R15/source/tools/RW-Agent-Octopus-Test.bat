@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0RW-Agent-Octopus-Test.ps1" %*
if errorlevel 1 pause
exit /b %errorlevel%
