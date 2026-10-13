# 可再现性与文件边界

**已放进仓库的：** 四份各自源 commit 导出的完整源代码/工具/文档与测试入口（`versions/*/source`），冻结 Agent JAR 和随 Kit 的启动文件（`versions/*/kit-owned-files`），完整 KIT_MANIFEST，四版本关键证据、项目历史资料、Astra导航与版本间代码补丁、真人回放研究报告。

**未放进 GitHub 的：** 第三方商业游戏 `game-lib.jar`、游戏素材、JVM 和系统 DLL、字体、从用户机器获得的私人设置、存档/回放原文件，以及单个可达100～200MB的原生 `battle-*.jsonl.gz`。原因是版权、仓库体积、隐私与 GitHub 单文件限制。这里的“独立环境”是互不覆盖的完整源代码及 Agent 工件；若要一键运行，须用合法本地游戏依赖按 [恢复脚本](../scripts/restore_local_kits.ps1) 组装。

原始完整 Windows Kit 仍保存在各版本 `VERSION.json` 中的 `local_full_kit`；恢复脚本只复制，绝不修改源目录。强相关 R15/R16 对照原始数据位于本机：
`G:\deepseek 工作台\_validation\octopus-r16-ground-20261012\`。
R15 单独自然样本在：
`G:\deepseek 工作台\_validation\octopus-r15-air-clearance-20261012\`。

**源代码事实**取自固定 Git 树，**可执行二进制事实**用 SHA256 固定，**实际运行事实**必须引用真实原生日志/后帧；Git 仓库中的一份 Handoff 不自动等价于胜利或验证。

如 Astra 只能访问远端 GitHub，可先完成源码差异审查。需要巨型原始 battle 数据时应在用户授权的 Desktop Commander 环境访问原文件，不应假装仓库已上传全量原生日志。

## 2026-10-13 upload extension

The previously excluded raw run files, native validation traces and the human original replay are now included in the private repo using Git LFS; see [full index](FULL_EVIDENCE_INDEX.md). A prior statement about keeping these out of GitHub is superseded by this section. Only proprietary game/JVM/third-party assets and credentials are excluded, with local restoration supported.

## 2026-10-13 upload extension

The previously excluded raw run files, native validation traces and the human original replay are now included in the private repo using Git LFS; see [full index](FULL_EVIDENCE_INDEX.md). A prior statement about keeping these out of GitHub is superseded by this section. Only proprietary game/JVM/third-party assets and credentials are excluded, with local restoration supported.
