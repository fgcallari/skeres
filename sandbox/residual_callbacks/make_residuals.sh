#!/bin/bash
#
# Build script for the residual_callbacks wrap. 
# Tested on:
#   MacOSX 10.10.5
#   swig: SWIG Version 3.0.7
#   c++: Apple LLVM version 7.0.2 (clang-700.1.81)
#   javac: 1.7.0_71
#   scalac: Scala compiler version 2.10.4
#   scala:  Scala code runner version 2.10.4
#
set -eux

mkdir -p residuals classes
swig -c++ -java -package residuals -outdir residuals residuals.i
c++ -v -fPIC -c residuals.cc residuals_wrap.cxx -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin
c++ -dynamiclib -o libresiduals.dylib residuals.o residuals_wrap.o

javac -classpath classes -d classes residuals/*.java
scalac -classpath classes -d classes org/somelightprojections/skeres/Residuals.scala

scala -cp classes org.somelightprojections.skeres.Test

