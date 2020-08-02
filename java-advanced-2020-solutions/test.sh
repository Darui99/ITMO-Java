#!/bin/bash

DIRECTORY="out/production"

ROOT=".."
MYPROJECT="$ROOT/java-advanced-2020-solutions"
TEACHERSPROJECT="$ROOT/java-advanced-2020"

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.concurrent advanced ru.ifmo.rain.kurbatov.concurrent.IterativeParallelism ${1}

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.mapper advanced ru.ifmo.rain.kurbatov.concurrent.IterativeParallelism ${1}

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.crawler hard ru.ifmo.rain.kurbatov.crawler.WebCrawler ${1}

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.hello client-evil ru.ifmo.rain.kurbatov.hello.HelloUDPClient ${1}

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.hello server-evil ru.ifmo.rain.kurbatov.hello.HelloUDPServer ${1}

java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
      -m info.kgeorgiy.java.advanced.hello server-evil ru.ifmo.rain.kurbatov.hello.HelloUDPNonblockingServer ${1}

#java  -cp "$DIRECTORY/java-advanced-2020-solutions" -p "$TEACHERSPROJECT/artifacts":"$TEACHERSPROJECT/lib":"$DIRECTORY/." \
#      -m info.kgeorgiy.java.advanced.hello client-evil ru.ifmo.rain.kurbatov.hello.HelloUDPNonblockingClient ${1}
