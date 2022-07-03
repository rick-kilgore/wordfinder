#!/bin/zsh

source ~/.venv/bin/activate
./gen_full.py
cython fw.py --embed
gcc fw.c -o fw -I$HOME/homebrew/Cellar/python@3.9/3.9.13_1/Frameworks/Python.framework/Versions/3.9/include/python3.9 \
    -L$HOME/homebrew/Cellar/python@3.9/3.9.13_1/Frameworks/Python.framework/Versions/3.9/lib/python3.9/config-3.9-darwin \
    -lpython3.9 -O2
./clean.sh

