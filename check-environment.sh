#!/bin/bash

echo "================================"
echo "Legado 项目环境检查脚本"
echo "================================"
echo

echo "1. 检查 Java 版本..."
if ! command -v java &> /dev/null; then
    echo "[错误] Java 未安装或未配置到 PATH"
    echo "请安装 JDK 17 或更高版本"
    exit 1
fi

java -version
echo

echo "2. 检查 Gradle 版本..."
if ! ./gradlew --version; then
    echo "[错误] Gradle Wrapper 执行失败"
    echo "请检查网络连接或镜像源配置"
    exit 1
fi
echo

echo "3. 检查 Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "[警告] ANDROID_HOME 环境变量未设置"
    echo "请设置 Android SDK 路径"
else
    echo "ANDROID_HOME: $ANDROID_HOME"
fi
echo

echo "4. 检查项目依赖..."
echo "正在检查项目依赖配置..."
if ./gradlew dependencies --configuration implementation > /dev/null 2>&1; then
    echo "[成功] 依赖配置检查通过"
else
    echo "[警告] 依赖检查失败，可能需要网络连接"
fi
echo

echo "5. 检查 Node.js (Web模块)..."
if ! command -v node &> /dev/null; then
    echo "[信息] Node.js 未安装，Web模块将无法构建"
else
    echo "Node.js 版本:"
    node --version
    echo "NPM 版本:"
    npm --version
fi
echo

echo "================================"
echo "环境检查完成"
echo "================================"
echo
echo "如果所有检查都通过，可以运行以下命令构建项目:"
echo "  ./gradlew clean build"
echo