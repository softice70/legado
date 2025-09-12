#!/bin/bash

# Legado个人Fork版本 - 快速设置脚本
# 用法: ./setup_personal_fork.sh

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
    echo "    Legado个人Fork版本 - 快速设置脚本"
    echo "=================================================="
    echo -e "${NC}"
}

# 检查必要工具
check_requirements() {
    print_info "检查必要工具..."
    
    if ! command -v git &> /dev/null; then
        print_error "Git未安装，请先安装Git"
        exit 1
    fi
    
    print_success "Git已安装: $(git --version)"
}

# 检查Git配置
check_git_config() {
    print_info "检查Git配置..."
    
    local git_name=$(git config --global user.name 2>/dev/null || echo "")
    local git_email=$(git config --global user.email 2>/dev/null || echo "")
    
    if [[ -z "$git_name" ]] || [[ -z "$git_email" ]]; then
        print_warning "Git用户信息未配置"
        read -p "请输入您的Git用户名: " git_name
        read -p "请输入您的Git邮箱: " git_email
        
        git config --global user.name "$git_name"
        git config --global user.email "$git_email"
        print_success "Git用户信息已配置"
    else
        print_success "Git用户信息: $git_name <$git_email>"
    fi
}

# 设置远程仓库
setup_remotes() {
    print_info "设置远程仓库..."
    
    # 检查origin远程仓库
    if ! git remote get-url origin &> /dev/null; then
        print_error "未找到origin远程仓库，请确保在正确的Git仓库中运行此脚本"
        exit 1
    fi
    
    local origin_url=$(git remote get-url origin)
    print_info "Origin仓库: $origin_url"
    
    # 添加upstream远程仓库
    if ! git remote get-url upstream &> /dev/null; then
        print_info "添加upstream远程仓库..."
        git remote add upstream https://github.com/gedoor/legado.git
        print_success "已添加upstream远程仓库"
    else
        print_success "Upstream远程仓库已存在"
    fi
    
    # 获取远程仓库信息
    print_info "获取远程仓库信息..."
    git fetch origin
    git fetch upstream
}

# 创建个人开发分支
setup_branches() {
    print_info "设置分支结构..."
    
    # 确保在master分支
    git checkout master 2>/dev/null || git checkout main 2>/dev/null || {
        print_error "未找到master或main分支"
        exit 1
    }
    
    # 同步上游master
    print_info "同步上游master分支..."
    git merge upstream/master --no-edit
    git push origin master
    
    # 创建personal-dev分支
    if ! git show-ref --verify --quiet refs/heads/personal-dev; then
        print_info "创建personal-dev分支..."
        git checkout -b personal-dev
        git push -u origin personal-dev
        print_success "personal-dev分支已创建"
    else
        print_info "personal-dev分支已存在，切换到该分支..."
        git checkout personal-dev
        git pull origin personal-dev
    fi
}

# 设置脚本权限
setup_scripts() {
    print_info "设置脚本权限..."
    
    if [[ -f "scripts/sync_upstream.sh" ]]; then
        chmod +x scripts/sync_upstream.sh
        print_success "sync_upstream.sh 权限已设置"
    fi
    
    if [[ -f "scripts/dev_workflow.sh" ]]; then
        chmod +x scripts/dev_workflow.sh
        print_success "dev_workflow.sh 权限已设置"
    fi
    
    chmod +x setup_personal_fork.sh
    print_success "setup_personal_fork.sh 权限已设置"
}

# 创建个人配置文件
create_personal_config() {
    print_info "创建个人配置文件..."
    
    # 创建.gitignore.personal文件（如果不存在）
    if [[ ! -f ".gitignore.personal" ]]; then
        cat > .gitignore.personal << 'EOF'
# 个人配置文件
.personal/
*.personal
personal_*.json
personal_*.xml

# IDE个人设置
.vscode/settings.json
.idea/workspace.xml

# 本地测试文件
test_personal/
EOF
        print_success "已创建 .gitignore.personal"
    fi
    
    # 创建个人配置目录
    mkdir -p .personal
    
    # 创建个人配置文件
    if [[ ! -f ".personal/config.json" ]]; then
        cat > .personal/config.json << 'EOF'
{
  "version": "v3.24.1-personal.1",
  "maintainer": "Your Name",
  "email": "your.email@example.com",
  "features": {
    "voip_pause": true,
    "enhanced_audio_focus": true
  },
  "build": {
    "version_suffix": "personal",
    "build_number_offset": 1000
  }
}
EOF
        print_success "已创建个人配置文件"
    fi
}

# 显示使用指南
show_usage_guide() {
    print_info "设置完成！以下是常用命令："
    echo ""
    echo -e "${GREEN}日常开发:${NC}"
    echo "  ./scripts/dev_workflow.sh status          # 查看当前状态"
    echo "  ./scripts/dev_workflow.sh feature <name>  # 创建功能分支"
    echo "  ./scripts/dev_workflow.sh commit <msg>    # 智能提交"
    echo "  ./scripts/dev_workflow.sh finish          # 完成功能开发"
    echo ""
    echo -e "${GREEN}同步上游:${NC}"
    echo "  ./scripts/sync_upstream.sh                # 同步上游更新"
    echo "  ./scripts/sync_upstream.sh --force        # 强制同步（有未提交更改时）"
    echo ""
    echo -e "${GREEN}版本管理:${NC}"
    echo "  ./scripts/dev_workflow.sh release <ver>   # 创建发布版本"
    echo "  ./scripts/dev_workflow.sh clean           # 清理已合并分支"
    echo ""
    echo -e "${GREEN}文档:${NC}"
    echo "  cat FORK_MAINTENANCE_GUIDE.md             # 查看维护指南"
    echo "  cat PERSONAL_CHANGES.md                   # 查看修改记录"
    echo ""
}

# 主函数
main() {
    print_header
    
    # 检查是否在Git仓库中
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "当前目录不是Git仓库"
        exit 1
    fi
    
    print_info "开始设置Legado个人Fork版本开发环境..."
    echo ""
    
    check_requirements
    check_git_config
    setup_remotes
    setup_branches
    setup_scripts
    create_personal_config
    
    echo ""
    print_success "🎉 Legado个人Fork版本开发环境设置完成！"
    echo ""
    
    show_usage_guide
    
    print_info "当前分支: $(git branch --show-current)"
    print_info "现在可以开始您的个人开发了！"
}

main "$@"