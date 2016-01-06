#!/bin/bash
#
# Build script for the HelloWorld ceres wrap demo. 
# Tested on:
#   ceres-solver source at commit point:
#      commit 8ecfb2d7058583510778dc93cdc643e7933f7f62
#      Author: Sameer Agarwal <sameeragarwal@google.com>
#      Date:   Thu Dec 17 13:55:02 2015 -0800
#   MacOSX 10.10.5
#   swig: SWIG Version 3.0.7
#   c++: Apple LLVM version 7.0.2 (clang-700.1.81)
#   javac: 1.7.0_71
#   scalac: Scala compiler version 2.10.4
#   scala:  Scala code runner version 2.10.4
#
set -eux

mkdir -p ceres classes

swig -O -c++ -java -package ceres -outdir ceres \
-I/Users/francesco/src/ceres-bin/config \
-I/Users/francesco/src/ceres-solver/include \
-I/Users/francesco/src/ceres-bin/config \
-I/opt/twitter/Cellar/glog/0.3.4/include/ \
-I/opt/twitter/Cellar/gflags/2.1.2/include \
-DCERES_EXPORT \
ceres.i

c++ -c -fPIC -O3 -o ceres_wrap.cxx.o ceres_wrap.cxx \
-I$JAVA_HOME/include -I$JAVA_HOME/include/darwin \
-I../../include/ \
-I/opt/twitter/Cellar/glog/0.3.4/include/ \
-I/opt/twitter/Cellar/gflags/2.1.2/include/ \
-I../../../ceres-bin/config/

c++ -O3 -dynamiclib -o libskeres.dylib ceres_wrap.cxx.o \
-L/opt/twitter/Cellar/glog/0.3.4/lib -lglog \
-L/Users/francesco/src/ceres-bin/lib -lceres

javac -classpath classes -d classes ceres/*.java
scalac -classpath classes -d classes \
org/somelightprojections/skeres/skeres.scala \
org/somelightprojections/skeres/HelloWorld.scala

export LD_LIBRARY_PATH=\
"."\
":$HOME/src/ceres-bin/lib"\
":/opt/twitter/Cellar/glog/0.3.4/lib"\
":/opt/twitter/Cellar/gflags/2.1.2/lib"

scala -classpath classes org.somelightprojections.skeres.HelloWorld

