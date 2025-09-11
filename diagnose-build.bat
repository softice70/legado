@echo off
echo ================================
echo Legado 构建诊断脚本
echo ================================
echo.

echo 1. 检查Gradle守护进程...
call gradlew.bat --status
echo.

echo 2. 清理项目...
call gradlew.bat clean
echo.

echo 3. 检查依赖解析...
echo 正在检查依赖冲突...
call gradlew.bat dependencies --configuration debugCompileClasspath > dependencies.log 2>&1
if %errorlevel% neq 0 (
    echo [错误] 依赖解析失败，请查看 dependencies.log 文件
) else (
    echo [成功] 依赖解析正常
)
echo.

echo 4. 尝试构建APK（详细日志）...
call gradlew.bat assembleDebug --info --stacktrace > build.log 2>&1
if %errorlevel% neq 0 (
    echo [错误] APK构建失败
    echo 错误日志已保存到 build.log 文件
    echo.
    echo 显示最后50行错误信息：
    powershell "Get-Content build.log | Select-Object -Last 50"
) else (
    echo [成功] APK构建成功！
    echo APK文件位置：app\build\outputs\apk\app\debug\
)
echo.

echo 5. 检查生成的APK文件...
if exist "app\build\outputs\apk\app\debug\*.apk" (
    echo [成功] 找到APK文件：
    dir "app\build\outputs\apk\app\debug\*.apk" /b
) else (
    echo [错误] 未找到APK文件
)
echo.

echo ================================
echo 诊断完成
echo ================================
echo 如果构建失败，请查看 build.log 文件获取详细错误信息
pause