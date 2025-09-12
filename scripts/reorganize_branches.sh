#!/bin/bash

# Legado个人Fork版本 - 分支重新整理脚本
# 用法: ./reorganize_branches.sh

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

print_header() {
    echo -e "${BLUE}"
    echo "=================================================="
    echo "    Legado分支重新整理脚本"
    echo "=================================================="
    echo -e "${NC}"
}

# 检查Git仓库状态
check_git_status() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "当前目录不是Git仓库"
        exit 1
    fi
    
    local current_branch=$(git branch --show-current)
    if [[ "$current_branch" != "master" ]] && [[ "$current_branch" != "main" ]]; then
        print_error "请在master或main分支上运行此脚本"
        print_info "当前分支: $current_branch"
        exit 1
    fi
    
    print_info "当前分支: $current_branch"
}

# 检查是否有未提交的更改
check_uncommitted_changes() {
    if ! git diff-index --quiet HEAD --; then
        print_error "检测到未提交的更改，请先提交或暂存"
        git status --porcelain
        exit 1
    fi
    print_success "工作区干净"
}

# 显示当前提交历史
show_commit_history() {
    print_info "当前提交历史（最近10个提交）:"
    echo ""
    git log --oneline --graph -10
    echo ""
}

# 获取上游信息
setup_upstream() {
    if ! git remote get-url upstream &> /dev/null; then
        print_info "添加upstream远程仓库..."
        git remote add upstream https://github.com/gedoor/legado.git
    fi
    
    print_info "获取上游信息..."
    git fetch upstream
}

# 识别个人提交
identify_personal_commits() {
    print_info "分析提交历史..."
    
    # 获取与上游的差异提交
    local personal_commits=$(git log --oneline master ^upstream/master 2>/dev/null || git log --oneline master ^upstream/main 2>/dev/null || echo "")
    
    if [[ -z "$personal_commits" ]]; then
        print_warning "没有发现个人提交，可能已经与上游同步"
        read -p "是否继续创建personal-dev分支？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "操作取消"
            exit 0
        fi
        return 0
    fi
    
    print_info "发现以下个人提交:"
    echo "$personal_commits"
    echo ""
    
    # 计算提交数量
    local commit_count=$(echo "$personal_commits" | wc -l)
    print_info "共 $commit_count 个个人提交"
    
    return $commit_count
}

# 创建备份
create_backup() {
    local backup_name="backup-before-reorganization-$(date +%Y%m%d-%H%M%S)"
    print_info "创建备份分支: $backup_name"
    git branch "$backup_name"
    git tag "$backup_name"
    print_success "备份已创建: $backup_name"
    echo "$backup_name" > .git/last_backup
}

# 执行分支重新整理
reorganize_branches() {
    local commit_count=$1
    
    if [[ $commit_count -eq 0 ]]; then
        # 没有个人提交，直接创建personal-dev分支
        print_info "创建personal-dev分支..."
        git checkout -b personal-dev
        git push -u origin personal-dev
        
        # 同步master到上游
        git checkout master
        git merge upstream/master --no-edit
        git push origin master
        
        print_success "分支创建完成"
        return
    fi
    
    print_info "开始重新整理分支..."
    
    # 获取个人提交的hash列表
    local commits=($(git log --format="%H" master ^upstream/master 2>/dev/null || git log --format="%H" master ^upstream/main 2>/dev/null))
    
    # 反转数组，使最早的提交在前
    local reversed_commits=()
    for ((i=${#commits[@]}-1; i>=0; i--)); do
        reversed_commits+=("${commits[i]}")
    done
    
    # 找到fork点（最后一个上游提交）
    local fork_point=$(git merge-base master upstream/master 2>/dev/null || git merge-base master upstream/main 2>/dev/null)
    print_info "Fork点: $fork_point"
    
    # 重置master到fork点
    print_info "重置master分支到fork点..."
    git reset --hard "$fork_point"
    
    # 创建personal-dev分支
    print_info "创建personal-dev分支..."
    git checkout -b personal-dev
    
    # Cherry-pick个人提交
    print_info "应用个人提交到personal-dev分支..."
    for commit in "${reversed_commits[@]}"; do
        local commit_msg=$(git log --format="%s" -n 1 "$commit")
        print_info "应用提交: $commit ($commit_msg)"
        if ! git cherry-pick "$commit"; then
            print_error "Cherry-pick失败，请手动解决冲突"
            print_info "解决冲突后运行: git cherry-pick --continue"
            print_info "或者取消: git cherry-pick --abort"
            exit 1
        fi
    done
    
    # 推送personal-dev分支
    print_info "推送personal-dev分支到远程..."
    git push -u origin personal-dev
    
    # 更新master分支
    print_info "更新master分支到最新上游..."
    git checkout master
    git merge upstream/master --no-edit 2>/dev/null || git merge upstream/main --no-edit 2>/dev/null
    
    # 推送master分支（可能需要force）
    if git push origin master 2>/dev/null; then
        print_success "master分支已更新"
    else
        print_warning "master分支推送失败，可能需要强制推送"
        read -p "是否强制推送master分支？这会覆盖远程的master分支 (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git push origin master --force-with-lease
            print_success "master分支已强制更新"
        else
            print_warning "master分支未推送，请手动处理"
        fi
    fi
}

# 验证结果
verify_result() {
    print_info "验证重新整理结果..."
    
    # 检查分支存在
    if git show-ref --verify --quiet refs/heads/personal-dev; then
        print_success "personal-dev分支已创建"
    else
        print_error "personal-dev分支创建失败"
        return 1
    fi
    
    # 检查personal-dev分支的提交
    git checkout personal-dev
    local personal_commits=$(git log --oneline personal-dev ^master | wc -l)
    print_info "personal-dev分支包含 $personal_commits 个个人提交"
    
    # 检查master分支状态
    git checkout master
    local behind_upstream=$(git log --oneline master..upstream/master 2>/dev/null | wc -l || echo "0")
    if [[ $behind_upstream -eq 0 ]]; then
        print_success "master分支与上游同步"
    else
        print_warning "master分支落后上游 $behind_upstream 个提交"
    fi
    
    print_success "分支重新整理完成！"
}

# 显示后续操作指南
show_next_steps() {
    print_info "后续操作指南:"
    echo ""
    echo -e "${GREEN}1. 验证分支状态:${NC}"
    echo "   git branch -a"
    echo "   git log --oneline --graph -10"
    echo ""
    echo -e "${GREEN}2. 开始个人开发:${NC}"
    echo "   git checkout personal-dev"
    echo "   # 进行您的开发工作"
    echo ""
    echo -e "${GREEN}3. 使用开发工具:${NC}"
    echo "   ./scripts/dev_workflow.sh status"
    echo "   ./scripts/sync_upstream.sh"
    echo ""
    echo -e "${GREEN}4. 如果需要回滚:${NC}"
    local backup_name=$(cat .git/last_backup 2>/dev/null || echo "backup-before-reorganization")
    echo "   git checkout $backup_name"
    echo "   git branch -D personal-dev"
    echo "   git checkout -b master"
    echo ""
}

# 主函数
main() {
    print_header
    
    print_info "此脚本将帮助您重新整理分支结构:"
    print_info "1. 创建备份"
    print_info "2. 识别个人提交"
    print_info "3. 创建personal-dev分支"
    print_info "4. 将个人提交移动到personal-dev"
    print_info "5. 同步master到上游"
    echo ""
    
    read -p "是否继续？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "操作取消"
        exit 0
    fi
    
    check_git_status
    check_uncommitted_changes
    show_commit_history
    setup_upstream
    
    identify_personal_commits
    local commit_count=$?
    
    create_backup
    reorganize_branches $commit_count
    verify_result
    show_next_steps
    
    print_success "🎉 分支重新整理完成！"
    print_info "当前分支: $(git branch --show-current)"
}

main "$@"