# R12 Builder 工作与预约契约

本轮从 R11 `f4d81e8` 出发，只修普通 Builder 的找矿、施工预约和原生命令兼容。产品提交 `7b9e4e5`。未改 Ground/Air/Repair 主循环、全局调度额度或侦察频率。

## 区域偏好与具体施工分离

`ProspectCommitment` 的已到达区域继续提供就近开发偏好，**不再是区域独占权**。其他 Builder 可以开发同一片区域中的另一座矿。

具体矿点的排他依据仍是己方真实任务：未付款的当前 native 报价预约、已付款待 witness 的 extractor 施工、当前 PROSPECT 的最终矿点，以及 baseline Economy 的 BuildJob/early expansion。使用原有矿点去重距离60，不把旧 static tile、IDLE 旧目标或600范围偏好当施工占用。

baseline Economy 与 Strategy worker 保留原 owner/BuildJob，只共用具体矿点排除；baseline 不排除它自己的 BuildJob。owner/generation、同 actor 冲突、unsettled credit 和 paid ghost 继续由原机制处理。

## 原生报价与施工优先

`GET /expansion/plan?unitId=N` 的旧行为保持。可选新参数 `excludeSites=x,y;x,y`，用 URL encoding 传输。排除仅表示己方施工选择，不授权目标；Bridge 仍先检查当前 visibility，再读取资源格，并核验实际 native footprint、当前可达路径、工人范围与真实 action/price。排除后重新选最近的当前合法矿。其他经济 endpoint 不接受这个参数。

Strategy 如果第一座报价矿已有真实任务或已知危险，会向 endpoint 请求排除后的另一座矿，并再次检查响应。未知/无合法替代就返回 UNKNOWN/无计划，不生成虚构坐标、价格或合法性。沿用1024 query transport 上限。

当前自己 ready、合法 owner、IDLE、没有原生 build/repair 或 paid construction 的 Builder，只要获得在其实际当前位置600工作范围内的当前合法报价，就可形成矿资金预约。无需先到旧 rally，也不要求矿属于旧 cluster。旅程本身仍是软意向；未知远矿不预留固定700。

矿预约先由现有资金链落实，accepted 后走已有付款/slot/witness；accepted 不等于到达、实际扣款或矿建成。缺少 endpoint 不能计时退款。每座可负担矿独立执行，不要求所有矿预约同时凑齐。

## 旧订单与新找矿命令

- 当前 own 原生 `orderType=build/repair` 时，普通分配和普通 prospect MOVE 不覆盖施工，即使本地记账已回到 IDLE。
- PROSPECT 发现实际 native 建造则结束自己的 travel bookkeeping，保留原生工作；不是再次下造矿，也不改变命令 owner。
- critical HP 的既有合法 retreat 仍可中断，未宣称施工绝对不可打断。
- native idle 必须来自当前 own 订单字段；字段缺失是 UNKNOWN，不能当 idle。
- 当前确实 idle 且途中已有合法当地矿时，可以转施工；没有当地工作才续同一最终资源方向。相同 waypoint 只在原生 idle、实际仍未到位时允许恢复丢失的 MOVE，不每轮刷新有效命令。
- 到达仍要求后帧自己的实际位置；accepted 不刷新真实移动 progress。
- 静态 approach 暂时不可用不永久污染 `prospects` 集合。

## Trace 与验证边界

`strategy_frontier_funding_committed.reason=CURRENT_NATIVE_ACTIONABLE_SITE_AND_OWN_WORKER`，`ownPositionSourceFrame` 表示当前 own 位置来源，不伪称旧目标 arrival。

worker 旧/新状态增加 `resourceTile/resourceX/resourceY`、`waypointX/waypointY`、`regionalPreferenceTile/regionalPreferenceArrived`、`mineReserve`、`regionalPreferenceIsExclusiveReservation=false`。报价保留真实 `costSourceObservationId/costSourceRequestPath`，包括带 excludeSites 的真实 endpoint。

`R12BuilderWorkHarness` 覆盖附近多矿预约、过时 IDLE 目标、旧区域之外的当前合法工作、继承 native 建造保护、第一报价冲突后重新报价、baseline 与 worker 去重及 UNKNOWN 替代。`R12BuilderNativeHarness` 使用原生对象、菜单、地形与 fog guard，验证两条不同矿点命令进入 native 队列；`simulationTicks=0`，不能冒充自然施工或建成。自然结果另见本轮 handoff/evidence。
