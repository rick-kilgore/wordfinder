#!/bin/bash

export PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin:$PATH"

java -jar wordfinder/build/libs/wordfinder.jar "$@"
