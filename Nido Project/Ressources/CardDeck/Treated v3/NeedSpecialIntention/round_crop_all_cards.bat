@echo off
for %%f in (nido_card_*.jpg) do (
  call convert_one.bat "%%f"
)
echo All done!
pause
