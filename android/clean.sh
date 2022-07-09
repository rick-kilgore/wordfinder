#!/bin/zsh

rm -rf python/*.c *.pickle python/wf.py mk.log __pycache__

if test "x$1" = "x-f"; then
  rm -rf wf wordfinder/build
fi
