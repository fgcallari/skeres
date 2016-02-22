#!/bin/bash

set -eux

. ./configuration.sh

rm -rf project root target src \
core/target examples/lib examples/target \
$GEN_JAVA_DIR $GEN_CPP_DIR $GEN_OBJ_DIR $GEN_LIB_DIR
