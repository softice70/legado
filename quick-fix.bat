@echo off
echo ================================
echo 快速修复Gradle缓存问题
echo ================================

echo 1. 停止Gradle守护进程...
./gradlew --stop

echo.
echo 2. 删除损坏的缓存文件...
echo 删除transforms缓存...
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms" 2>nul
echo 删除分析缓存...
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\scripts" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\kotlin-dsl" 2>nul

echo.
echo 3. 清理项目构建文件...
./gradlew clean --no-daemon

echo.
echo 4. 尝试重新构建...
echo 使用无缓存模式构建...
./gradlew :app:assembleDebug --no-daemon --no-build-cache --offline

echo.
echo 如果上面失败，尝试在线模式...
./gradlew :app:assembleDebug --no-daemon --no-build-cache --refresh-dependencies

echo.
echo ================================
echo 修复完成！
echo ================================
pause