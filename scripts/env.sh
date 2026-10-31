#!/usr/bin/env bash
export JAVA_HOME=$(ls -d /f/Android/jdk-21* | head -1)
export ANDROID_HOME=/f/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export GRADLE_USER_HOME=/f/Android/gradle-home
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:/f/Android/gradle-8.13/bin:$PATH"
