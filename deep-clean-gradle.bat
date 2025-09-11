@echo off
echo ================================
echo 深度清理Gradle缓存
echo ================================

echo 1. 停止所有Gradle守护进程...
./gradlew --stop
echo Gradle守护进程已停止

echo.
echo 2. 清理用户Gradle目录...
echo 正在删除: %USERPROFILE%\.gradle\caches
rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
echo 正在删除: %USERPROFILE%\.gradle\daemon
rmdir /s /q "%USERPROFILE%\.gradle\daemon" 2>nul
echo 正在删除: %USERPROFILE%\.gradle\wrapper
rmdir /s /q "%USERPROFILE%\.gradle\wrapper" 2>nul
echo 用户Gradle缓存已清理

echo.
echo 3. 清理项目本地缓存...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "build" 2>nul
rmdir /s /q "app\build" 2>nul
rmdir /s /q "modules\book\build" 2>nul
rmdir /s /q "modules\rhino\build" 2>nul
rmdir /s /q "modules\web\build" 2>nul
echo 项目缓存已清理

echo.
echo 4. 清理Gradle Wrapper...
rmdir /s /q "gradle\wrapper" 2>nul
echo Gradle Wrapper已清理

echo.
echo 5. 重新下载Gradle Wrapper...
echo 正在重新初始化Gradle Wrapper...
gradle wrapper --gradle-version 8.11.1 --distribution-type bin

echo.
echo 6. 验证Gradle安装...
./gradlew --version

echo.
echo 7. 尝试基础构建...
./gradlew clean --no-daemon --no-build-cache --refresh-dependencies

echo.
echo ================================
echo 深度清理完成！
echo 现在可以尝试构建项目了
echo ================================
pause