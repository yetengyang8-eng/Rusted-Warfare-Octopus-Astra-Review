# 当前状态

更新：2026-10-01（Asia/Shanghai）。冻结基线仍是 **RW-BASELINE-2026-09-30-GS-v1**。先读 BASELINE、baseline-manifest、本文件和 NEXT_STAGE_PLAN；从本仓库最新增量提交继续，保留已有改动。

## 最新候选：运营与队伍调动

- **RW-CANDIDATE-2026-10-01-OPERATIONS-v1**，从 `8109e11` / Specialist Lifecycle 继续；实现提交 `3d96d98fc05d87569145c09f9f60d3e4a9fd185a`。
- 固定 JAR SHA256 `28668be7c49a2b5d8f7f326fbc3f365a2179d4c1b92a169959c58213ec44ecb7`，contentDigest `5805037b3d360966e161c2fcfba82ec527f84ab340509a5dd298912898b83c93`。
- Match 前有界 bootstrap：无建造者最多接受一次原生生产，确认新 ready builder 后重做 preflight；已有 builder / 待产队列不重复购买。
- 多队主力稳定编组，各队独立目标、前沿、进展和轮转；最多4队、每队48人，原专属 owner、合法视野、Target Guard 和共享命令预算保留。
- 富余资金在已有军力槽内少量补重炮，保护全部储备与两次普通补兵价；已付款未观察产品同时占用普通生产/能力采购容量。军力接近目标且收入富余时推迟升矿。
- 三项范围由本轮用户明确授权；此前单主题计划不覆盖这次新授权。hard cap、冻结引擎和专属单位生命周期保持原语义。

完整 Windows 回归 **47/47步通过**：23 Java runs、21 Python suites、342 Python tests、0 Python skipped、failed steps=0。最终报告解析器补做 **25/25** 契约及 **7/7** 控兵联动，合并矩阵为352个不同Python用例；没有把重跑次数相加成新增覆盖。策略169项、编队372项、经济42项合同通过，98个非manifest归档项与固定JAR相同。Windows文件锁真实执行；POSIX-only Java模拟按平台跳过，Linux本轮未运行。

两次普通Big Island原生局Battle均为VICTORY，修正运行器报告数量兼容后的复核整轮PASS。两次无builder夹具局都自动只生产1 builder，经济/发展PASS，Battle在1201.040/1201.104游戏秒预算到期PARTIAL/ONGOING。最新夹具局实际最多4队、106次局部命令、5笔原生报价3100重炮采购共15500、5个ready成品，末pending为0。该局矿富余拒绝和付款空窗未自然采到；前者有7场经济专项覆盖，后者有hard-slot时间线覆盖。4组16份报告运营审计与原全局/专属策略审计无违规，最终严格离线解析没有issues。

原运行器失败原样保留：旧三报告检查及旧单前沿解析都未适配新增报告/多队；修正后的离线结论另存。原始无builder地图只删除隔离副本中的己方初始builder，不能当作未修改Spain桌面验收。

交付见 [候选目录](../deliveries/operations-2026-10-01/README.md)、[证据](../evidence/operations-2026-10-01/README.md)、[输入分析](../evidence/operations-2026-10-01/INPUT_ANALYSIS.md)、[GPT / DeepSeek 交接](../handoff/HANDOFF_Codex_Operations_2026-10-01.md)。候选未替换桌面JAR，未晋级基线。

用户临时追加的4800游戏秒入口已新增在现有 Specialist Lifecycle 独立游戏目录，原900/1200/2400入口保留。该入口运行目录里已安装的候选；本轮没有把它替换成新JAR。最终交付也带同一4800入口。预算合同4项通过，实际4800长局尚未执行。

## 最新桌面输入及历史身份

本轮只读确认 `游戏环境/P1F-Astra-SpecialistLifecycle-2026-10-01` 已由外部安装 `b3e172de…`。两场旧候选raw分别522.465游戏秒 PASS/VICTORY、2401.120游戏秒 PARTIAL/ONGOING；实际mapPath/difficulty及未记录人工起步条件 UNKNOWN。它们是本轮输入，不是新候选验收。

长局存在同48人跨三千多地图距离切换目标、末余额222397且三座工厂空闲、接近军力目标仍升矿。43次拉扯按固定诊断口径复算，不能当成战损因果结论。

此前实现保留：Specialist Lifecycle `db0f6eec006a4f61f85134c38adc5c0f88a99e8a`、Capability Funding `fd8572849388d2e7fe04a5d446154602b54d3118`；各自历史验证见 [专属单位交付](../deliveries/specialist-lifecycle-2026-09-30/README.md) 与 [资金证据](../evidence/capability-funding-2026-09-30/README.md)。

## 后续起点

v1源起点 `6214f073d1b76429c0b827268db918e1e90e9f5f`、冻结JAR `76711a8e…` 和原验收保持原样。只读核对106文件及环境，只有已完成增量源码漂移；不据此reset。默认开发仍是本仓库agent/tools，旧developer或解压目录不替代当前起点。

下一阶段补新候选真实桌面上下文及重复自然样本，比较队伍拉扯、成品和合法任务完成，再按证据处理护送、撤退和终局，见 [NEXT_STAGE_PLAN](NEXT_STAGE_PLAN.md)。本轮前状态/计划保存在 [归档](archive/2026-10-01-before-operations/CURRENT_STATE.md)，旧“当前”“未部署”仅代表历史时点。
