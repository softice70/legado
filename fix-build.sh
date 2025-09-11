#!/bin/bash

echo "================================"
echo "Legado 构建修复脚本"
echo "================================"
echo

echo "1. 清理Gradle缓存..."
if ! ./gradlew clean --refresh-dependencies; then
    echo "[错误] 清理缓存失败"
    exit 1
fi
echo

echo "2. 清理本地构建缓存..."
rm -rf .gradle
rm -rf build
rm -rf app/build
rm -rf modules/book/build
rm -rf modules/rhino/build
echo "本地缓存已清理"
echo

echo "3. 重新构建项目..."
if ! ./gradlew build --info; then
    echo "[错误] 构建失败，请查看上方错误信息"
    exit 1
fi
echo

echo "================================"
echo "构建修复完成！"
echo "================================"