@echo off
setlocal enabledelayedexpansion

echo ================================
echo 逐步修复Gradle缓存问题
echo ================================

echo 当前目录: %CD%
echo 用户目录: %USERPROFILE%
echo.

echo 1. 检查Gradle守护进程状态...
./gradlew --stop
if !errorlevel! equ 0 (
    echo [成功] Gradle守护进程已停止
) else (
    echo [警告] 停止Gradle守护进程时出现问题
)
echo.

echo 2. 检查损坏的缓存文件...
set "CACHE_DIR=%USERPROFILE%\.gradle\caches\8.11.1\transforms\9a086b85e41549b20247cf2cfaaf9388"
if exist "%CACHE_DIR%" (
    echo [发现] 找到损坏的缓存目录: %CACHE_DIR%
    echo 正在删除...
    rmdir /s /q "%CACHE_DIR%"
    if !errorlevel! equ 0 (
        echo [成功] 损坏缓存已删除
    ) else (
        echo [失败] 无法删除缓存目录
    )
) else (
    echo [信息] 损坏的缓存目录不存在
)
echo.

echo 3. 清理transforms缓存...
set "TRANSFORMS_DIR=%USERPROFILE%\.gradle\caches\8.11.1\transforms"
if exist "%TRANSFORMS_DIR%" (
    echo [发现] transforms目录存在
    echo 正在清理transforms缓存...
    rmdir /s /q "%TRANSFORMS_DIR%"
    if !errorlevel! equ 0 (
        echo [成功] transforms缓存已清理
    ) else (
        echo [失败] 无法清理transforms缓存
    )
) else (
    echo [信息] transforms目录不存在
)
echo.

echo 4. 清理项目构建缓存...
if exist ".gradle" (
    echo 删除项目.gradle目录...
    rmdir /s /q ".gradle"
    echo [完成] 项目.gradle目录已删除
)

if exist "build" (
    echo 删除项目build目录...
    rmdir /s /q "build"
    echo [完成] 项目build目录已删除
)

if exist "app\build" (
    echo 删除app\build目录...
    rmdir /s /q "app\build"
    echo [完成] app\build目录已删除
)
echo.

echo 5. 测试Gradle是否正常...
echo 执行: ./gradlew --version
./gradlew --version
if !errorlevel! equ 0 (
    echo [成功] Gradle运行正常
) else (
    echo [失败] Gradle运行异常
    goto :error
)
echo.

echo 6. 执行清理命令...
echo 执行: ./gradlew clean --no-daemon --no-build-cache
./gradlew clean --no-daemon --no-build-cache
if !errorlevel! equ 0 (
    echo [成功] 项目清理完成
) else (
    echo [失败] 项目清理失败
    goto :error
)
echo.

echo ================================
echo 修复完成！现在可以尝试构建：
echo ./gradlew :app:assembleDebug --no-daemon
echo ================================
goto :end

:error
echo.
echo ================================
echo 修复过程中出现错误！
echo 请尝试手动执行以下命令：
echo 1. rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms"
echo 2. ./gradlew clean --no-daemon
echo 3. ./gradlew :app:assembleDebug --no-daemon
echo ================================

:end
pause