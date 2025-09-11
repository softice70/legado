@echo off
echo ================================
echo 紧急修复 - 解决Gradle缓存损坏
echo ================================

echo 正在停止所有Gradle进程...
taskkill /f /im java.exe 2>nul
./gradlew --stop 2>nul

echo.
echo 删除特定的损坏缓存...
del /f /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms\9a086b85e41549b20247cf2cfaaf9388\transformed\analysis\*" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms\9a086b85e41549b20247cf2cfaaf9388" 2>nul

echo.
echo 清理项目缓存...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "build" 2>nul
rmdir /s /q "app\build" 2>nul

echo.
echo 尝试最小化构建...
./gradlew clean --no-daemon --no-build-cache

echo.
echo ================================
echo 现在请手动执行以下命令测试：
echo ./gradlew :app:assembleDebug --no-daemon
echo ================================
pause