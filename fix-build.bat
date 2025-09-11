@echo off
echo ================================
echo Legado 构建修复脚本
echo ================================
echo.

echo 1. 清理Gradle缓存...
call gradlew.bat clean --refresh-dependencies
if %errorlevel% neq 0 (
    echo [错误] 清理缓存失败
    goto :error
)
echo.

echo 2. 清理本地构建缓存...
rmdir /s /q .gradle 2>nul
rmdir /s /q build 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q modules\book\build 2>nul
rmdir /s /q modules\rhino\build 2>nul
echo 本地缓存已清理
echo.

echo 3. 重新构建项目...
call gradlew.bat build --info
if %errorlevel% neq 0 (
    echo [错误] 构建失败，请查看上方错误信息
    goto :error
)
echo.

echo ================================
echo 构建修复完成！
echo ================================
goto :end

:error
echo.
echo ================================
echo 构建修复失败
echo ================================
echo 请检查网络连接和错误信息
pause
exit /b 1

:end
pause