#!/bin/bash

# Legado个人Fork版本 - 上游同步脚本
# 用法: ./sync_upstream.sh [--force]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否在git仓库中
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "当前目录不是Git仓库"
    exit 1
fi

# 检查是否有未提交的更改
if ! git diff-index --quiet HEAD --; then
    print_warning "检测到未提交的更改"
    if [[ "$1" != "--force" ]]; then
        print_error "请先提交或暂存更改，或使用 --force 参数强制执行"
        exit 1
    else
        print_warning "使用 --force 参数，将暂存当前更改"
        git stash push -m "自动暂存 - $(date)"
    fi
fi

# 保存当前分支
current_branch=$(git branch --show-current)
print_info "当前分支: $current_branch"

# 检查upstream远程仓库是否存在
if ! git remote get-url upstream > /dev/null 2>&1; then
    print_warning "未找到upstream远程仓库，正在添加..."
    git remote add upstream https://github.com/gedoor/legado.git
    print_success "已添加upstream远程仓库"
fi

print_info "开始同步上游更新..."

# 获取上游更新
print_info "获取上游更新..."
git fetch upstream

# 切换到master分支
print_info "切换到master分支..."
git checkout master

# 检查master分支是否干净
if ! git diff-index --quiet HEAD --; then
    print_error "master分支有未提交的更改，请手动处理"
    git checkout "$current_branch"
    exit 1
fi

# 合并上游master
print_info "合并上游master分支..."
if git merge upstream/master; then
    print_success "成功合并上游更新到master分支"
else
    print_error "合并失败，请手动解决冲突"
    git checkout "$current_branch"
    exit 1
fi

# 推送master分支
print_info "推送master分支到远程..."
git push origin master

# 切换到personal-dev分支
if git show-ref --verify --quiet refs/heads/personal-dev; then
    print_info "切换到personal-dev分支..."
    git checkout personal-dev
    
    # 合并master到personal-dev
    print_info "合并master更新到personal-dev分支..."
    if git merge master; then
        print_success "成功合并master更新到personal-dev分支"
        
        # 推送personal-dev分支
        print_info "推送personal-dev分支到远程..."
        git push origin personal-dev
        print_success "personal-dev分支已更新"
    else
        print_warning "合并到personal-dev时出现冲突"
        print_info "请手动解决冲突后执行:"
        print_info "  git add ."
        print_info "  git commit -m 'merge: 解决与上游更新的合并冲突'"
        print_info "  git push origin personal-dev"
        exit 1
    fi
else
    print_warning "personal-dev分支不存在，跳过合并"
fi

# 回到原分支
if [[ "$current_branch" != "master" ]] && [[ "$current_branch" != "personal-dev" ]]; then
    print_info "切换回原分支: $current_branch"
    git checkout "$current_branch"
fi

# 恢复暂存的更改（如果有）
if [[ "$1" == "--force" ]] && git stash list | grep -q "自动暂存"; then
    print_info "恢复暂存的更改..."
    git stash pop
fi

print_success "上游同步完成！"

# 显示最新的几个提交
print_info "最新提交:"
git log --oneline -5