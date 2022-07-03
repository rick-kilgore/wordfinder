#!/bin/zsh

rm -rf *.c *.pickle fw.py mk.log __pycache__

if test "x$1" = "x-f"; then
  rm -f fw
fi
