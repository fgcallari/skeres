#!/bin/bash
#
# Build script for skeres.
# Tested on:
#   ceres-solver source at commit point:
#      commit 8ecfb2d7058583510778dc93cdc643e7933f7f62
#      Author: Sameer Agarwal <sameeragarwal@google.com>
#      Date:   Thu Dec 17 13:55:02 2015 -0800
#   MacOSX 10.10.5
#   swig: SWIG Version 3.0.7
#   c++: Apple LLVM version 7.0.2 (clang-700.1.81)
#   glog: 0.3.4, from Homebrew
#   gflags: 2.1.2, from Homebrew.
#   javac: 1.7.0_71
#   scalac: Scala compiler version 2.11.7
#   scala:  Scala code runner version 2.11.7
#   sbt: SBT version 0.13.9
#
set -eux

. ./configuration.sh

mkdir -p $GEN_JAVA_DIR $GEN_CPP_DIR $GEN_OBJ_DIR $GEN_LIB_DIR

swig -O -c++ -java \
-package com.google.ceres \
-outdir "${GEN_JAVA_DIR}" \
-o "${GEN_CPP_DIR}/ceres_wrap.cc" \
-oh "${GEN_CPP_DIR}/ceres_wrap.h" \
-I"${CERES_BIN}/config" \
-I"${CERES_SRC}/include" \
-I"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/include/" \
-I"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/include" \
-D"CERES_EXPORT" \
ceres.i

c++ -c -fPIC -O3 \
"${GEN_CPP_DIR}/ceres_wrap.cc" \
-o "${GEN_OBJ_DIR}/ceres_wrap.cc.o" \
-I "${GEN_CPP_DIR}" \
-I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/darwin" \
-I"${CERES_SRC}/include" \
-I"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/include" \
-I"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/include" \
-I"${HOMEBREW_TOP}/eigen/${EIGEN_VERSION}/include/eigen3" \
-I"${CERES_BIN}/config"

c++ -O3 -dynamiclib \
"${GEN_OBJ_DIR}/ceres_wrap.cc.o" \
-o "${GEN_LIB_DIR}/libskeres.dylib" \
-L"${HOMEBREW_TOP}/glog/${GLOG_VERSION}/lib" -lglog \
-L"${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/lib" -lgflags \
-L"${CERES_BIN}/lib" -lceres

export DYLD_LIBRARY_PATH=\
"$PWD/${GEN_LIB_DIR}"\
":${CERES_BIN}/lib"\
":${HOMEBREW_TOP}/glog/${GLOG_VERSION}/lib"\
":${HOMEBREW_TOP}/gflags/${GFLAGS_VERSION}/lib"

sbt -Djava.library.path="${DYLD_LIBRARY_PATH}"

