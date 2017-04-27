#!/bin/bash
#
# CONFIGURATION SECTION
# Edit variables below to suit your environment.
#

# Top-level ceres-solver source directory.
CERES_SRC="${HOME}/src/hacks/ceres-solver"

# Top-level build directory for ceres-solver.
CERES_BIN="${HOME}/src/hacks/ceres-bin"

# Root directory for the installed Homebrew packages.
HOMEBREW_TOP="/usr/local/Cellar"

# Installation directories for some ceres-solver dependencies (headers,  DLL's).
GLOG_INCLUDE_DIR="${HOMEBREW_TOP}/glog/0.3.4_1/include"
GLOG_LIB_DIR="${HOMEBREW_TOP}/glog/0.3.4_1/lib"

GFLAGS_INCLUDE_DIR="${HOMEBREW_TOP}/gflags/2.2.0/include"
GFLAGS_LIB_DIR="${HOMEBREW_TOP}/gflags/2.2.0/lib"

EIGEN_INCLUDE_DIR="${HOMEBREW_TOP}/eigen/3.3.3/include/eigen3"

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

