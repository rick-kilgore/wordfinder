#!/bin/zsh

rm -rf *.c *.pickle wf.py mk.log __pycache__ org

if test "x$1" = "x-f"; then
  rm -f wf
fi
