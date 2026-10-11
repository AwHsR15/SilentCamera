@echo off
where py >nul 2>nul
if %errorlevel% equ 0 (
    py -3 "%~dp0adb_control.py" %*
) else (
    python "%~dp0adb_control.py" %*
)
