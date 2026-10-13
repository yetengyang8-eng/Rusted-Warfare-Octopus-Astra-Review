@echo off
setlocal
"C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" "%~dp0runtime\tools\rw-agent-human.py" --game-dir "%~dp0game" --jar "%~dp0game\rw-agent-bootstrap.jar" --java "%~dp0jvm64\bin\java.exe" --port 47707 --seconds 2400 --output-root "%~dp0runs" --require-same-bridge --html --property rwagent.reportMode=compact --property rwagent.reportGzip=true --property rwagent.g6=true
if errorlevel 1 pause
