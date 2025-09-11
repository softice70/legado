@echo off
echo ================================
echo 修复KSP路径问题
echo ================================

echo 1. 清理Gradle缓存...
rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
echo Gradle缓存已清理

echo.
echo 2. 清理项目构建缓存...
./gradlew clean
echo 项目缓存已清理

echo.
echo 3. 清理KSP生成的文件...
rmdir /s /q "app\build\generated\ksp" 2>nul
rmdir /s /q "app\build\tmp\kapt3" 2>nul
echo KSP缓存已清理

echo.
echo 4. 重新构建项目...
./gradlew :app:assembleDebug --no-daemon --no-build-cache

echo.
echo ================================
echo 修复完成！
echo ================================
pause