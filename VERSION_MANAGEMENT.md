# Legado个人版本管理规划

## 🎯 版本策略概览

### 分支结构
- **master**: 纯净上游同步分支，仅用于同步官方更新
- **personal-dev**: 个人开发主分支，包含所有个人修改
- **feature/***: 功能开发分支，用于开发新特性
- **hotfix/***: 紧急修复分支，用于修复严重问题
- **release/***: 发布分支，用于准备发布版本

### 版本命名规范

#### 个人版本格式
`v{主版本}.{次版本}.{修订版本}-personal.{构建号}`

示例：
- `v3.24.1-personal.1` - 首个个人版本
- `v3.24.1-personal.2` - 基于3.24.1的第二个个人版本
- `v3.25.0-personal.1` - 基于3.25.0的首个个人版本

#### 标签命名
- `personal-v3.24.1-1` - 个人版本标签
- `upstream-sync-20250912` - 上游同步记录

## 🔄 日常维护流程

### 1. 上游同步流程
每月执行一次上游同步：

```bash
# 切换到master分支
git checkout master

# 获取上游更新
git fetch upstream

# 合并上游更新
git merge upstream/master

# 推送到远程
git push origin master

# 打标签记录同步点
git tag upstream-sync-$(date +%Y%m%d)
git push origin upstream-sync-$(date +%Y%m%d)
```

### 2. 个人版本更新流程

```bash
# 切换到个人开发分支
git checkout personal-dev

# 合并上游更新
git merge master

# 解决冲突（如有）
# 测试功能

# 更新版本号和文档
./scripts/update_personal_version.sh

# 创建发布标签
git tag personal-v$(date +%Y.%m.%d)-1
git push origin personal-dev --tags
```

### 3. 功能开发流程

```bash
# 创建功能分支
git checkout personal-dev
git checkout -b feature/new-feature-name

# 开发完成后
# 1. 测试功能
# 2. 更新文档
# 3. 合并回personal-dev
git checkout personal-dev
git merge feature/new-feature-name

# 4. 删除功能分支
git branch -d feature/new-feature-name
```

## 📋 版本发布计划

### 短期计划（未来3个月）
- **v3.24.1-personal.2**: 修复VoIP功能的已知问题
- **v3.24.1-personal.3**: 优化音频焦点处理逻辑
- **v3.25.0-personal.1**: 同步上游3.25.0版本

### 中期计划（未来6个月）
- **v3.25.x-personal.x**: 基于上游更新，持续优化
- **v3.26.0-personal.1**: 考虑添加更多个性化功能

### 长期计划（未来1年）
- **v4.0.0-personal.x**: 当上游发布4.0版本时同步
- 评估是否向上游贡献有价值的修改

## 🔧 自动化脚本

### 版本更新脚本
已创建`scripts/update_personal_version.sh`用于自动化版本管理。

### 上游同步脚本
使用现有的`scripts/sync_upstream.sh`进行上游同步。

## 📊 变更记录

### 当前版本状态
- **基础版本**: v3.24.1 (基于910e5d5b6)
- **个人修改**: 
  - VoIP通话暂停朗读功能
  - 国内镜像源配置
  - Firebase依赖移除

### 版本历史
- `v3.24.1-personal.1` (2025-09-12): 首个个人版本发布

## 🎯 质量保证

### 测试策略
1. **功能测试**: 每个个人修改都需要充分测试
2. **兼容性测试**: 确保与官方版本的数据兼容
3. **回归测试**: 上游同步后进行回归测试

### 回滚策略
- 保留每个稳定版本的标签
- 使用`git revert`而非强制推送
- 重要版本创建备份分支

## 📚 文档维护

### 需要维护的文档
- `PERSONAL_CHANGES.md`: 个人修改详细记录
- `VERSION_MANAGEMENT.md`: 本版本管理文档
- `CHANGELOG.md`: 更新日志
- 各功能的README文档

### 文档更新时机
- 每次发布新版本时
- 上游同步完成后
- 功能开发完成时

## 🚀 最佳实践建议

### 1. 提交规范
```
feat: 添加新功能
docs: 更新文档
fix: 修复问题
refactor: 代码重构
style: 代码格式
```

### 2. 代码审查
- 每次合并前自我审查
- 检查是否影响上游兼容性
- 确保文档同步更新

### 3. 备份策略
- 重要版本创建备份分支
- 定期推送到远程仓库
- 本地保留多个备份

### 4. 社区互动
- 定期查看上游项目动态
- 评估有价值的修改是否向上游贡献
- 参与社区讨论获取灵感

## ⚠️ 注意事项

1. **避免修改应用ID**: 除非确定不与官方版本冲突
2. **保持数据兼容**: 确保个人版本与官方版本数据兼容
3. **及时同步**: 避免与上游差异过大导致合并困难
4. **测试充分**: 每个修改都要充分测试后再发布

## 📞 联系方式

如有问题或建议：
- GitHub Issues: [仓库链接]/issues
- 文档更新: 直接编辑相关文档并提交PR

---

*最后更新: 2025-09-12*
*版本: v1.0*