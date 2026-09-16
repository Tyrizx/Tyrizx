cmake_minimum_required(VERSION 3.22.1)
project("tyrizx-native")

# Header for node
include_directories(${CMAKE_SOURCE_DIR}/../../../libnode/include/node)

# Import prebuilt libnode.so for the current ABI
add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libnode.so
)

add_library(tyrizx-native SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(tyrizx-native node ${log-lib})
