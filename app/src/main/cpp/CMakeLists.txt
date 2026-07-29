cmake_minimum_required(VERSION 3.22.1)

project(tinto_whisper CXX)

# whisper.cpp is pulled at configure time rather than vendored into the repo, so
# upgrading is a one-line change and we are not carrying ~100k lines of someone
# else's C++ in our history.
#
# TODO(sprint-N): replace GIT_TAG with a full 40-character commit SHA. A tag is
#  mutable; this app writes to a ledger and a build should not be able to change
#  underneath us. The tag below could not be verified from the environment this
#  was written in (github.com was unreachable) — confirm it resolves on the
#  first CI run and pin the SHA it produced.
include(FetchContent)

set(WHISPER_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS      OFF CACHE BOOL "" FORCE)

# No OpenMP runtime on Android — ggml falls back to its own thread pool, which is
# what n_threads drives from the Kotlin side.
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    whisper
    GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
    GIT_TAG        v1.7.4
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(whisper)

add_library(tinto_whisper SHARED whisper_jni.cpp)

find_library(android-log log)

target_link_libraries(tinto_whisper PRIVATE whisper ${android-log})

target_compile_options(tinto_whisper PRIVATE -O3 -fvisibility=hidden)

# Android 15 introduced 16 KB memory pages, and targetSdk 36 means we will meet
# those devices. NDK r28+ aligns to 16 KB by default; passing it explicitly keeps
# an older NDK from producing a library that silently fails to load.
target_link_options(tinto_whisper PRIVATE "-Wl,-z,max-page-size=16384")
