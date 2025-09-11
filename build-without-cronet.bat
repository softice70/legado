@echo off
echo ================================
echo 跳过Cronet下载的构建测试
echo ================================
echo.

echo 1. 备份download.gradle文件...
if exist "app\download.gradle" (
    copy "app\download.gradle" "app\download.gradle.bak"
    echo 已备份 download.gradle
) else (
    echo download.gradle 文件不存在
)
echo.

echo 2. 临时禁用download.gradle...
echo // 临时禁用Cronet下载 > "app\download.gradle.temp"
echo // apply plugin: 'de.undercouch.download' >> "app\download.gradle.temp"
move "app\download.gradle.temp" "app\download.gradle"
echo.

echo 3. 清理项目...
call gradlew.bat clean
echo.

echo 4. 尝试构建APK...
call gradlew.bat assembleDebug --info > build-test.log 2>&1
if %errorlevel% neq 0 (
    echo [错误] 构建失败，查看错误信息：
    echo.
    powershell "Get-Content build-test.log | Select-Object -Last 30"
) else (
    echo [成功] 构建成功！
    if exist "app\build\outputs\apk\app\debug\*.apk" (
        echo 找到APK文件：
        dir "app\build\outputs\apk\app\debug\*.apk" /b
    )
)
echo.

echo 5. 恢复download.gradle文件...
if exist "app\download.gradle.bak" (
    move "app\download.gradle.bak" "app\download.gradle"
    echo 已恢复 download.gradle
)
echo.

echo ================================
echo 测试完成
echo ================================
pause