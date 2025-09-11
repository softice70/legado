# Firebase 依赖移除说明

## 移除原因
为了解决构建过程中Firebase依赖下载失败的问题，已将Firebase相关依赖从项目中移除。

## 已移除的内容

### 1. 依赖库移除
在 `app/build.gradle` 中注释了以下依赖：
```gradle
// implementation platform(libs.firebase.bom)
// implementation libs.firebase.analytics
// implementation libs.firebase.perf
```

### 2. 插件移除
在 `app/build.gradle` 和 `build.gradle` 中注释了：
```gradle
// alias libs.plugins.google.services
```

### 3. 配置文件处理
- **备份**: `app/google-services.json` → `app/google-services.json.bak`
- **移除**: 删除了 `app/google-services.json` 文件

## 功能影响分析

### ❌ 失去的功能
1. **用户行为分析** - 无法收集用户使用统计数据
2. **性能监控** - 无法监控应用性能指标
3. **崩溃统计** - 无法自动收集崩溃报告

### ✅ 不受影响的功能
1. **所有核心阅读功能** - 完全正常
2. **书源管理** - 完全正常
3. **本地/在线图书** - 完全正常
4. **WebDAV同步** - 完全正常
5. **TTS朗读** - 完全正常
6. **主题设置** - 完全正常
7. **所有用户界面** - 完全正常

## 恢复Firebase的方法

如果将来需要恢复Firebase功能：

### 1. 恢复配置文件
```bash
copy app\google-services.json.bak app\google-services.json
```

### 2. 恢复依赖
在 `app/build.gradle` 中取消注释：
```gradle
implementation platform(libs.firebase.bom)
implementation libs.firebase.analytics
implementation libs.firebase.perf
```

### 3. 恢复插件
在 `app/build.gradle` 和 `build.gradle` 中取消注释：
```gradle
alias libs.plugins.google.services
```

## 构建验证

移除Firebase后，建议执行以下命令验证构建：

```bash
# 清理缓存
./gradlew clean

# 重新构建
./gradlew build
```

## 注意事项

1. **数据收集**: 移除后将无法收集用户使用数据
2. **调试信息**: 无法获取自动崩溃报告，需要依赖用户反馈
3. **性能监控**: 无法监控应用在用户设备上的性能表现
4. **版本兼容**: 移除不影响应用的版本兼容性

## 更新日期
2025年1月11日