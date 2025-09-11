# PowerShell脚本：修复Gradle缓存问题
Write-Host "================================" -ForegroundColor Green
Write-Host "逐步修复Gradle缓存问题" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

Write-Host "当前目录: $(Get-Location)" -ForegroundColor Yellow
Write-Host "用户目录: $env:USERPROFILE" -ForegroundColor Yellow
Write-Host ""

# 1. 停止Gradle守护进程
Write-Host "1. 检查Gradle守护进程状态..." -ForegroundColor Cyan
try {
    & ./gradlew --stop
    Write-Host "[成功] Gradle守护进程已停止" -ForegroundColor Green
} catch {
    Write-Host "[警告] 停止Gradle守护进程时出现问题: $($_.Exception.Message)" -ForegroundColor Yellow
}
Write-Host ""

# 2. 检查并删除损坏的缓存
Write-Host "2. 检查损坏的缓存文件..." -ForegroundColor Cyan
$cacheDir = "$env:USERPROFILE\.gradle\caches\8.11.1\transforms\9a086b85e41549b20247cf2cfaaf9388"
if (Test-Path $cacheDir) {
    Write-Host "[发现] 找到损坏的缓存目录: $cacheDir" -ForegroundColor Yellow
    try {
        Remove-Item -Path $cacheDir -Recurse -Force
        Write-Host "[成功] 损坏缓存已删除" -ForegroundColor Green
    } catch {
        Write-Host "[失败] 无法删除缓存目录: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "[信息] 损坏的缓存目录不存在" -ForegroundColor Gray
}
Write-Host ""

# 3. 清理transforms缓存
Write-Host "3. 清理transforms缓存..." -ForegroundColor Cyan
$transformsDir = "$env:USERPROFILE\.gradle\caches\8.11.1\transforms"
if (Test-Path $transformsDir) {
    Write-Host "[发现] transforms目录存在" -ForegroundColor Yellow
    try {
        Remove-Item -Path $transformsDir -Recurse -Force
        Write-Host "[成功] transforms缓存已清理" -ForegroundColor Green
    } catch {
        Write-Host "[失败] 无法清理transforms缓存: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "[信息] transforms目录不存在" -ForegroundColor Gray
}
Write-Host ""

# 4. 清理项目构建缓存
Write-Host "4. 清理项目构建缓存..." -ForegroundColor Cyan
$dirsToClean = @(".gradle", "build", "app\build", "modules\book\build", "modules\rhino\build")
foreach ($dir in $dirsToClean) {
    if (Test-Path $dir) {
        Write-Host "删除目录: $dir" -ForegroundColor Yellow
        try {
            Remove-Item -Path $dir -Recurse -Force
            Write-Host "[完成] $dir 目录已删除" -ForegroundColor Green
        } catch {
            Write-Host "[失败] 无法删除 $dir : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
Write-Host ""

# 5. 测试Gradle
Write-Host "5. 测试Gradle是否正常..." -ForegroundColor Cyan
try {
    Write-Host "执行: ./gradlew --version" -ForegroundColor Gray
    & ./gradlew --version
    Write-Host "[成功] Gradle运行正常" -ForegroundColor Green
} catch {
    Write-Host "[失败] Gradle运行异常: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "请检查Gradle安装和环境变量" -ForegroundColor Yellow
    return
}
Write-Host ""

# 6. 执行清理
Write-Host "6. 执行清理命令..." -ForegroundColor Cyan
try {
    Write-Host "执行: ./gradlew clean --no-daemon --no-build-cache" -ForegroundColor Gray
    & ./gradlew clean --no-daemon --no-build-cache
    Write-Host "[成功] 项目清理完成" -ForegroundColor Green
} catch {
    Write-Host "[失败] 项目清理失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "请手动执行: ./gradlew clean --no-daemon" -ForegroundColor Yellow
    return
}
Write-Host ""

Write-Host "================================" -ForegroundColor Green
Write-Host "修复完成！现在可以尝试构建：" -ForegroundColor Green
Write-Host "./gradlew :app:assembleDebug --no-daemon" -ForegroundColor White
Write-Host "================================" -ForegroundColor Green

Read-Host "按回车键继续..."