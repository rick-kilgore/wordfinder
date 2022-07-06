#!/bin/zsh

rm -rf *.c *.pickle wf.py mk.log __pycache__

if test "x$1" = "x-f"; then
  rm -rf wf org wordfinder.jar
fi
