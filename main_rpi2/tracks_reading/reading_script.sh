#!/bin/bash
LABEL=$(/sbin/blkid -s LABEL | grep /dev/sda1 | cut -d\" -f2 | tr '\ ' '_')
LABEL=/media/${LABEL}
python /home/pi/IODD/tracks_reading/dirtree_to_JSON.py $LABEL
