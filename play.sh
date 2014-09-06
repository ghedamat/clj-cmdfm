#!/bin/bash
FILE=$1
wget -q -O - $FILE > /tmp/mplayer-data &
mplayer /tmp/mplayer-data -cache 2048 -slave -input file=/tmp/mplayer-control

