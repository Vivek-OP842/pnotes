# libpnotests: the vendored tree-sitter runtime, the bundled grammars and a thin
# JNI bridge, compiled as one high-performance shared library.
cmake_minimum_required(VERSION 3.22.1)
project(pnotests C)

set(CMAKE_C_STANDARD 11)
# Optimized for performance with Link-Time Optimization (LTO)
add_compile_options(-O3 -flto -fvisibility=hidden -ffunction-sections -fdata-sections)

set(GRAMMAR_SOURCES
    grammars/bash/parser.c
    grammars/bash/scanner.c
    grammars/c/parser.c
    grammars/cpp/parser.c
    grammars/cpp/scanner.c
    grammars/java/parser.c
    grammars/javascript/parser.c
    grammars/javascript/scanner.c
    grammars/json/parser.c
    grammars/kotlin/parser.c
    grammars/kotlin/scanner.c
    grammars/python/parser.c
    grammars/python/scanner.c
)

add_library(pnotests SHARED
    tree-sitter/lib/src/lib.c
    ${GRAMMAR_SOURCES}
    ts_jni.c
)

target_include_directories(pnotests PRIVATE
    tree-sitter/lib/include
    tree-sitter/lib/src
)

# Each grammar carries its own tree_sitter/parser.h; scope it to that grammar's
# sources so the copies never collide.
foreach(src ${GRAMMAR_SOURCES})
    get_filename_component(dir ${src} DIRECTORY)
    set_source_files_properties(${src} PROPERTIES
        INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/${dir}")
endforeach()

# max-page-size=16384 keeps the lib loadable on 16 KB page-size devices (Android 15+).
target_link_options(pnotests PRIVATE -flto -Wl,--gc-sections -Wl,-z,max-page-size=16384)

