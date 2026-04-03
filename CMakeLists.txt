cmake_minimum_required(VERSION 3.20)

project(TradingSystemStarter VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(trading_system
    src/order_book.cpp
    src/trading_system.cpp
)

target_include_directories(trading_system PUBLIC include)

target_compile_options(trading_system PRIVATE
    $<$<CXX_COMPILER_ID:AppleClang,Clang,GNU>:-Wall -Wextra -Wpedantic>
)

add_executable(trading_demo apps/demo_main.cpp)
target_link_libraries(trading_demo PRIVATE trading_system)

add_executable(trading_system_test tests/trading_system_test.cpp)
target_link_libraries(trading_system_test PRIVATE trading_system)

include(CTest)
enable_testing()
add_test(NAME trading_system_test COMMAND trading_system_test)
