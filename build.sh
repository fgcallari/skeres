#!/bin/bash
#
# Build script for skeres.
# Swig's the libary, compiles and links the wrappers and then starts sbt with
# the environment set up to load the generated DLL's.
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
-I"${CERES_SRC}/internal" \
-I"${GLOG_INCLUDE_DIR}" \
-I"${GFLAGS_INCLUDE_DIR}" \
-D"CERES_EXPORT" \
ceres.i

$CXX $CXXFLAGS \
"${GEN_CPP_DIR}/ceres_wrap.cc" \
-o "${GEN_OBJ_DIR}/ceres_wrap.cc.o" \
-I "${GEN_CPP_DIR}" \
-I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/darwin" \
-I"${CERES_SRC}/include" \
-I"${CERES_SRC}/internal" \
-I"${GLOG_INCLUDE_DIR}" \
-I"${GFLAGS_INCLUDE_DIR}" \
-I"${EIGEN_INCLUDE_DIR}" \
-I"${CERES_BIN}/config"

$LD $LDFLAGS \
"${GEN_OBJ_DIR}/ceres_wrap.cc.o" \
-o "${GEN_LIB_DIR}/libskeres.1.0${DLL_EXT}" \
-L"${GLOG_LIB_DIR}" -lglog \
-L"${GFLAGS_LIB_DIR}" -lgflags \
-L"${CERES_BIN}/lib" -lceres

(\
  cd $GEN_LIB_DIR; \
  ln -sf libskeres.1.0${DLL_EXT} libskeres.1${DLL_EXT}; \
  ln -sf libskeres.1${DLL_EXT} libskeres${DLL_EXT} \
)

export DYLD_LIBRARY_PATH=\
"$PWD/${GEN_LIB_DIR}"\
":${CERES_BIN}/lib"\
":${GLOG_LIB_DIR}"\
":${GFLAGS_LIB_DIR}"

sbt -Djava.library.path="${DYLD_LIBRARY_PATH}"

