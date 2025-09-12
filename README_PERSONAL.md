# Legado 个人优化版

> 基于 [gedoor/legado](https://github.com/gedoor/legado) 的个人优化版本

## 📖 关于此版本

这是一个基于官方Legado的个人fork版本，主要目的是：
- 修复一些个人使用中遇到的问题
- 添加一些个人需要的小功能
- 保持与上游版本的同步更新

## ✨ 个人优化功能

### VoIP通话暂停功能
- **问题**: 朗读中接听微信语音通话时，朗读无法自动暂停
- **解决**: 新增音频模式监听器，能够检测VoIP通话状态并自动暂停/恢复朗读
- **支持**: 微信语音通话、QQ语音通话等VoIP应用

## 🚀 快速开始

### 下载安装
1. 前往 [Releases](../../releases) 页面下载最新版本APK
2. 安装APK文件
3. 可以直接从官方版本升级，数据会保留

### 从源码编译
```bash
git clone https://github.com/[您的用户名]/legado.git
cd legado
# 切换到个人开发分支
git checkout personal-dev
# 使用Android Studio打开项目编译
```

## 📋 版本说明

- **版本格式**: `vX.Y.Z-personal.N`
- **基础版本**: 跟随上游官方版本
- **个人版本**: 在基础版本上的个人修改版本号

### 当前版本
- **基础版本**: v3.24.1 (gedoor/legado)
- **个人版本**: v3.24.1-personal.1
- **发布日期**: 2025-09-12

## 🔄 更新策略

### 上游同步
- 定期同步上游更新（通常每周检查）
- 自动化同步流程，减少手动操作
- 保持与官方版本的兼容性

### 个人修改
- 所有个人修改都在独立分支进行
- 详细记录修改内容和原因
- 尽量保持模块化，便于维护

## 📚 文档

- [Fork维护指南](FORK_MAINTENANCE_GUIDE.md) - 如何维护个人fork版本
- [个人修改记录](PERSONAL_CHANGES.md) - 详细的修改记录和技术说明
- [开发脚本使用](scripts/) - 自动化脚本的使用说明

## 🛠️ 开发工具

### 自动化脚本
```bash
# 同步上游更新
./scripts/sync_upstream.sh

# 开发工作流
./scripts/dev_workflow.sh help
```

### GitHub Actions
- 自动检测上游更新
- 自动创建同步PR
- 自动化测试和构建

## 🤝 贡献

### 向上游贡献
如果个人修改对社区有价值，会考虑向上游提交PR。

### 问题反馈
- 个人版本特有问题：请在此仓库提交Issue
- 通用问题：建议在上游仓库反馈

## 📄 许可证

本项目遵循与上游项目相同的许可证。

## 🙏 致谢

- 感谢 [gedoor](https://github.com/gedoor) 开发的优秀开源项目
- 感谢所有为Legado项目贡献的开发者们

## ⚠️ 免责声明

- 此版本为个人使用和学习目的
- 不保证稳定性，请谨慎用于生产环境
- 如遇问题，请优先尝试官方版本

---

## 📞 联系方式

- GitHub Issues: [提交问题](../../issues)
- 上游项目: [gedoor/legado](https://github.com/gedoor/legado)

---

*最后更新: 2025-09-12*