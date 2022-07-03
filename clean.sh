#!/bin/zsh

rm -f *.c *.pickle fw.py mk.log

if test "x$1" = "x-f"; then
  rm -f fw
fi
