@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0runtime\start-game.ps1"
if errorlevel 1 pause
