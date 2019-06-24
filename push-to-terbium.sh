#!/bin/bash

SOURCE_DIR="candidates"
TARGET_DIR="terbium:/tmp/all-candidates"
WHOAMI=`hostname`

for dir in `ls $SOURCE_DIR`; do
    rsync -r $SOURCE_DIR/$dir $TARGET_DIR/$WHOAMI-$dir
done
