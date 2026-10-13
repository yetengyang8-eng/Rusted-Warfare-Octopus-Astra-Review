# Rusted Warfare Octopus — Astra 独立工程审查资料库

**2026-10-13 · 公开审查仓库 · 八套版本独立冻结 · 原始实验数据与真实对局证据**

**云端 Astra（无法访问用户本地）的最新 R12/R16 陆军任务，请直接从 [云端专用任务书](cloud_review/ASTRA_R12_R16_CLOUD_PROMPT.md) 开始，并使用 [仓库内运行入口](cloud_review/README.md)。** 其他项目级资料仍见 [总审查说明](review/ASTRA_START_HERE.md)、[回归问题](review/REGRESSION_QUESTIONS.md) 和 [全部证据清单](review/FULL_EVIDENCE_INDEX.md)。 本仓库不是 R16 的成功公告，而是让真实效果、失败记录与代码差异可以复核。

## 版本和可核查成绩

| 版本 | 冻结源提交 | 目前证据地位 |
|---|---|---|
| [R9](versions/R9/) | `56a0598` | 较早的攻城、生产与空军多编队参照；不要用单次测试声称稳定胜率。 |
| [R10E](versions/R10E/) | `d3be4f6` | 有较易 difficulty -2 的原生自主胜利；较难 Spain 仍有未结束对局。 |
| [R11B](versions/R11B/) | `f4d81e8` | 移动连续性、Builder 避免卡死的工程参照，未证明稳定总体更强。 |
| [R12](versions/R12/) | `eef352f` | Builder 与经济工程基线；Hard Spain 自然运行没有证明清图。 |
| [R13B](versions/R13B/) | `56d5b7f` | 分兵与任务连续性候选，原生自然测试仍 PARTIAL。 |
| [R14](versions/R14/) | `6637cdd` | 清剿责任过渡版本；**胜局有真人干预，不计为自主胜利**。 |
| [R15](versions/R15/) | `1311f2a` | **目前最有说服力的自主胜利参照**：普通 Spain、player5、5倍速，单次原生胜利检测 Game42:43.664。 |
| [R16](versions/R16/) | `0ada07a` | **最新、用户反馈问题严重的候选**。同参数单次自然对照 R15 在约45:32获胜、R16 在60:14仍ONGOING；工程回归139/139通过。 |

**不能过度推断**：R15只有一次单局原生成功验证；R16的单次对照也没有固定随机种子，不能据此量化稳定胜率或确认唯一原因。前后局时长不同，命令数与兵力存量不能机械直接比较。

## 完整材料布局

- `versions/<version>/source/`：从精确原始提交完整导出的**八套互不覆盖源码、测试、工具、工程交接与知识库**。
- `versions/<version>/kit-owned-files/`：对应 Agent JAR、原启动器与实际运行工具，含 KIT_MANIFEST。
- `versions/<version>/VERSION.json`：冻结提交、二进制 SHA-256、Windows 本地原始 kit 路径与端口（R9 到 R16 每套端口不同）。
- `review/diffs/`：源版本差异汇总及完整补丁，尤其 R15→R16；中间版本可直接对照独立源码。
- `evidence/native-raw/`：版本 Kit 的**完整相关原生 run 文本与压缩战斗日志**，包括未完成的 `.partial` 文件。
- `evidence/validation-archives/`：八阶段原生验证的测试、日志、失败结果与 trace 压缩归档；已过滤重复商业资源、编译缓存等。
- `evidence/human-1v9-original/`：真人西班牙1v9的原始回放、地图、原始实战遥测、逐单位 GUI 回放采样与分析代码。
- `evidence/octopus-*/`：源码仓库已有的可读验证摘要、自然运行核查及交接证据。
- `review/FULL_EVIDENCE_INDEX.md`：文件级完整清单与大小。不要把原生命令 receipt 当作实际击杀、抵达或胜利。
- `scripts/restore_local_kits.ps1`：在有合法完整游戏依赖的原 Windows 电脑上**复制并校验**八套独立运行环境；不改原文件。

## 大型资料下载

**Git LFS 已上传原始数据**：`.gz`、`.zip`、`.replay`、`.partial` 文件在 Git 仓库中对应 LFS pointer。取得完整字节时，请在本地运行：

```powershell
git clone https://github.com/yetengyang8-eng/Rusted-Warfare-Octopus-Astra-Review.git
cd Rusted-Warfare-Octopus-Astra-Review
git lfs pull
```

如果只需审查核心问题，可以直接看普通 Git 中的源码、回归问题和 R15/R16 原生测试摘要，而不必先下载全部大型证据。

**不重新分发**未经许可的商业游戏文件、JVM、第三方字体和账号密钥。本机完整 Kit 保持独立，恢复脚本仅在合法本地游戏依赖存在时使用。详见 [文件边界](review/ARTIFACT_POLICY.md)。

## 审查优先级

首先研究 R15→R16 的基地接近与 Builder 区域保留改动，分析它们如何间接导致 Air 清岛效率下降；检查内外 General 分兵、领地任务所有权、合法重获视野、Builder/Scout 连续性、真实对空威胁、经济产能与终局清图。**代码未改，不等于运行行为未被其他模块改变。**

原始开发历史仓库仍为 `yetengyang8-eng/Rusted-Warfare-Agent`。本审查仓库完全独立，不重写原提交、没有合并用户本地未提交的工具改动。
