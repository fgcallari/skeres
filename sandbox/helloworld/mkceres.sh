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

CERES_BIN="$HOME/src/ceres-bin"
HOMEBREW_TOP="/opt/twitter/Cellar/"
GLOG_VERSION="0.3.4"
GFLAGS_VERSION="2.1.2"

mkdir -p ceres classes

swig -O -c++ -java -package ceres -outdir ceres \
-I"${CERES_BIN}/config" \
-I"../../include" \
-I"${CERES_BIN}/config" \
-I"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/include/" \
-I"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/include" \
-D"CERES_EXPORT" \
ceres.i

c++ -c -fPIC -O3 -o ceres_wrap.cxx.o ceres_wrap.cxx \
-I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/darwin" \
-I"../../include" \
-I"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/include" \
-I"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/include" \
-I"${CERES_BIN}/config"

c++ -O3 -dynamiclib -o libskeres.dylib ceres_wrap.cxx.o \
-L"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/lib" -lglog \
-L"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/lib" -lgflags \
-L"${CERES_BIN}/lib" -lceres

javac -classpath classes -d classes ceres/*.java
scalac -classpath classes -d classes \
org/somelightprojections/skeres/skeres.scala \
org/somelightprojections/skeres/HelloWorld.scala

export LD_LIBRARY_PATH=\
"."\
":${CERES_BIN}/lib"\
":${HOMEBREW_TOP}/glog/${GLOG_VERSION}/lib"\
":${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/lib"

scala -classpath classes org.somelightprojections.skeres.HelloWorld

