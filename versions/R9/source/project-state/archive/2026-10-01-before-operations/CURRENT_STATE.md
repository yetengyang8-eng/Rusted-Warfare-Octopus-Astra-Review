# 当前状态

当前基线：**RW-BASELINE-2026-09-30-GS-v1**，建立于 2026-09-30（Asia/Shanghai）。

## 唯一入口

先读 [BASELINE.md](BASELINE.md) 和 [baseline-manifest.json](baseline-manifest.json)，再读 [NEXT_STAGE_PLAN.md](NEXT_STAGE_PLAN.md)。本文件只记录基线建立后的增量状态，不再叠加多个“当前版本”。

## 最新增量：专属单位生命周期

- 候选 ID：`RW-CANDIDATE-2026-09-30-SPECIALIST-LIFECYCLE-v1`。实现提交 `db0f6eec006a4f61f85134c38adc5c0f88a99e8a`，从上一轮最新提交 `ea614b5fdf729a0a09b3b324252555a31d565ac5` 继续。
- 固定候选 JAR SHA256：`b3e172de9ba54e9ebfd8da1f6787c72ad9f9f7a8702e42cdc63cc948bc57e71e`；contentDigest：`bea182a367f327d69000a3053a259a1e2f1288c58ab7737e0f0a25c978541f69`。
- 已实现：工程师退出普通经济任务，采购/任务/响应者明确关联；到点后失联进入最多 15 游戏秒调查，耗尽保留 UNKNOWN，返回后释放旧任务。新合法接触可重启；已在基地或原生仍在返回的单位不重复下令，远处中断仍恢复。普通 builder 经济职责、资金预留、明确移动缺口的两栖喷气机施工及成品接手保留。
- 已核对最新两份参考原文和三场 Capability Funding 桌面 raw。第二局为 14 名工程师观察、12 名损失，并非 14 名死亡；大量工程师被普通探矿/施工挪用。到点失联后重复原坐标命令与已在家重复返回均有逐行证据，见 [输入分析](../evidence/specialist-lifecycle-2026-09-30/INPUT_ANALYSIS.md)。
- 旧候选在新增四个真实 HTTP 场景中全部预期失败；最终新候选四场全部通过，原有七场资金测试继续通过。策略契约为 166 项，包含失联后处理新需求、同目标新证据重启、采购成品等待与三次损失上限。
- 完整 Windows 回归：42/42 步通过，21 Java runs、18 Python suites、292 Python tests、0 Python skipped、failed steps=0。POSIX 专用 Java 模拟按平台跳过，Windows 文件锁真实执行；88 个非 manifest JAR 归档项与固定交付完全一致。Linux 未重新运行。
- 独立原生局：Big Island / difficulty=1 / 请求5x / poll500ms / 1200.752 Battle 游戏秒，预算到期 PARTIAL/ONGOING。两笔采购、两名工程师 #925/#1105、四次响应/调查/到期/返家释放；两者均接手过第二个需求，工程师损失0、普通经济挪用0、已到家重复返回0。调查识别前有一次刚到点重发，详细边界保留在证据中；调查期间和超出明确失联观察宽限后的重发0。
- 合法解决数0，末尾四项需求仍 UNKNOWN。本局没有自然触发调查中重新发现目标、支援喷气机接手；这些只有自动测试覆盖。不把单局存活、预算到期或流程通过当成胜率收益。
- 固定交付见 [候选目录](../deliveries/specialist-lifecycle-2026-09-30/README.md)；[完整证据](../evidence/specialist-lifecycle-2026-09-30/README.md)、[身份清单](../evidence/specialist-lifecycle-2026-09-30/candidate-manifest.json)、[验证摘要](../evidence/specialist-lifecycle-2026-09-30/validation-summary.json) 已保存旧/新对照、原始报告、运行设置与清理记录。
- 本候选未晋级基线、未替换桌面安装。后续任务从本仓库最新提交及本文件继续，不因 v1 校验器报告增量源码差异而重置。

## 已完成的上一项：有界资金预留

`RW-CANDIDATE-2026-09-30-CAP-FUNDING-v1` 的实现提交为 `fd8572849388d2e7fe04a5d446154602b54d3118`，JAR 为 `41c52392d0d9cc7af39b4e726d6bab2537249ca146b5621d890fd2a8851a2141`。按真实报价预留单项工程师资金，预计缺口不超过60游戏秒、90秒到期、失败冷却60秒，已纳入当前候选。其历史回归和自然局保留在 [原交付证据](../evidence/capability-funding-2026-09-30/README.md)。

最新只读核对确认它已经运行于 `游戏环境/P1F-Astra-CapabilityFunding-2026-09-30`，三场桌面 raw 均自证该 SHA。本文件覆盖上一轮“未部署”的现状措辞，原交付文档保留当时记录；本轮没有执行该安装。三局实际地图路径、难度、人工 bootstrap 未记录，不能补猜。

## 已固定的起点

- 默认源码：本仓库 agent/、tools/；生产代码及既有证据起点为 6214f073d1b76429c0b827268db918e1e90e9f5f。
- 最新功能：Global Strategy / Feasibility / Combat Engineer。
- 不可变交付 JAR：SHA256 76711a8ee2716af5d5b0b66d5c53b159ae28f4f744dcbb24cae492d96f0342ed。
- 基线桌面环境：工作区 游戏环境/P1F-Astra-GlobalStrategy-2026-09-30；更新的 Capability Funding 桌面身份见上节，两者分开保存。
- 旧 P1F-冒烟环境 与旧 developer/dist 仍为 5741e241…，保留历史身份，不能据此判断最新候选未部署。
- 基线建立轮只整理和验证；其后的产品增量见上节，不回写旧基线结果。

## 已验证

- 最终交付件已有 Linux 完整回归、原生 E2 和单局无画面 VICTORY。
- 同一最终交付件已有三场 Windows 桌面长局：约 2400 游戏秒、5x，三场均 PARTIAL/ONGOING，专项审计均无违规。
- Big Island / Spain A / Spain B 峰值移动武装单位为 88 / 21 / 101。
- Spain 两局均在 Match 前人工生产一个 builder，之后自主运行；不是原生出生零干预启动。
- 本轮进一步核对 raw-evidence 内 76/76 文件、桌面 SHA 清单 11/11 文件、桌面压缩包内 9/9 原始报告。
- Windows 隔离验证为 PASS_COMBINED_MATRIX：首轮 39/40，补齐遗漏的同提交测试数据后受影响套件 16/16；最终 40 步、281 Python 全通过，Windows 文件锁项已覆盖。87 个非 manifest 条目与最终交付完全一致。平台跳过与原始失败保留在 [基线验收记录](../evidence/baseline-2026-09-30/VALIDATION.md)。

## 未验证与待解决

- Spain A/B 方差没有根因结论；两局不构成控制全部条件的 A/B。
- 经济积压、响应者生存/成组执行、终局清理仍未解决。
- 缺地图路径、难度等完整运行上下文；无 builder 原生开局尚不支持。
- 没有地图级胜率提升或因果收益证明；修理/回收、完整海军、同局双玩家仍在后续范围。
- 原始 Astra source commit 2f310ec 未进入本地对象库。已有交付 overlay/patch 对照，不能声称完整 Git ancestry 已验证。

## 下一项工作

优先处理有证据的响应者护送：沿最新旧桌面 Run 2 的四次低血中断，核定受损位置、合法威胁和附近可用友军，设计最小护卫集合、等待上限、任务归属及撤回条件，再用同条件自然样本验证。保持主力最低可用力量，不按工程师价格推算战力。通用敌群/多编队、修理和无 builder 起步保持独立待办；若实际使用被无 builder 阻塞，再提前该项。当前任务已完成的生命周期增量不回退。完整计划见 NEXT_STAGE_PLAN。

## 历史记录

整理前全部状态见 [archive/2026-09-30-pre-baseline](archive/2026-09-30-pre-baseline/README.md)。旧交付材料中的“未部署桌面”是当时状态，保留原始证据不改写；最新桌面证据覆盖其现状措辞。
