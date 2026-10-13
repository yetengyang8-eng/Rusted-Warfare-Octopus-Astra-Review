# Astra 审查入口：找回有效行为，解释 R16 的异常

## 一、审查目标

用户反馈当前最新版本问题很大。现阶段可直接证明的是：R15 的一次原生自主 Spain 胜利，与 R16 的同条件单次自然对照存在显著结果落差，而 R16 工程矩阵全部通过。**不要自行推定所有异常已归咎于 Ground 模块，也不要说“Air 类字节不变，因此 Air 表现相同”。**

请首先复核以下四个冻结源码快照：

- `versions/R9/source/agent` — 早期基线；
- `versions/R12/source/agent` — Builder/经济基线；
- `versions/R15/source/agent` — **最重要的可重复检查参照**；
- `versions/R16/source/agent` — 用户反馈严重问题的最新候选。

最重要的代码差异位于 [R15_to_R16_code.patch](diffs/R15_to_R16_code.patch)。全部源码在四个彼此独立的目录中，**千万不要在审查阶段把它们拼成一个版本**。

## 二、优先阅读的原始文档（按重要性）

1. `versions/R16/source/handoff/HANDOFF_Octopus_R16_2026-10-12.md`
2. `versions/R15/source/handoff/HANDOFF_Octopus_R15_2026-10-12.md`
3. `versions/R16/source/docs/OCTOPUS_R16_GROUND_APPROACH.md`
4. `versions/R15/source/docs/OCTOPUS_R15_AIR_CLEARANCE.md`
5. `evidence/octopus-r16-ground-2026-10-12/spain-comparison.json`
6. `evidence/octopus-r16-ground-2026-10-12/validation-summary.json`
7. `evidence/octopus-r15-air-clearance-2026-10-12/spain-normal-player5.json`
8. `versions/R12/source/handoff/HANDOFF_Octopus_R12_Builder_2026-10-11.md`
9. `versions/R9/source/handoff/HANDOFF_Octopus_R9_2026-10-05.md`
10. [真人战术参照](human_reference/)（不可当 Agent 同难度验证）。

## 三、可核查事实

- R15：普通 difficulty0，Spain，player5，5倍速，独立单次自主原生 `VICTORY`，检出样本约Game42:43.664。存在多编队冗余支援和后期30万量级资金；并非理想无缺的版本。
- R16：同参数并行对照中，R15再次约45:32胜利，而R16到约60:14仍 `ONGOING`。R16当时 ready两栖305、重坦97、矿37、陆厂4；R15胜利局对照ready两栖128、重坦108、矿44、陆厂9。样本长度不同，不把这些数值机械除以时间作比。
- R16 增加 Ground 的局部接敌准入并改变普通 Builder 召回政策；其 Air 相关14个编译class与R15逐字节相同，**不意味着宏观调度、依赖世界状态或全局胜负相同**。
- R16 完整工程矩阵139/139绿灯，但首次只有138/139；旧 G55 期望与新的 Builder 地域保护相冲突，重定用例后同JAR重跑通过。请保留这一变更历史。
- R12：Hard Spain自然局仍未赢，Builder 到矿/Prospect 问题、地图最后清剿仍有遗留，不因其叫“基线”误写成功胜率。
- R9：多角色、多空军生产、攻城连续性曾有相对好的工程结果，具体以 R9 native 报告为准，不以版本号推断表现。

## 四、已知用户反馈，可能跨越多个版本

- **地面 General**：远处任务不应调走整支大军；应抽取局部分队，靠新视野动态补充，任务失联仍保留合法重获视野责任。
- **空军**：两栖移动连续性差、威胁判定差、索敌紊乱；仅能实际对空的武器应构成空军火力威胁，优先清除当前可合法攻击的防空目标，然后其他目标；不能幽灵攻击或忽略实际危险。
- **Scout**：直线连续移动仍带表演性质，转向停顿明显；需要保留连续运动意图、合法路点与安全。
- **Builder/经济**：软 prospect 循环不能覆盖附近真实可施工矿，远端区域施工职责不能无故解除；后期现金和产能需要投到能结束比赛的目标，而不是无意义重坦、空军/地面重复命令循环。
- **最新 R16**：用户报告问题严重；尚无新的、精确 Game/Frame 和 raw 复盘，因此新投诉的具体症状 **NEEDS_USER_EVIDENCE**，不能编造。

## 五、推荐审查方法

对每个可疑回归，按：**输入状态 → Actor / Mission ownership → 合法 native 行动 → 下一帧位置/损伤/收益 → 是否完成区域目标**。优先检查缓存时代差、全名册成员代替实际近邻、搜索覆盖任务、Air 辅助队伍过度集结以及 Builder 被误认闲置。

所有新结论标记：

- `CONFIRMED_NATIVE`：原生后帧、结果和合法观察链直接支持。
- `CODE_SUPPORTED`：源码路径可证，但战果尚未实际验证。
- `HYPOTHESIS`：有合理机制但缺直接证据。
- `NEEDS_USER_EVIDENCE`：用户观察未附具体日志/时间点。

最有价值的输出不是铺陈假设，而是**按影响力排列的可验证故障链**，以及隔离于现有基线的最小补丁建议。不要在没有复现前贸然重构 G0～G6。
