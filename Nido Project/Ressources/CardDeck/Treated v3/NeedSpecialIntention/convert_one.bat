@echo off
set input=%~1
set output=%~n1_cut.png
magick "%input%" -gravity center -crop 300x600+0+0 +repage ^
( -size 300x600 xc:none -fill white -draw "roundrectangle 0,0 299,599,40,40" ) ^
-alpha set -compose DstIn -composite "%output%"
