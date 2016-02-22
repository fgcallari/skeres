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

# ceres-solver dependecy (DLL) versions.
GLOG_VERSION="0.3.4"
GFLAGS_VERSION="2.1.2"
EIGEN_VERSION="3.2.2"

#
# END OF CONFIGURATION SECTION
# Do not edit below (achtung alles lookenpeepers ... blinkenlichts)

GEN_JAVA_DIR="./core/src/main/java/com/google/ceres"
GEN_CPP_DIR="./core/src/main/c++/ceres"
GEN_OBJ_DIR="./core/target/c++/ceres/obj"
GEN_LIB_DIR="./lib"

