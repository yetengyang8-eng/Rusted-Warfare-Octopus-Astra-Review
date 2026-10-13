# Codex 提示词 — Octopus G5.1 真人实战修正

继续 `G:\deepseek 工作台\GitHub发布\Rusted-Warfare-Agent`，当前分支 `codex/octopus-g3-execution-20261002`，以 `ca2fc92` 为已验收 G5 起点。先确认工作区状态并阅读：
- `handoff/HANDOFF_Octopus_G5_2026-10-03.md`
- `handoff/HANDOFF_Octopus_G5_HumanSpain_Feedback_2026-10-03.md`
- `docs/OCTOPUS_G5_COMBAT_RUNTIME.md`
- 必要时直接读取真人 Spain raw/replay；不要重新做 G0~G4 全仓审计。

本轮目标不是继续堆测试或继续追求更高 decision frequency，而是把已经成立的 G5 从“能打、会撤、会自然长出多 General”推进到“更难被真人稳定针对、战略推进更有效率”。请把真人长局当主证据来源，自主定位根因并做跨模块修复；不要求机械逐项实现下面每条，如果发现更基础的共同根因，可以优先解决共同根因。

已证明且必须保住的能力：Spain 自然形成 4 支 General；多路独立推进/撤退；长期生产扩矿；合法 fog/证据边界；大军 >48 分块；bridge/native 无 desync/resync。不要为了修漏洞退回固定 build order、固定路线、随机扰动掩盖问题，或重新把所有军队合成一团。

优先解决的真人实战问题：
1. 第一波过于可预测且容易送进敌基地/静态火力；对手见过几次后可稳定利用。让当前合法可见威胁、基地火力、局部交换、后援距离和撤退成本真正改变进攻选择，而不是简单随机路线。
2. 对空军截杀缺乏适应。当地面主力遭遇当前合法可见空中威胁时，应能产生能力需求、避战/撤退、补反空或其它合法应对；不要从 LOST_CONTACT 推断隐藏空军位置。
3. Builder 的 prospect 缺少局部承诺。Spain 中 5924 已到北部安全资源区，又被全局重选拉去南面，浪费约 1:50。优先考虑 local resource cluster、travel sunk cost、已探安全区承诺：已经远征到低威胁矿区后，应优先吃掉当地可达空矿，再允许跨图重规划。
4. 战略吞吐偏慢：G5 不是“不醒”，而是阶段转换慢。真人基准中首厂差距不大，但 6坦、3矿、首厂升级、矿升级明显落后；中后期最高 credits 约 17648，`factoryTarget` 仍基本为 2，`factoryTargetIncreases=0`。修“收入→产能/升级/兵力”的转化，不要只调一个固定阈值。
5. 命令效率：G5 约 109 command APM，对比真人约 54，但大量单单位 move；总单位引用量只高约 11%。减少重复覆盖、稳定编组复用、已有有效命令不必每轮刷新；在合法且有价值时允许 direct target attack，而不是全部依赖 attack-move。

工程方式：给你较大自主权。可以做跨模块重构、增加诊断/fixture/工具，只要服务于真实问题；测试是反馈手段，不是开发本身。每个实现块跑 focused tests，必要时跑短自然 headless smoke；不要每改一点就全量回归。候选收束时再跑一次完整 Windows regression，若失败必须保留原始失败和因果说明，不能改断言糊过去。

硬边界：
- 所有战术/威胁判断只使用该玩家当前合法可见信息、己方状态和允许的静态地图知识；ghost 只能维持保守窗口，不能触发新攻击或隐藏位置推断。
- `CLEARED != killed`；敌失联/己缺席不自动算死亡；不要发明 K/D 或击杀归因。
- HP proxy 当前仍是近似，可改善但不要把未知包装成精确战斗力。
- 保持 ownership/generation/ABA、FREE/LR/JOINING/ATTACHED、每单位唯一有效 command owner 等 G3/G4/G5 契约。
- 不要修改 Universal Bridge 来掩盖 Agent 问题；本轮主工作在原 Agent 仓库。
- 不要启动、关闭或自动操作用户桌面游戏。需要真人验证时，产出可执行入口和明确观察点，等用户自己操作。

真人 Spain 证据入口：
`G:\铁锈战争 桥 工作空间\headless-runs\human-g5-spain-20261003-01`
Agent raw：`...\agent\rw-agent-reports\battle-1791014693923-d7f87169.jsonl`
Replay：`G:\deepseek 工作台\游戏环境\rustedwarfare PC 1.15 原版\replays\ 10p 西班牙混战 by MP97 [v1.15] (3 10月 2026 16.04.39).replay`

本轮请直接推进到一个可运行 G5.1 候选，而不是只写方案。交付时说明：实际修改、真人证据对应的根因、哪些能力被证明/仍 NEEDS_EVIDENCE、focused/native/smoke/full 结果、候选 JAR SHA、人工验收入口，以及下一轮最值得看的 3~5 个现象。

如果需要子智能体，只使用 gpt-6.1-sol/high 或等价档；避免 Astra/Astra Work。不要让多个子智能体重复做全仓审计/全量回归。
启动注意：当前 `ca2fc92` 产品树原本 clean，但现在 `handoff/` 下有本轮新增的人类反馈/提示词文档可能处于 untracked 状态；它们是有意留下的输入，不要 `reset/clean` 删除。先 `git status` 识别，再从 `ca2fc92` 产品基线继续。