#!/bin/zsh

source ~/.venv/bin/activate
cd python
./gen_full.py && \
cython wf.py --embed |& tee mk.log && \
gcc wf.c -o wf -I$HOME/homebrew/Cellar/python@3.9/3.9.13_1/Frameworks/Python.framework/Versions/3.9/include/python3.9 \
    -L$HOME/homebrew/Cellar/python@3.9/3.9.13_1/Frameworks/Python.framework/Versions/3.9/lib/python3.9/config-3.9-darwin \
    -lpython3.9 -O2 |& tee -a mk.log && \
mv wf ../
#/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/javac -d . java/org/rkilgore/*.java >& mk.log && \
#/usr/bin/jar cvf wordfinder.jar org && \

cd ..
gradle -p wordfinder jar |& tee -a mk.log
./clean.sh && \
if test "x$1" = "x-i"; then
  mv wf ~/bin/
fi

if test -e mk.log; then
  cat mk.log
fi
