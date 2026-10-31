#!/usr/bin/env bash
# 构建环境变量。默认值按常见布局猜；用自己的环境变量覆盖即可。
export JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(command -v java 2>/dev/null || echo /usr/bin/java)")")}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
