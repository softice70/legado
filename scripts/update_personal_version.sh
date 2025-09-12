#!/bin/bash

# Legado个人版本更新脚本
# 用于自动化版本号更新和发布流程

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PERSONAL_VERSION_PREFIX="personal"

# 函数：显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help         显示帮助信息"
    echo "  -v, --version      指定版本号 (例如: 3.24.1)"
    echo "  -b, --build        指定构建号 (例如: 1)"
    echo "  -m, --message      版本更新说明"
    echo "  -t, --tag          创建发布标签"
    echo "  -p, --push         推送到远程仓库"
    echo ""
    echo "示例:"
    echo "  $0 -v 3.24.1 -b 2 -m \"修复VoIP功能问题\" -t -p"
}

# 函数：获取当前版本信息
get_current_version() {
    local version_file="$PROJECT_ROOT/PERSONAL_CHANGES.md"
    if [[ -f "$version_file" ]]; then
        local current_version=$(grep -oP '个人版本: v\K[0-9]+\.[0-9]+\.[0-9]+-personal\.[0-9]+' "$version_file" | head -1)
        echo "$current_version"
    else
        echo "3.24.1-personal.1"
    fi
}

# 函数：更新版本文件
update_version_files() {
    local new_version="$1"
    local build_num="$2"
    local message="$3"
    
    echo -e "${YELLOW}正在更新版本文件...${NC}"
    
    # 更新PERSONAL_CHANGES.md
    local changes_file="$PROJECT_ROOT/PERSONAL_CHANGES.md"
    if [[ -f "$changes_file" ]]; then
        # 更新版本信息
        sed -i "s/个人版本: v[0-9]\+\.[0-9]\+\.[0-9]\+-personal\.[0-9]\+/个人版本: v${new_version}/" "$changes_file"
        sed -i "s/最后更新: [0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}/最后更新: $(date +%Y-%m-%d)/" "$changes_file"
        
        # 添加更新记录
        local update_entry="### v${new_version} ($(date +%Y-%m-%d))\n- ${message}\n"
        sed -i "/## 更新日志/a\${update_entry}" "$changes_file"
    fi
    
    # 更新build.gradle中的版本号
    local build_gradle="$PROJECT_ROOT/app/build.gradle"
    if [[ -f "$build_gradle" ]]; then
        sed -i "s/versionName \"[0-9]\+\.[0-9]\+\.[0-9]\+\+personal[0-9]\+\"/versionName \"${new_version}\"/" "$build_gradle"
    fi
    
    echo -e "${GREEN}版本文件更新完成${NC}"
}

# 函数：创建发布标签
create_release_tag() {
    local version="$1"
    local message="$2"
    
    echo -e "${YELLOW}正在创建发布标签...${NC}"
    
    local tag_name="personal-v${version}"
    git tag -a "$tag_name" -m "Release ${tag_name}: ${message}"
    
    echo -e "${GREEN}发布标签创建完成: ${tag_name}${NC}"
}

# 函数：推送到远程
push_to_remote() {
    echo -e "${YELLOW}正在推送到远程仓库...${NC}"
    
    git push origin personal-dev
    git push origin --tags
    
    echo -e "${GREEN}推送完成${NC}"
}

# 函数：验证环境
validate_environment() {
    # 检查是否在personal-dev分支
    local current_branch=$(git rev-parse --abbrev-ref HEAD)
    if [[ "$current_branch" != "personal-dev" ]]; then
        echo -e "${RED}错误: 必须在personal-dev分支上运行此脚本${NC}"
        exit 1
    fi
    
    # 检查工作区是否干净
    if [[ -n $(git status --porcelain) ]]; then
        echo -e "${RED}错误: 工作区有未提交的更改，请先提交或暂存${NC}"
        git status --short
        exit 1
    fi
    
    # 检查上游同步
    git fetch origin
    local local_commit=$(git rev-parse personal-dev)
    local remote_commit=$(git rev-parse origin/personal-dev)
    
    if [[ "$local_commit" != "$remote_commit" ]]; then
        echo -e "${YELLOW}警告: 本地分支与远程分支不同步${NC}"
        echo "本地: $local_commit"
        echo "远程: $remote_commit"
        read -p "是否继续? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 主函数
main() {
    local version=""
    local build_num=""
    local message=""
    local create_tag=false
    local push_remote=false
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--version)
                version="$2"
                shift 2
                ;;
            -b|--build)
                build_num="$2"
                shift 2
                ;;
            -m|--message)
                message="$2"
                shift 2
                ;;
            -t|--tag)
                create_tag=true
                shift
                ;;
            -p|--push)
                push_remote=true
                shift
                ;;
            *)
                echo -e "${RED}未知参数: $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 验证环境
    validate_environment
    
    # 获取当前版本信息
    local current_version=$(get_current_version)
    echo -e "${GREEN}当前版本: ${current_version}${NC}"
    
    # 如果未指定版本，自动递增
    if [[ -z "$version" ]]; then
        local base_version=$(echo "$current_version" | cut -d'-' -f1)
        version="$base_version"
    fi
    
    # 如果未指定构建号，自动递增
    if [[ -z "$build_num" ]]; then
        local current_build=$(echo "$current_version" | grep -oP 'personal\.\K[0-9]+')
        build_num=$((current_build + 1))
    fi
    
    # 如果未指定消息，提示输入
    if [[ -z "$message" ]]; then
        read -p "请输入版本更新说明: " message
        if [[ -z "$message" ]]; then
            echo -e "${RED}错误: 必须提供版本更新说明${NC}"
            exit 1
        fi
    fi
    
    local new_version="${version}-personal.${build_num}"
    echo -e "${GREEN}新版本: ${new_version}${NC}"
    
    # 确认操作
    read -p "确认更新到 ${new_version}? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}操作已取消${NC}"
        exit 0
    fi
    
    # 执行更新
    update_version_files "$version" "$build_num" "$message"
    
    if [[ "$create_tag" == true ]]; then
        create_release_tag "$new_version" "$message"
    fi
    
    if [[ "$push_remote" == true ]]; then
        push_to_remote
    fi
    
    echo -e "${GREEN}版本更新完成!${NC}"
    echo "新版本: ${new_version}"
    echo "更新说明: ${message}"
}

# 执行主函数
main "$@"