#!/bin/bash
#
# CONFIGURATION SECTION
# Edit variables below to suit your environment.
#

# Top-level ceres-solver source directory.
CERES_SRC="${HOME}/src/ceres-solver"

# Top-level build directory for ceres-solver.
CERES_BIN="${HOME}/src/ceres-bin"

# Root directory for the installed Homebrew packages.
HOMEBREW_TOP="/opt/twitter/Cellar"

# Installation directories for some ceres-solver dependencies (headers,  DLL's).
GLOG_INCLUDE_DIR="/opt/twitter/Cellar/glog/0.3.4/include"
GLOG_LIB_DIR="/opt/twitter/Cellar/glog/0.3.4/lib"

GFLAGS_INCLUDE_DIR="/opt/twitter/Cellar/gflags/2.1.2/include"
GFLAGS_LIB_DIR="/opt/twitter/Cellar/gflags/2.1.2/lib"

EIGEN_INCLUDE_DIR="/opt/twitter/Cellar/eigen/3.2.2/include/eigen3"

# C++ compiler and flags to generate DLL-linkable objects.
CXX="c++"
CXXFLAGS="-c -fPIC -O3"

# Linker and flags to produce a DLL.
LD="c++"
LDFLAGS="-dynamiclib"

# DLL filename suffix on this system.
DLL_EXT=".dylib"

#
# END OF CONFIGURATION SECTION
# Do not edit below, unless you know what you are doing
# (achtung alles lookenpeepers ... blinkenlichts)
#
GEN_JAVA_DIR="core/src/main/java/com/google/ceres"
GEN_CPP_DIR="core/src/main/c++/ceres"
GEN_OBJ_DIR="core/target/c++/ceres/obj"
GEN_LIB_DIR="lib"

