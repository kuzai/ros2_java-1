#!/bin/bash
# scripts/travis-ci/osx_run.sh

set -e

cd $HOME_BUILD
. ament_ws/install_isolated/local_setup.sh
cd $ROS2WS
. install_isolated/local_setup.sh
ament test --symlink-install --isolated --only-packages ament_cmake_export_jars rcljava rcljava_common
