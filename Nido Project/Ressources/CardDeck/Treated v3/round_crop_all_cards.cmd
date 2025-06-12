@echo off
setlocal enabledelayedexpansion
for %%f in (nido_card_*.jpg) do (
  echo Processing %%f...
  call magick "%%f" -gravity center -crop 300x600+0+0 +repage ( -size 300x600 xc:none -fill white -draw "roundrectangle 0,0 299,599,40,40" ) -alpha set -compose DstIn -composite "%%~nf_cut.png"
)
echo All cards processed!