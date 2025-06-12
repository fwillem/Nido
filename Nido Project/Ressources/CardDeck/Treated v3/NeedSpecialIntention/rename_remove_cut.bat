@echo off
echo Renaming all *_cut.png to *.png...
for %%f in (*_cut.png) do (
    set "old=%%f"
    setlocal enabledelayedexpansion
    set "new=!old:_cut=!"
    ren "%%f" "!new!"
    endlocal
)
echo Done!
pause
