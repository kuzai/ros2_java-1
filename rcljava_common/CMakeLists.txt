cmake_minimum_required(VERSION 3.5)

project(rcljava_common)

find_package(ament_cmake REQUIRED)

find_package(ament_cmake_export_jars REQUIRED)
find_package(ament_cmake_export_libraries REQUIRED)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake/Modules")

include(JavaExtra)


if(NOT WIN32)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14 -Wall -Wextra")
endif()

set(${PROJECT_NAME}_java_sources
  "src/main/java/org/ros2/rcljava/common/RCLJavaProxy.java"
  "src/main/java/org/ros2/rcljava/internal/message/Message.java"
  "src/main/java/org/ros2/rcljava/internal/service/MessageService.java"
)

set(${PROJECT_NAME}_cpp_sources
  "src/main/cpp/rcljava_common.cpp"
)

add_jar("${PROJECT_NAME}_jar"
  "${${PROJECT_NAME}_java_sources}"
  OUTPUT_NAME
  "${PROJECT_NAME}"
)

install_jar("${PROJECT_NAME}_jar" "share/${PROJECT_NAME}/java")
ament_export_jars("share/${PROJECT_NAME}/java/${PROJECT_NAME}.jar")
ament_java_package_hook(${PROJECT_NAME})

find_jar(SFL4J_JAR NAMES slf4j-api)
if(${SFL4J_JAR})
  ament_export_jars(${SFL4J_JAR})
else()
  set(slf4j_version "1.7.25")
  #set(slf4j_api_sha256 "18c4a0095d5c1da6b817592e767bb23d29dd2f560ad74df75ff3961dbde25b79")
  #set(slf4j_api_url "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/${slf4j_version}/slf4j-api-${slf4j_version}.jar")
  set(slf4j_api_jar_path "/slf4j-api-${slf4j_version}.jar")
  #file(DOWNLOAD ${slf4j_api_url} ${slf4j_api_jar_path} EXPECTED_HASH SHA256=${slf4j_api_sha256})
  #execute_process(COMMAND bash "-c" "wget ${slf4j_api_url})
  install(FILES
    ${slf4j_api_jar_path}
    DESTINATION
    "share/${PROJECT_NAME}/java")

  ament_export_jars("share/${PROJECT_NAME}/java/slf4j-api-${slf4j_version}.jar")

  if(ANDROID)
    #set(slf4j_android_sha256 "fbd7b254e02d8aef60c418a5f0e14a783b38a16162caffb2d2a16ccd5d2c09b4")
    #set(slf4j_android_url "https://repo1.maven.org/maven2/org/slf4j/slf4j-android/${slf4j_version}/slf4j-android-${slf4j_version}.jar")
    set(slf4j_android_jar_path "/slf4j-android-${slf4j_version}.jar")
    #file(DOWNLOAD ${slf4j_android_url} ${slf4j_android_jar_path} EXPECTED_HASH SHA256=${slf4j_android_sha256})
    list(APPEND logging_jars "${slf4j_android_jar_path}")

    install(FILES
      ${slf4j_android_jar_path}
      DESTINATION
      "share/${PROJECT_NAME}/java")

    ament_export_jars("share/${PROJECT_NAME}/java/slf4j-android-${slf4j_version}.jar")
  else()
    set(log4j_version "1.2.17")

    set(log4j_sha256 "1d31696445697720527091754369082a6651bd49781b6005deb94e56753406f9")
    set(slf4j_log4j_sha256 "ddb343954deb6f046f862606c534178730c02ed23d0b7f6ca1012c1e3fa74273")
    set(slf4j_jdk14_sha256 "9b8b9b8279959b17e71432d40b8cf4175c761c3bc6ebc2c7ec0f2ae8ff223feb")

    set(log4j_url "https://repo1.maven.org/maven2/log4j/log4j/${log4j_version}/log4j-${log4j_version}.jar")
    set(slf4j_log4j_url "https://repo1.maven.org/maven2/org/slf4j/slf4j-log4j12/${slf4j_version}/slf4j-log4j12-${slf4j_version}.jar")
    set(slf4j_jdk14_url "https://repo1.maven.org/maven2/org/slf4j/slf4j-jdk14/${slf4j_version}/slf4j-jdk14-${slf4j_version}.jar")

    set(log4j_jar_path "${CMAKE_CURRENT_BINARY_DIR}/jars/log4j-${log4j_version}.jar")
    set(slf4j_log4j_jar_path "${CMAKE_CURRENT_BINARY_DIR}/jars/slf4j-log4j12-${slf4j_version}.jar")
    set(slf4j_jdk14_jar_path "${CMAKE_CURRENT_BINARY_DIR}/jars/slf4j-jdk14-${slf4j_version}.jar")

    file(DOWNLOAD ${log4j_url} ${log4j_jar_path} EXPECTED_HASH SHA256=${log4j_sha256})
    file(DOWNLOAD ${slf4j_log4j_url} ${slf4j_log4j_jar_path} EXPECTED_HASH SHA256=${slf4j_log4j_sha256})
    file(DOWNLOAD ${slf4j_jdk14_url} ${slf4j_jdk14_jar_path} EXPECTED_HASH SHA256=${slf4j_jdk14_sha256})
    list(APPEND logging_jars "${log4j_jar_path}")
    list(APPEND logging_jars "${slf4j_log4j_jar_path}")
    list(APPEND logging_jars "${slf4j_jdk14_jar_path}")

    install(FILES
      ${log4j_jar_path} ${slf4j_api_jar_path}
      ${slf4j_log4j_jar_path} ${slf4j_android_jar_path}
      ${slf4j_jdk14_jar_path}
      DESTINATION
      "share/${PROJECT_NAME}/java")

    ament_export_jars("share/${PROJECT_NAME}/java/log4j-${log4j_version}.jar")
    ament_export_jars("share/${PROJECT_NAME}/java/slf4j-log4j12-${slf4j_version}.jar")
    ament_export_jars("share/${PROJECT_NAME}/java/slf4j-jdk14-${slf4j_version}.jar")
  endif()
endif()

include_directories(include)

add_library(${PROJECT_NAME} SHARED ${${PROJECT_NAME}_cpp_sources})

target_include_directories(${PROJECT_NAME}
  PUBLIC
  ${JNI_INCLUDE_DIRS}
)

ament_export_include_directories(include)

ament_export_libraries(${PROJECT_NAME})

install(
  TARGETS ${PROJECT_NAME}
  ARCHIVE DESTINATION lib
  LIBRARY DESTINATION lib
  RUNTIME DESTINATION bin)

install(
  DIRECTORY include/
  DESTINATION include
)

if(BUILD_TESTING)
  find_package(ament_lint_auto REQUIRED)
  ament_lint_auto_find_test_dependencies()
endif()

ament_package(
  CONFIG_EXTRAS "rcljava_cmake_module-extras.cmake.in"
)

install(DIRECTORY cmake
  DESTINATION share/${PROJECT_NAME}
)
