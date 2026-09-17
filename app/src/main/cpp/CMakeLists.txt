cmake_minimum_required(VERSION 3.18.1)
project("tyrizx")

include_directories(${CMAKE_SOURCE_DIR}/libnode/include/node)

add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libnode/bin/${ANDROID_ABI}/libnode.so
)

find_library(log-lib log)

add_library(tyrizx SHARED native-lib.cpp)

target_link_libraries(tyrizx node ${log-lib})
