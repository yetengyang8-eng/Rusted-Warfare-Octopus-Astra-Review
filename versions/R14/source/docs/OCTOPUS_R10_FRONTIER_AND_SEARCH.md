# R10：响应回流、经济前沿与持续搜索

起点：R9 `56a0598`。产品集成后，Spain 暴露的 runtime 接线与施工连续性修正为 `6d0fc7f`、`769735e`、`d0d4f9e`。这轮沿用重坦、Ground General、Repair、施工与 AirGeneral 主循环，没有增加跨独立 actor 的全局操作额度。

## 临时响应与 origin

Commander 对当前合法可见任务区分机动小目标与战略目标。`ThreatTask.smallResponseRoleKnown` 是一次当前 row 分类结果：基地/建筑保留战略任务；合法 `building=false` 或可信 profile 的正移动速度可证明机动角色；未知角色不强行归为小骚扰。Commander 和 Force 使用同一分类。

小任务先使用已有 LocalResponse 分兵路径，优先 FREE/补兵，再借 JOINING，最后从附近 General 借少量兼容成员。数量沿用现有 HP 粗预算、兼容性与响应范围；不把 HP 预算解释为战斗模拟器。没有足够兼容兵力时记录欠缺，小目标不会因此驱使多个整队重复响应。基地与较大任务继续进入原有 whole-General allocation。

每个临时借用保留 origin，实际 owner transfer 仍 generation++：

| 原状态 | 临时响应期间 | 当前合法 visibility clear 后 |
| --- | --- | --- |
| FREE | LocalResponse owner | FREE |
| FREE + PENDING_JOIN | pending reservation 保留 + LocalResponse | JOINING，保留目标 General reservation |
| JOINING | 暂停接近、保留目标与 reservation | 原 JOINING owner，重新需要 receipt/后帧到达 witness |
| ATTACHED | 从当前 roster 移出；原 General 以 reservation 计入借出兵力 | 原 General owner + ATTACHED，恢复原 General 任务 |

这是用户这轮明确要求的**临时借兵路径修订**，不是把所有 G4 detach 改为自动回原队。Recovery、失效 General 或外部接管会使原恢复条件失效，不能复活旧 origin。General reservation 使借出期间不会重复购买/分配同一个空缺，也不把外出的兵算进当前 centroid。clear 使用当前合法 visibility；UNKNOWN 不清，失联不计击杀。旧 generation Intent 始终失效。

当前仍复用一个 LocalResponse controller，未增加多小队系统。不同地点同时发生多个骚扰的完全并行响应不是本轮承诺。

`/state` 与 `/combat/observe` 是独立端点。Commander 保留本轮 own observation 的分配权限，但任务 source time 匹配到通过现有 freshness/session/player 校验的 combat packet；Force 使用同一 combat source。两端不能要求同帧同时间。修正前自然局零次临时小响应；修正后的自然运行已出现实际借兵和 FREE/JOINING/General origin 恢复，完整统计见 handoff。

## 到矿资金承诺

远途 prospect 是软意向。以下事实同时成立才建立 `Worker.mineQuote/mineReserve`：

- 当前己方 ready Builder、IDLE、有效 ownership；
- prospect 的 accepted move 已有后帧己方位置到达 witness；
- 当前 `/expansion/plan?unitId=...` 为 planned，session 与 builderId 相符；
- 真实 finite positive extractorCost、当前原生候选坐标；Builder 已用后帧位置证明到达 prospect 的本地施工区，当前 native 合法、可达候选与 Builder 仍处在这个已到达 cluster 内；
- 当前/保守合法局部威胁检查允许，并且没有其它施工占用同一候选。

报价来自真实 native endpoint，保存 `costSourceObservationId + costSourceRequestPath`。不按 Builder 人数静态预留 700，也不把 `/state` 当价格来源。保存 quote 的副本仍带原始 provenance。

`BattleClient.strategyReserve()` 汇总这些 arrived reserve；普通连续生产和新增未支付 Capability/Air 意向必须让出这笔资金。矿自己的执行预算加回自己的 reserve，避免重复扣除。Strategy 首先落实已筹款矿，再做后续工作。新增 air fund 使用真实报价核对剩余资金，不能在第一份 jet 价格尚未知时压住已经筹好的矿。

已筹款、尚未凑齐报价的 Worker 由这条真实施工承诺持有；普通选点不能又发 prospect MOVE，撤掉它自己的 bank。当前风险、ownership、job 或 native candidate 改变仍走正常释放；没有新增 clear timer 或全局生产停机。

accepted build 后，未支付 reservation 转为原有 execution spending/slot commitment，继续等 native spending/construction witness，不把 receipt 当矿完成。Builder 离开、任务/ownership 改变或当前合法候选改变会释放未支付意向，不产生“退款”。endpoint 暂不可用是 UNKNOWN；最终 native command guard 仍权威。已有 paid-before-visible/ghost 不按计时释放。

这是现有 allocator 已能落实的 native local construction 阶段，不新增“必须靠到矿心75”或“必须先到最终静态 rally”的 gate。远方静态候选以及尚无后帧 own arrival 的 travel 仍没有硬资金承诺；附近当前合法矿可以先施工，不必为了远途意向忽略已经可做的本地工作。现有600 cluster与native候选范围沿用，不代表矿安全或实际已完成。

Builder 需求仍采用 R9 的实际地面军队空间、准备开发机会与施工服务需求，没有用全图潜在矿区数购买 Builder。危险绕路消费 ScoutBridge 的当前合法 waypoint；先验证 session/unit/tile/source time，再发 MOVE。静态地图连通不宣称当前路径安全。

## Scout：长任务，短中断

`FRONTIER_SWEEP / EXPLORE` 保持同一 Task、lease 和 generation。获取少量视野是进展，不是任务完成。当前完整合法 frontier route 有 cursor；receipt 不推进 cursor，后帧实际位置推进。路段仍复用现有长度和安全检查，不提高指令频率。

RECHECK 只能在探索完整 route 的实际到达边界临时借用，`suspendedFrontier` 保留探索身份。合法检查结束、原有 timeout、当前风险/健康/接管变化后恢复原探索，重新取得合法 route。不能无限用一条旧 intel 重复中断。

当前危险先尝试合法绕路；有可用 detour 时可以直接替换 MOVE。找不到安全合法 route 时保留长期任务，避免把等待当完成。已知威胁只用于保守避让，不攻击 ghost、不推断隐藏位置。

`/scout/plan?role=recon&routeScope=scout-search` 额外输出完整合法 route 与当前 frame/time。`/scout/resource-approach` 保留旧 x/y，同时增加 waypointTile/X/Y、safeSegmentKnown、routeScope；采用合法已知 terrain + remembered threats，实际当前位置到首 waypoint 也验证直线危险。

任务连续性不等于 Scout 自然存活已证明。原版路径偏移、未知敌军、单个 Scout 缺席以及补充 Scout 的长期机制仍须真实对局观察。

## 新增资源的空间职责

`MilitaryInvestmentPolicy` 是轻量粗分配，不是战争结果判定。仅在独立 `/combat/observe` 当前 session/time 合法，且 current rows/target domains 足以解释时评估可见地面压力；UNKNOWN 不当零。已有健康地面军力覆盖当前粗需求，且已有 endgame own-force readiness 成立、空海信息任务仍未结束时，让新增产能与资金更积极服务空海。

这不宣布大陆清理、不推断雾中敌人消失。日志明确输出 `mainlandCleared=UNKNOWN`，HP proxy 只作 coarse allocation。战术交换仍由已有 FightEstimate/GeneralCombat 处理。

执行层保护**一个真实下一份** engineer 或 amphibiousJet 报价，使普通陆军消费不能吃掉每次刚积累的空军资金；没有固定飞机比例或坦克停产数。ground runway 从各陆厂一轮全量缩为当前一个真实产品报价，必要补充继续。扩厂决策在已有 ground covered/spatial duty 时让新增 capacity 服务 air，不改变工厂合法生产 guard。

更多后方 provider 的目标同时参考实际收入、现有陆军消费和已观察两栖制造 service rate；未得到 service witness 不伪造吞吐。pending 采购不重复，实际 rear purpose 不被其它 Capability need 偷走。矿、修复必要承诺和已支付资金保持；屯蛋仍排在未补齐空中制造之后。

## 多 AirGeneral

`AirGeneralController` 保持现有 facade，每支 formation 有独立 `air-general:N` owner、generation、members、route/cursor、mode 和 target。初队接口兼容，snapshot 增加 formations/formationCount，aggregate roster 仍供原 Strategy 使用。

只有真实可用 ready/健康 ATTACHED 飞机足够、存在另一个完整合法且信息得分正的分离 frontier 时才分队。沿用现有成军/保留规模（6/3）；一次己方 frame 最多探测一次新的分队机会，不设将军数量固定上限。转入子队仍 JOINING，至少经过更晚 own frame 的实际位置到达 witness 才 ATTACHED；原本就在子队 anchor 附近也不能同帧直接 ATTACHED。需要移动的成员独立发令，receipt 本身不算到位。普通 Ground JOINING 的既有 receipt/后帧合约保持。

各队领取分离 frontier，不要求同步。当前合法接触由合适的独立 owner 处理，其它队继续 route；clear 恢复本队 cursor，不能把 lost contact 当击杀。逐机 desired/requested/physical/weapon/terrain 证据继续分离。没有新增隐身扫描、隐藏敌方读取或新的原生作弊入口。

当前 SearchArea 的其它历史 LAND demand 条件不冒充 AIR 路由权威；这轮分离职责由合法原生 AIR route + frontier 信息得分证明。水下首次合法发现、搜索效率、多个自然队伍的胜率与 native victory 独立于 fixture。

`/state ownUnits` 不导出 `movementType`。分队不得依赖这个缺字段，也不能把它补成AIR；资格由当前 own ready、现有 ATTACHED/非恢复、实际 native mode witness 与 AIR_READY 决定。requested Fly、旧物理AIR但仍FLY_REQUESTED、SUBMERGED或UNKNOWN不合格。当前 navigation type、物理模式、武器/地形合法性仍不是同一证据。

## Human Validation

所有新 kit 默认启用 display-only `/state gameTimeMs/frame`。控制台标题和 live HTML 顶部固定显示 `Game 13:42.6 | Frame 245731`；读取失败/换 session 时显示 UNKNOWN，不使用墙钟推算、不参与 Agent 决策。只读轮询约每秒一次。7200 单倍入口传入的 14400 秒 wall safety 不再被客户端悄悄截成 3600；正常预算结束为 PARTIAL。

T2 scout aircraft、核弹以及完整主动 Capability Search 不在本轮。原版容量精确 API、UNKNOWN construction payment 历史消歧也未借本轮改成计时退款。
