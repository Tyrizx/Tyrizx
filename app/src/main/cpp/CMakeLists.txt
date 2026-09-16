cmake_minimum_required(VERSION 3.22.1)
project("tyrizx-native")

include_directories(${CMAKE_SOURCE_DIR}/../../../libnode/include/node)

add_library(tyrizx-native SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(tyrizx-native ${log-lib})
