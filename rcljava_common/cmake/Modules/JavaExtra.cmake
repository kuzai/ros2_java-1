# Copyright 2016 Esteve Fernandez <esteve@apache.org>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include(CrossCompilingExtra)

if(ANDROID)
  # https://cmake.org/cmake/help/v3.0/module/FindJava.html?highlight=java
  find_host_package(Java 1.6 COMPONENTS Development REQUIRED)
else()
  # https://cmake.org/cmake/help/v3.0/module/FindJava.html?highlight=java
  find_package(Java 1.6 COMPONENTS Development REQUIRED)

  # https://cmake.org/cmake/help/v3.0/module/FindJNI.html?highlight=jni
  find_package(JNI REQUIRED)
endif()

# https://cmake.org/cmake/help/v3.0/module/UseJava.html?highlight=java
include(UseJava)

### Definitions ###
# Java version profile (<1.6 for old-android)
#set(CMAKE_JAVA_COMPILE_FLAGS "-source" "1.6" "-target" "1.6")

# Bug in find_jar for Junit4 : https://sourceforge.net/p/rdkit/mailman/message/35301727/
set(JUNIT_JAR "/usr/share/java/junit4.jar")

function(add_junit_tests TARGET_NAME)

  cmake_parse_arguments(_add_junit_tests
    ""
    ""
    "TESTS;SOURCES;INCLUDE_JARS;LIBRARY_PATHS"
    ${ARGN}
  )

  set(_source_files ${_add_junit_tests_SOURCES} ${_add_junit_tests_UNPARSED_ARGUMENTS})

  if(WIN32 AND NOT CYGWIN)
    set(SEPARATOR ";")
  else()
    set(SEPARATOR ":")
  endif()

  find_jar(JUNIT_JAR NAMES junit4)
  if(${JUNIT_JAR})
    ament_export_jars(${JUNIT_JAR})
    set(${TARGET_NAME}_jar_dependencies "${JUNIT_JAR}")
  else()
    find_jar(JUNIT_JAR NAMES junit VERSIONS 4)
    if(${JUNIT_JAR})
      ament_export_jars(${JUNIT_JAR})
    else()
      set(junit_version "4.12")

      set(junit_sha256 "59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a")

      set(junit_url "http://central.maven.org/maven2/junit/junit/${junit_version}/junit-${junit_version}.jar")

      set(junit_jar_path "${CMAKE_CURRENT_BINARY_DIR}/jars/junit-${junit_version}.jar")

      file(DOWNLOAD ${junit_url} ${junit_jar_path} EXPECTED_HASH SHA256=${junit_sha256})

      install(FILES
        ${junit_jar_path}
        DESTINATION
        "share/${PROJECT_NAME}/java")

      ament_export_jars("share/${PROJECT_NAME}/java/junit-${junit_version}.jar")

      set(JUNIT_JAR "${junit_jar_path}")
    endif()
  endif()

  find_jar(HAMCREST_JAR NAMES hamcrest-all)
  if(${HAMCREST_JAR})
    ament_export_jars(${HAMCREST_JAR})
  else()
    set(hamcrest_version "1.3")

    set(hamcrest_sha256 "4877670629ab96f34f5f90ab283125fcd9acb7e683e66319a68be6eb2cca60de")

    set(hamcrest_url "http://central.maven.org/maven2/org/hamcrest/hamcrest-all/${hamcrest_version}/hamcrest-all-${hamcrest_version}.jar")

    set(hamcrest_jar_path "${CMAKE_CURRENT_BINARY_DIR}/jars/hamcrest-all-${hamcrest_version}.jar")

    file(DOWNLOAD ${hamcrest_url} ${hamcrest_jar_path} EXPECTED_HASH SHA256=${hamcrest_sha256})

    install(FILES
      ${hamcrest_jar_path}
      DESTINATION
      "share/${PROJECT_NAME}/java")

    ament_export_jars("share/${PROJECT_NAME}/java/hamcrest-all-${hamcrest_version}.jar")

    set(HAMCREST_JAR "${hamcrest_jar_path}")
  endif()

  set(${TARGET_NAME}_jar_dependencies "${JUNIT_JAR}${SEPARATOR}${HAMCREST_JAR}")

  add_jar("${TARGET_NAME}_jar"
    "${_source_files}"
    OUTPUT_NAME
    "${TARGET_NAME}"
    INCLUDE_JARS
    "${_add_junit_tests_INCLUDE_JARS}"
    "${JUNIT_JAR}"
    "${HAMCREST_JAR}"
  )

  get_property(_jar_test_file
    TARGET "${TARGET_NAME}_jar"
    PROPERTY "JAR_FILE"
  )

  set(${TARGET_NAME}_jar_dependencies "${${TARGET_NAME}_jar_dependencies}${SEPARATOR}${_jar_test_file}")
  foreach(_jar_dep ${_add_junit_tests_INCLUDE_JARS})
    set(${TARGET_NAME}_jar_dependencies "${${TARGET_NAME}_jar_dependencies}${SEPARATOR}${_jar_dep}")
  endforeach()

  string(REPLACE ";" ${SEPARATOR} _library_paths "${_add_junit_tests_LIBRARY_PATHS}")

  ament_add_test(
    ${TARGET_NAME}
    GENERATE_RESULT_FOR_RETURN_CODE_ZERO
    TIMEOUT 200
    COMMAND ${Java_JAVA_EXECUTABLE}
    ${JVMARGS} -classpath ${${TARGET_NAME}_jar_dependencies} -Djava.library.path=${_library_paths}
    org.junit.runner.JUnitCore ${_add_junit_tests_TESTS}
  )

  add_custom_target(${TARGET_NAME} DEPENDS ${_jar_test_file})

  add_dependencies(${TARGET_NAME} "${TARGET_NAME}_jar")
endfunction()
