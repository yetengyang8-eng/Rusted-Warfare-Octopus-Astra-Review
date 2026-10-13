@echo off
setlocal
python "%~dp0rw-agent-human.py" %*
exit /b %errorlevel%
