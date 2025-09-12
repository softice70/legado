#!/bin/bash

# Legado个人Fork版本 - 开发工作流脚本
# 用法: ./dev_workflow.sh <command> [options]

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

# 显示帮助信息
show_help() {
    echo "Legado个人Fork版本开发工作流脚本"
    echo ""
    echo "用法: $0 <command> [options]"
    echo ""
    echo "命令:"
    echo "  init                    初始化开发环境"
    echo "  feature <name>          创建新功能分支"
    echo "  finish [branch]         完成功能开发并合并到personal-dev"
    echo "  commit <message>        智能提交（自动添加类型前缀）"
    echo "  release <version>       创建发布版本"
    echo "  status                  显示当前状态"
    echo "  clean                   清理已合并的分支"
    echo ""
    echo "示例:"
    echo "  $0 init"
    echo "  $0 feature voip-enhancement"
    echo "  $0 commit '修复VoIP通话暂停问题'"
    echo "  $0 finish feature/voip-enhancement"
    echo "  $0 release v3.24.1-personal.1"
}

# 初始化开发环境
init_dev_env() {
    print_info "初始化Legado个人开发环境..."
    
    # 检查upstream远程仓库
    if ! git remote get-url upstream > /dev/null 2>&1; then
        print_info "添加upstream远程仓库..."
        git remote add upstream https://github.com/gedoor/legado.git
    fi
    
    # 创建personal-dev分支（如果不存在）
    if ! git show-ref --verify --quiet refs/heads/personal-dev; then
        print_info "创建personal-dev分支..."
        git checkout -b personal-dev
        git push -u origin personal-dev
    fi
    
    # 切换到personal-dev分支
    git checkout personal-dev
    
    print_success "开发环境初始化完成！"
    print_info "当前分支: $(git branch --show-current)"
}

# 创建功能分支
create_feature_branch() {
    local feature_name="$1"
    if [[ -z "$feature_name" ]]; then
        print_error "请提供功能分支名称"
        echo "用法: $0 feature <name>"
        exit 1
    fi
    
    local branch_name="feature/$feature_name"
    
    # 确保在personal-dev分支
    git checkout personal-dev
    git pull origin personal-dev
    
    # 创建功能分支
    print_info "创建功能分支: $branch_name"
    git checkout -b "$branch_name"
    git push -u origin "$branch_name"
    
    print_success "功能分支 '$branch_name' 创建完成！"
    print_info "现在可以开始开发功能: $feature_name"
}

# 完成功能开发
finish_feature() {
    local branch_name="$1"
    local current_branch=$(git branch --show-current)
    
    # 如果没有指定分支，使用当前分支
    if [[ -z "$branch_name" ]]; then
        branch_name="$current_branch"
    fi
    
    # 检查是否是功能分支
    if [[ ! "$branch_name" =~ ^feature/ ]]; then
        print_error "只能完成feature/开头的分支"
        exit 1
    fi
    
    print_info "完成功能分支: $branch_name"
    
    # 切换到功能分支并推送最新更改
    git checkout "$branch_name"
    git push origin "$branch_name"
    
    # 切换到personal-dev并合并
    git checkout personal-dev
    git pull origin personal-dev
    git merge "$branch_name" --no-ff -m "feat: 合并功能分支 $branch_name"
    git push origin personal-dev
    
    # 删除功能分支
    print_info "删除功能分支..."
    git branch -d "$branch_name"
    git push origin --delete "$branch_name"
    
    print_success "功能开发完成并已合并到personal-dev分支！"
}

# 智能提交
smart_commit() {
    local message="$1"
    if [[ -z "$message" ]]; then
        print_error "请提供提交消息"
        echo "用法: $0 commit '<message>'"
        exit 1
    fi
    
    # 检查是否有更改
    if git diff-index --quiet HEAD --; then
        print_warning "没有检测到更改"
        exit 0
    fi
    
    # 智能判断提交类型
    local commit_type=""
    local files_changed=$(git diff --name-only --cached 2>/dev/null || git diff --name-only)
    
    if echo "$files_changed" | grep -q "\.md$\|README\|CHANGELOG"; then
        commit_type="docs"
    elif echo "$message" | grep -qi "fix\|修复\|解决"; then
        commit_type="fix"
    elif echo "$message" | grep -qi "feat\|功能\|新增\|添加"; then
        commit_type="feat"
    elif echo "$message" | grep -qi "refactor\|重构"; then
        commit_type="refactor"
    elif echo "$message" | grep -qi "style\|格式\|样式"; then
        commit_type="style"
    elif echo "$message" | grep -qi "test\|测试"; then
        commit_type="test"
    else
        commit_type="chore"
    fi
    
    # 如果消息已经有类型前缀，不重复添加
    if [[ "$message" =~ ^(feat|fix|docs|style|refactor|test|chore): ]]; then
        local final_message="$message"
    else
        local final_message="$commit_type: $message"
    fi
    
    print_info "提交类型: $commit_type"
    print_info "提交消息: $final_message"
    
    git add .
    git commit -m "$final_message"
    git push origin "$(git branch --show-current)"
    
    print_success "提交完成！"
}

# 创建发布版本
create_release() {
    local version="$1"
    if [[ -z "$version" ]]; then
        print_error "请提供版本号"
        echo "用法: $0 release <version>"
        echo "示例: $0 release v3.24.1-personal.1"
        exit 1
    fi
    
    # 确保版本号格式正确
    if [[ ! "$version" =~ ^v[0-9]+\.[0-9]+\.[0-9]+-personal\.[0-9]+$ ]]; then
        print_warning "建议使用格式: vX.Y.Z-personal.N"
    fi
    
    # 切换到personal-dev分支
    git checkout personal-dev
    git pull origin personal-dev
    
    # 创建发布分支
    local release_branch="release/$version"
    print_info "创建发布分支: $release_branch"
    git checkout -b "$release_branch"
    
    # 更新版本信息（如果有版本文件）
    if [[ -f "app/build.gradle" ]]; then
        print_info "更新版本信息..."
        # 这里可以添加版本号更新逻辑
    fi
    
    # 推送发布分支
    git push -u origin "$release_branch"
    
    # 创建标签
    print_info "创建版本标签: $version"
    git tag -a "$version" -m "Release $version"
    git push origin "$version"
    
    print_success "发布版本 $version 创建完成！"
    print_info "发布分支: $release_branch"
    print_info "版本标签: $version"
}

# 显示状态
show_status() {
    print_info "=== Legado个人Fork版本状态 ==="
    echo ""
    
    print_info "当前分支: $(git branch --show-current)"
    print_info "远程仓库:"
    git remote -v
    echo ""
    
    print_info "最近5次提交:"
    git log --oneline -5
    echo ""
    
    print_info "本地分支:"
    git branch
    echo ""
    
    if git diff-index --quiet HEAD --; then
        print_success "工作区干净"
    else
        print_warning "工作区有未提交的更改:"
        git status --porcelain
    fi
    
    echo ""
    print_info "最新标签:"
    git tag -l | tail -5
}

# 清理已合并的分支
clean_branches() {
    print_info "清理已合并的分支..."
    
    # 切换到personal-dev分支
    git checkout personal-dev
    
    # 获取已合并的分支
    local merged_branches=$(git branch --merged | grep -v "\*\|master\|personal-dev" | xargs -n 1)
    
    if [[ -z "$merged_branches" ]]; then
        print_info "没有需要清理的分支"
        return
    fi
    
    print_info "以下分支已合并，将被删除:"
    echo "$merged_branches"
    
    read -p "确认删除这些分支吗？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "$merged_branches" | xargs -n 1 git branch -d
        print_success "分支清理完成！"
    else
        print_info "取消清理操作"
    fi
}

# 主函数
main() {
    # 检查是否在git仓库中
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "当前目录不是Git仓库"
        exit 1
    fi
    
    local command="$1"
    shift
    
    case "$command" in
        "init")
            init_dev_env
            ;;
        "feature")
            create_feature_branch "$1"
            ;;
        "finish")
            finish_feature "$1"
            ;;
        "commit")
            smart_commit "$1"
            ;;
        "release")
            create_release "$1"
            ;;
        "status")
            show_status
            ;;
        "clean")
            clean_branches
            ;;
        "help"|"-h"|"--help"|"")
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"