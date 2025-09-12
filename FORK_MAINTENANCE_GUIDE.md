# Legado个人Fork版本维护指南

## 概述
本指南介绍如何维护一个基于gedoor/legado的个人fork版本，同时能够持续获取上游更新并合并。

## 1. 初始设置

### 1.1 配置远程仓库
```bash
# 查看当前远程仓库
git remote -v

# 添加上游仓库（如果还没有添加）
git remote add upstream https://github.com/gedoor/legado.git

# 验证远程仓库配置
git remote -v
# 应该显示：
# origin    https://github.com/YOUR_USERNAME/legado.git (fetch)
# origin    https://github.com/YOUR_USERNAME/legado.git (push)
# upstream  https://github.com/gedoor/legado.git (fetch)
# upstream  https://github.com/gedoor/legado.git (push)
```

### 1.2 创建个人开发分支

#### 情况一：全新开始（推荐）
```bash
# 基于master创建个人开发分支
git checkout -b personal-dev

# 推送到远程仓库
git push -u origin personal-dev
```

#### 情况二：已在master分支开发（需要重新整理）
如果您已经在master分支上进行了开发，需要重新整理分支结构：

```bash
# 使用自动化脚本（推荐）
./scripts/reorganize_branches.sh

# 或者手动操作（参考 BRANCH_REORGANIZATION_GUIDE.md）
```

**重新整理后的分支结构：**
- `master`: 与上游保持同步的纯净分支
- `personal-dev`: 包含您个人修改的开发分支

## 2. 分支策略

### 2.1 推荐的分支结构
- `master`: 保持与上游同步的纯净分支
- `personal-dev`: 个人开发主分支，包含所有个人修改
- `feature/xxx`: 功能开发分支（可选）
- `hotfix/xxx`: 紧急修复分支（可选）

### 2.2 分支用途说明
- **master分支**: 仅用于同步上游更新，不在此分支直接开发
- **personal-dev分支**: 个人修改的主分支，日常开发在此进行
- **功能分支**: 开发大型功能时可创建独立分支，完成后合并到personal-dev

## 3. 日常开发工作流

### 3.1 开发新功能
```bash
# 切换到personal-dev分支
git checkout personal-dev

# 确保是最新状态
git pull origin personal-dev

# 创建功能分支（可选，小修改可直接在personal-dev开发）
git checkout -b feature/voip-pause-enhancement

# 进行开发...
# 提交更改
git add .
git commit -m "feat: 优化VoIP通话暂停功能"

# 推送到远程
git push origin feature/voip-pause-enhancement

# 合并到personal-dev（或通过PR）
git checkout personal-dev
git merge feature/voip-pause-enhancement
git push origin personal-dev

# 删除功能分支
git branch -d feature/voip-pause-enhancement
git push origin --delete feature/voip-pause-enhancement
```

### 3.2 提交规范建议
```bash
# 功能增强
git commit -m "feat: 添加VoIP通话检测功能"

# 问题修复
git commit -m "fix: 修复朗读中接听语音通话无法暂停的问题"

# 代码重构
git commit -m "refactor: 清理音频焦点处理相关调试日志"

# 文档更新
git commit -m "docs: 更新fork维护指南"

# 样式调整
git commit -m "style: 统一代码格式"
```

## 4. 同步上游更新

### 4.1 定期同步流程
```bash
# 1. 切换到master分支
git checkout master

# 2. 获取上游更新
git fetch upstream

# 3. 合并上游master到本地master
git merge upstream/master

# 4. 推送更新到自己的远程仓库
git push origin master

# 5. 切换到personal-dev分支
git checkout personal-dev

# 6. 合并master的更新到personal-dev
git merge master
```

### 4.2 处理合并冲突
```bash
# 如果出现冲突，Git会提示冲突文件
# 手动解决冲突后：
git add .
git commit -m "merge: 解决与上游更新的合并冲突"
git push origin personal-dev
```

### 4.3 自动化同步脚本
创建 `sync_upstream.sh` 脚本：
```bash
#!/bin/bash
echo "开始同步上游更新..."

# 保存当前分支
current_branch=$(git branch --show-current)

# 切换到master并同步
git checkout master
git fetch upstream
git merge upstream/master
git push origin master

# 切换到personal-dev并合并更新
git checkout personal-dev
git merge master

if [ $? -eq 0 ]; then
    echo "同步成功！"
    git push origin personal-dev
else
    echo "出现合并冲突，请手动解决"
fi

# 回到原分支
git checkout $current_branch
```

## 5. 版本管理策略

### 5.1 标签管理
```bash
# 为个人版本打标签
git tag -a v3.24.1-personal.1 -m "个人版本 v3.24.1-personal.1: 优化VoIP通话暂停功能"
git push origin v3.24.1-personal.1

# 查看所有标签
git tag -l
```

### 5.2 版本号规范
- 基础版本号跟随上游：`v3.24.1`
- 个人版本后缀：`-personal.x`
- 示例：`v3.24.1-personal.1`, `v3.24.1-personal.2`

## 6. 发布管理

### 6.1 创建Release
```bash
# 基于personal-dev创建release分支
git checkout personal-dev
git checkout -b release/v3.24.1-personal.1

# 进行最后的调整和测试
# 提交并推送
git push origin release/v3.24.1-personal.1

# 合并到master（如果需要）
git checkout master
git merge release/v3.24.1-personal.1
git tag -a v3.24.1-personal.1 -m "Release v3.24.1-personal.1"
git push origin master --tags
```

## 7. 最佳实践

### 7.1 定期维护
- **每周检查**: 检查上游是否有重要更新
- **每月同步**: 定期同步上游更新到个人版本
- **及时备份**: 重要修改及时推送到远程仓库

### 7.2 冲突预防
- **小步提交**: 频繁提交小的更改，避免大量冲突
- **关注上游**: 关注上游项目的重大变更
- **文档记录**: 记录个人修改的详细信息

### 7.3 代码组织
- **模块化修改**: 将个人修改组织成独立的模块
- **配置分离**: 个人配置与核心代码分离
- **向后兼容**: 尽量保持与上游的兼容性

## 8. 应急处理

### 8.1 回滚操作
```bash
# 回滚到上一个提交
git reset --hard HEAD~1

# 回滚到特定提交
git reset --hard <commit-hash>

# 创建反向提交
git revert <commit-hash>
```

### 8.2 强制同步上游
```bash
# 警告：这会丢失所有本地修改
git checkout master
git fetch upstream
git reset --hard upstream/master
git push origin master --force
```

## 9. 工具推荐

### 9.1 Git GUI工具
- **SourceTree**: 可视化Git操作
- **GitKraken**: 强大的Git客户端
- **VS Code Git**: 集成在编辑器中

### 9.2 自动化工具
- **GitHub Actions**: 自动化CI/CD
- **Dependabot**: 依赖更新提醒
- **Renovate**: 自动化依赖管理

## 10. 注意事项

### 10.1 许可证合规
- 保持原项目的许可证
- 在README中说明fork来源
- 遵守开源协议要求

### 10.2 贡献回馈
- 有价值的修改可考虑提PR给上游
- 参与上游社区讨论
- 分享使用经验和改进建议

---

## 快速参考命令

```bash
# 日常开发
git checkout personal-dev
git pull origin personal-dev
# ... 开发 ...
git add .
git commit -m "feat: 新功能"
git push origin personal-dev

# 同步上游
git checkout master
git fetch upstream
git merge upstream/master
git push origin master
git checkout personal-dev
git merge master
git push origin personal-dev

# 创建标签
git tag -a v3.24.1-personal.1 -m "个人版本发布"
git push origin --tags