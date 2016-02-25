#!/bin/bash

set -eux

. ./configuration.sh

rm -rf project root target src \
core/target core/project \
examples/lib examples/target examples/project \
examples/src/main/java examples/src/main/resources examples/src/main/scala-2.11 \
examples/src/test \
$GEN_JAVA_DIR $GEN_CPP_DIR $GEN_OBJ_DIR $GEN_LIB_DIR
