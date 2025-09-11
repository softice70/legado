@echo off
echo ================================
echo Legado 项目环境检查脚本
echo ================================
echo.

echo 1. 检查 Java 版本...
java -version
if %errorlevel% neq 0 (
    echo [错误] Java 未安装或未配置到 PATH
    echo 请安装 JDK 17 或更高版本
    goto :error
)
echo.

echo 2. 检查 Gradle 版本...
call gradlew.bat --version
if %errorlevel% neq 0 (
    echo [错误] Gradle Wrapper 执行失败
    echo 请检查网络连接或镜像源配置
    goto :error
)
echo.

echo 3. 检查 Android SDK...
if not defined ANDROID_HOME (
    echo [警告] ANDROID_HOME 环境变量未设置
    echo 请设置 Android SDK 路径
) else (
    echo ANDROID_HOME: %ANDROID_HOME%
)
echo.

echo 4. 检查项目依赖...
echo 正在检查项目依赖配置...
call gradlew.bat dependencies --configuration implementation > nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 依赖检查失败，可能需要网络连接
) else (
    echo [成功] 依赖配置检查通过
)
echo.

echo 5. 检查 Node.js (Web模块)...
node --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [信息] Node.js 未安装，Web模块将无法构建
) else (
    echo Node.js 版本:
    node --version
    echo NPM 版本:
    npm --version
)
echo.

echo ================================
echo 环境检查完成
echo ================================
echo.
echo 如果所有检查都通过，可以运行以下命令构建项目:
echo   gradlew.bat clean build
echo.
goto :end

:error
echo.
echo ================================
echo 环境检查失败
echo ================================
echo 请根据上述错误信息修复环境配置
pause
exit /b 1

:end
pause