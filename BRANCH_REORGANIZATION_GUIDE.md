# 分支重新整理指南

## 问题描述
您在fork项目后，直接在本地master分支上进行了开发并提交了两次修改，现在希望：
1. 从fork时的版本创建personal-dev分支
2. 将后续的两次修改移动到新分支上
3. 保持master分支与上游同步

## 解决方案

### 方案一：使用cherry-pick（推荐）

这是最安全的方法，可以精确控制哪些提交被移动。

```bash
# 1. 查看当前提交历史，找到fork时的提交点
git log --oneline -10

# 假设输出如下：
# abc1234 (HEAD -> master) 第二次个人修改
# def5678 第一次个人修改  
# xyz9012 fork时的最后一个上游提交
# ...

# 2. 记录您的两次个人修改的commit hash
# 第一次修改: def5678
# 第二次修改: abc1234

# 3. 重置master到fork时的状态
git reset --hard xyz9012  # 替换为实际的fork时提交hash

# 4. 从当前位置创建personal-dev分支
git checkout -b personal-dev

# 5. 将您的修改cherry-pick到personal-dev分支
git cherry-pick def5678  # 第一次修改
git cherry-pick abc1234  # 第二次修改

# 6. 推送personal-dev分支到远程
git push -u origin personal-dev

# 7. 同步master分支到最新的上游状态
git checkout master
git fetch upstream
git merge upstream/master
git push origin master
```

### 方案二：使用分支重命名

如果您希望保留当前的提交历史结构：

```bash
# 1. 将当前的master分支重命名为personal-dev
git branch -m master personal-dev

# 2. 推送personal-dev分支到远程
git push -u origin personal-dev

# 3. 从上游重新创建master分支
git fetch upstream
git checkout -b master upstream/master

# 4. 推送新的master分支到远程
git push -u origin master

# 5. 设置master分支跟踪上游
git branch --set-upstream-to=upstream/master master
```

### 方案三：使用rebase（高级用户）

如果您熟悉Git rebase操作：

```bash
# 1. 查看提交历史，确定fork点
git log --oneline --graph

# 2. 创建personal-dev分支（包含您的修改）
git checkout -b personal-dev

# 3. 推送personal-dev分支
git push -u origin personal-dev

# 4. 切换回master并重置到fork点
git checkout master
git reset --hard <fork-point-hash>

# 5. 同步上游更新
git fetch upstream
git merge upstream/master
git push origin master --force-with-lease
```

## 详细步骤说明

### 步骤1: 确定fork点
```bash
# 查看详细的提交历史
git log --oneline --graph --all

# 或者查看与上游的差异
git fetch upstream
git log --oneline master ^upstream/master
```

### 步骤2: 备份当前状态（安全措施）
```bash
# 创建备份分支
git branch backup-before-reorganization

# 或者创建标签
git tag backup-$(date +%Y%m%d-%H%M%S)
```

### 步骤3: 执行重新整理
选择上述方案之一执行。

### 步骤4: 验证结果
```bash
# 检查分支状态
git branch -a

# 检查personal-dev分支包含您的修改
git checkout personal-dev
git log --oneline -5

# 检查master分支与上游同步
git checkout master
git log --oneline -5
```

## 实际操作示例

假设您的情况如下：
```
* abc1234 (HEAD -> master) fix: 修复VoIP通话暂停问题
* def5678 feat: 优化音频焦点处理
* xyz9012 (upstream/master) 上游最新提交
* uvw3456 更早的上游提交
```

### 执行方案一（推荐）：
```bash
# 1. 重置master到fork点
git reset --hard xyz9012

# 2. 创建personal-dev分支
git checkout -b personal-dev

# 3. 应用您的修改
git cherry-pick def5678
git cherry-pick abc1234

# 4. 推送分支
git push -u origin personal-dev

# 5. 更新master
git checkout master
git fetch upstream
git merge upstream/master
git push origin master
```

## 注意事项

### ⚠️ 重要警告
- 如果您已经将修改推送到远程master分支，需要使用`--force-with-lease`
- 在执行前务必创建备份分支或标签
- 如果有其他人基于您的master分支工作，需要协调处理

### 🔍 验证检查清单
- [ ] personal-dev分支包含您的所有个人修改
- [ ] master分支与上游保持同步
- [ ] 远程仓库的分支结构正确
- [ ] 没有丢失任何重要提交

### 🚨 如果出错了
```bash
# 恢复到备份状态
git checkout backup-before-reorganization
git branch -D personal-dev
git branch -D master
git checkout -b master
git checkout -b personal-dev backup-before-reorganization

# 或者从标签恢复
git reset --hard backup-20250912-1000
```

## 后续维护

重新整理完成后，按照正常的工作流程：

```bash
# 日常开发在personal-dev分支
git checkout personal-dev

# 定期同步上游更新
./scripts/sync_upstream.sh

# 使用开发工作流脚本
./scripts/dev_workflow.sh status
```

## 总结

推荐使用**方案一（cherry-pick）**，因为它：
- ✅ 最安全，不会丢失数据
- ✅ 可以精确控制哪些提交被移动
- ✅ 保持清晰的提交历史
- ✅ 易于理解和执行

完成后，您将拥有：
- `master`分支：与上游保持同步
- `personal-dev`分支：包含您的个人修改
- 清晰的分支结构，便于后续维护