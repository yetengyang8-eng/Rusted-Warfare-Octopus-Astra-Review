# Octopus G5.1：战术准入、收入转化与局部运营契约

起点 `ca2fc92`；保留 G5 的 decision/token cadence、多 General、独立 owner、force collect/priority、成熟 lane immediate dispatch。本轮不提高 decision frequency，不改 Universal Bridge，不启用 SearchArea。

## 当前可见目标的准入

GeneralCombatDirector 先使用当前同 session/player 的合法 contact packet；旧/缺失/外国/非法 contact 不产生新攻击。目标候选仍须通过现有 native engagement / known approach，最多48 actors分块。

TacticalOpportunity 为每个当前可见候选计算保守 exposure proxy：目标周围600 world units 内 armed mobile HP、armed building HP×2、后援距离最多35%成本、近处己方后援最多25%折扣，再加最近合法 own HP下降成本。该值不是真实DPS、射程、击杀归因或K/D。候选按距离/exposure/经济目标价值/已有目标/Commander attention确定性排序；不使用随机路线。

暴露比与交换成本超过1.15，或有已知本队反空缺口时，拒绝该目标；尝试其他合法目标。无其他准入目标时安装距拒绝目标约700的边界内 standoff attack-move，覆盖之前危险的前进命令。不得从拒绝目标回落到其后方 frontier。该几何点仍是 `SAFETY/REACHABILITY_UNKNOWN`，native guard 保留。当前本队已接近静态火力或 AIR 缺口时进入现有 retreat/regroup；新的合法证据可以解除保守状态。

Trace/Observer 的 General 行新增 `tacticalChoice / tacticalReason / targetExposureRatio / localExposureRatio / visibleStaticHp / retreatDistance / nearbySupportHp`，以及 `capabilityNeed / currentVisibleAirThreatIds / visibleAirHp / compatibleAirHp / unknownAirHp / airCoverage`。来源时钟与事件发生时钟仍分离。

## AIR 需求与普通生产

AirResponsePolicy 只读取当前合法可见、fresh domain、可信 catalog 的 armed AIR，且距当前 ready ordinary force≤700。敌失联不生成需求，不从 LastObservation 找隐藏飞机。玩家身份可由 native `player.teamId` 导出；非法/陈旧来源为 UNKNOWN。

健康 ready ground/HOVER 反空候选与现有 accepted pending counter共同计入覆盖。需求只给普通生产一个有界偏好（目标2～6），不创造 attack permission，也不保证反空单位已到前线。

产品从当前 native factory menu 的 `COMPATIBLE(AIR)` 原生 action中选择；不硬编码厂级路线或价格。菜单可能选择原版 `scout`，这里只是普通反空采购，不激活 Recon/Scout 新战略。保护 builder、mine/investment、capability 与 primary reserve；每个 counter Intent显式 `militarySlots=1`。实际价格/action、credits/slots/unsettled/ghost与producer commitment继续由 G3+native守门。

现有 Bridge没有可用 direct target attack 接口，继续合法 attack-move；也没有 group move 接口，JOINING/retreat仍逐单位。没有通过修改 Bridge 来制造这些能力。

## 收入转化

Strategy 的 read-only capacity assessment在 G5.1纳入 current ready General-owned ordinary force；仅用于统计，不借用 owner、不发令。这修复“成军后 Army assessment空掉→长期NO_USEFUL_DEMAND”的观察口径问题。

远处 General 的交换不再等同 homeland production recovery。builder恢复、当前可见home500内armed威胁以及未知当前敌情仍保护恢复资金。非法/陈旧/缺失行不能当安静。

现有 native upgrade可在至少6个健康 ordinary、健康占比≥60%、各 reserve及一个真实 replacement quote资金受保护时选择。replacement quote与军队slot分离：满编仍可升级现有工厂，不能新增军队slot。预计正净收入能在30 game seconds内覆盖时允许有界tech wait；收入来自既有测量模型/当前矿数估计，消费来自accepted production ledger。`fundingEstimateStatus=MODEL_ESTIMATE_NOT_NATIVE_THROUGHPUT`；不宣称已知道未来产品或native生产时长。

新矿投资在健康早期主力与真实 replacement资金存在时即可考虑，不等满32/43 policy target；当前本土危险/恢复仍保护资金。ready施工完成后释放额外15秒扩张等待，但不从accepted receipt推施工完成。

扩厂/军队容量仍走既有150s收入/需求/安全窗口、80% producer utilization、native route、reserve、hard cap与单factoryCommitmentId。没有cash-only扩厂，也没有降低80%阈值。

## Builder local prospect

ProspectCommitment从真实accepted move记录目标、frame/time与旅行成本。只有严格后帧己方位置到达75内才能建立600半径local cluster；这不说明矿点安全、空闲或可达。

到达后优先local native construction plan、local resource及当前合法 approach；资金不足等待，UNKNOWN保留承诺，局部继续移动不漂移cluster anchor。禁止先把该worker跨图全局分配。当前危险/owner或capability接管可解除；相隔至少8game seconds的两次完整当前空簇评估才能解除。原生occupation/path/build/menu/price guard仍权威。accepted不算arrival或build完成。

## 命令复用与证据

有原生orderX/Y时，命令复用要求owner generation、当前revision、目标/模式、严格后于receipt的frame、匹配native order和有界己方进展；停滞重新允许发令。新的危机/目标/owner变化立即取消旧复用。只有后帧位置可用于JOINING arrival，receipt不算execution。

退却途中，已取得本generation/current revision严格后帧rally位置见证的个体不继续刷新move，即使整支General尚未满足75%集结门槛。个体位置见证与General regroup是两个不同结论；新威胁或新rally不复用旧见证。

旧兼容真人 Bridge可能缺orderX/Y。仅当两键均缺失，当前orderType匹配，严格后帧且向已接受几何目标有净距离减少时允许短暂推迟刷新。诊断 `DESTINATION_UNKNOWN_PROGRESS_SHORT_DEFER`，不是原生命令目的地 witness。General/retreat/JOINING硬窗口分别6/4/12game seconds，到期重发；partial/null/非法或不同坐标不适用。实际减少APM及推进收益需要新真人raw。

`rwagent.g51=false`只关闭本轮BattleClient运营/Strategy assessment接线；不作为完整回退G5战术的A/B开关。`battle_config.g51Operations`明确该范围。对照旧产品须使用旧候选JAR。

所有G3/G4/G5 ownership/ABA/唯一owner、FREE/LR/JOINING/ATTACHED、paid-before-visible、两栖证据分离合同保留。当前General数量与运行频率不因本轮改动提高。
