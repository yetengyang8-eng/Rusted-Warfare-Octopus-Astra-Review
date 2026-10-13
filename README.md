# Rusted Warfare Octopus — Astra 工程审查资料库

**私有审查仓库 · 2026-10-13 · R9 / R12 / R15 / R16 四套冻结独立版本**

> **先读 [review/ASTRA_START_HERE.md](review/ASTRA_START_HERE.md)。** 本仓库不是“R16 已经更好”的发布公告，而是将成功与失败证据并列，供 Astra 找到真正的回归原因。

## 版本与证据

| 冻结版本 | 代码快照 | 现有可靠结论 | 状态 |
|---|---|---|---|
| [R9](versions/R9/) | `56a0598` | 早期多角色/生产能力较好的阶段，部分 Spain 原生报告仍 ONGOING | 历史参照，不是证实胜率 |
| [R12](versions/R12/) | `eef352f` | Builder、资金链相关原生证据；Hard Spain 未清图，仍 ONGOING | Builder 工程基线 |
| [R15](versions/R15/) | `1311f2a` | 普通 Spain / player5 / 5×，**一次自主原生 VICTORY，约42:43.664** | 当前最有力的自主胜利参照 |
| [R16](versions/R16/) | `0ada07a` | 同条件一次自然对照：R15 ~45:32胜利；R16 ~60:14仍ONGOING。139/139 工程矩阵通过 | **最新有问题的候选，禁止默认替换R15** |

**重要**：R15 的一次胜利不能代表稳定胜率；R16 的一次失败对照也不足以独立确认因果根因。用户明确反馈“最新版本问题很大”，但未附新的具体人工测试证据，作为待补证的用户观察单列。

## 资料组织

- `versions/R9|R12|R15|R16/source/`：分别从精确 Git commit 导出的完整 `agent`、工具、工程文档、知识与交接资料，**互不覆盖**。
- `versions/*/kit-owned-files/`：各版本冻结的 Agent 成品 JAR、启动脚本、运行工具与完整 KIT_MANIFEST，**不含商业游戏本体**。
- `versions/*/VERSION.json`：来源 commit、二进制 SHA256、对应本地完整环境及端口。
- `evidence/`：四个版本的原生验证摘要、矩阵、失败与对照记录，以及本地验证文件索引。
- `review/diffs/`：R9→R12、R12→R15、**R15→R16** 的逐文件源码补丁和变更统计。
- `review/human_reference/`：真人西班牙1v9实验战术/遥测分析；**不是同难度或同地图受控 Agent 对照**。
- `scripts/restore_local_kits.ps1`：可选；在拥有本机独立合法游戏 Kit 的 Windows 上恢复完整运行副本。GitHub 不包含 JVM/商业游戏资源/超大 raw 日志。

## Astra 应当做什么

1. 阅读 [审查任务书](review/ASTRA_START_HERE.md) 和 [问题登记](review/REGRESSION_QUESTIONS.md)，以 R15 为证据基线，先区分 **代码没改** 与 **实战行为没有受其他模块影响**。
2. 针对 R15→R16 `GeneralCombatDirector` 局部攻城准入、Builder 保留远端区域职责改动，检查如何通过任务顺序、战线节奏、预算与资源间接影响 Air 清岛。
3. 根据真实失败样本提出最小可验证机制假说，设计 native 对照，**不靠固定分队数量、盲目提高全局操作频率或扩大防御阈值**。
4. 把证据等级、复现入口、已确认/待验证结论区分记录，不把日志 receipt 当作实际抵达和击杀。

原项目历史仓库：`yetengyang8-eng/Rusted-Warfare-Agent`。本仓库为**独立审查副本**，不替代、不改写原工程仓库及当前工作区未提交改动。

## Complete raw evidence (Git LFS)

- [Complete artifact index](review/FULL_EVIDENCE_INDEX.md): raw run traces, native validation archives, original human replay/map and unit snapshots.
- After cloning, run git lfs pull to obtain large files.
