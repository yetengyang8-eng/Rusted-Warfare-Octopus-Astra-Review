# R8：施工、推进与空中搜索的持续执行契约

本轮从 R7 `366dabd` 延续。它修正已成立模块之间的执行断点，不重建 Commander、General、Recovery、重坦与经济循环，也不声明 G6 终局验收完成。

## 施工与真实预算

现代 G3 运行的 `BuildJob.orderAccepted` 对所有普通施工生效，包括非 capacity 扩厂和建矿：同一 job 收到 accepted receipt 后，在后帧己方现场出现前不再次提交、不重复记账。现场实际 buildProgress/ready witness 仍走原有观察和完成路径。显式关闭 G3 的历史路径保留冻结行为，不作为当前候选的运行模式。

receipt 不证明施工、付款、ready；未知施工没有 timer 退款。本轮不替真人旧局两笔同址 700 承诺猜退款。该旧局只有一个现场效果，不能为两笔支出提供唯一证据。

已真实到达 prospect 区的 Builder，若当前原生 `/expansion/plan` 对本单位、本 session 提供既有 arrival radius 内的合法未承诺矿点，但 effective/free 预算不足，则保留当地工作，记录 `strategy_local_mine_waiting / LOCAL_SITE_AWAITING_FUNDS`。等待仍使用原有 retry；不重新发小范围探索 MOVE，不把 native wallet 当可自由支配资金。预算足够后从当前报价进入原有建矿路径。危险、占用、报价缺失及远处候选维持既有判断。

## 命令构造前的局部拒绝

Strategy 菜单可比 `/state` 新。菜单里的 actor 不在当前合法己方 actor 集合时，构造 generation snapshot 前拒绝本组提案，不能让 `IllegalArgumentException` 结束整局，也不能把这个 ID 注入 ownership。

`g3_admission_rejected` 保存 owner、actorIds、path、cancelReason、observationId、`admissionPhase=BEFORE_GENERATION_SNAPSHOT`；`nativeAttempted/nativeAccepted/commitmentApplied=false`，`actorAbsenceProvesDeath=false`。G1 command span 同时保留本地拒绝因果。拒绝清掉该命令的临时 quote context，不发 HTTP、不占 slot、不留资金账。

G4 collect 路径同样拒绝当前缺席/越权单位；已 collect 的 Intent 仍交 Scheduler 原有 generation 校验，transfer/release/ABA 后旧 Intent 无效。混合合法/非法 actor 的组整体拒绝，不静默删掉成员。后续独立合法 actor 在同一 observation 继续工作。

## General：战斗与长期推进

Base Threat Zone 继续保护小编制的普通 travel；失联基地没有攻击授权。若阻挡基地当前合法可见且本组取得当前原生 eligibility，它使用与正常战斗相同的已知 exchange 判定，或可信己方 profile / UNKNOWN-defender 的既有 exposure fallback。不能再由记忆防区重复要求完整已知 TTK，否决已经通过的当前战斗。当前实际损失、空中威胁、未核验机制和 native 不兼容仍有意义。

OutwardMission 的后帧到达只完成 accepted `targetTile` 中途 waypoint，不能完成尚未抵达的 `frontierTile` 信息前沿。同一外侧 frontier 的下一合法 waypoint 可以继续使用。失去 anchor 的 movement domain 不算到达。

native exhaustive fallback 可能忽略 soft avoid；同一 collect 已返回的相同 waypoint/frontier 候选不反复求第三次。已有合法 detour 可以复用，否则留待下一实际 observation。没有增加时间降频 gate、凭空坐标或探索完成假证据。

## AirGeneral：完整合法路线与路段 cursor

新请求仅是 `/scout/plan?unitId=N&role=recon&routeScope=air-search`。它要求当前原生己方 AIR movement、核验 TerrainSemantics，并返回完整合法 `routeTiles`、独立 native frame/gameTime、`routeComplete=true`。无 scope 的旧 Recon/Ground 输出与 260/640 world 路段合同保持原样。完整静态 AIR 地形可知，隐藏动态单位、建筑、当前安全性和原生全局动态 path grid 不可作为地图先验。

AIR 选择先覆盖合法可达 never-seen 信息前沿，再 deep/stale；局部旧 fog 可选不代表全图信息已经穷尽。完整 route 不意味着直飞终点：controller 保留 cursor，只合并连续且当前合法威胁证据允许的直线路段，不切 BFS 转角，不跨未验证的间断路线。

leader 自身后帧位置、已接受或实际观察到的 MOVE 才推进 cursor；其它成员先到不能冒充 leader 到达。leader 当前缺席可以从合法剩余成员重新选择，不把缺席称作死亡。原生 route source frame 较新时保留计划，等己方位置 anchor；原地 hold 不视为新危险 travel，不反复清掉待续 escape route。

新危险可否决实际旧 MOVE/attackMove，并发一次 MOVE 取消危险行进，然后重规划；remembered threat 只用于 travel veto，不授权攻击。合法当前目标可以暂时接敌，失联目标不继续授权进攻；之后重新搜索。完整 route 到达仍会继续选择信息目标，不宣称全图已清、敌全灭或 native victory。

## 调度边界与剩余证据

G5 仍使用 independent actors，未恢复全局人为 APM 上限。保留 owner/generation、同 actor 冲突、有效命令复用、资金承诺、producer slot 与 military safety cap。Ground force 是实际 collect→priority→dispatch；经济、Strategy、Recon、AirGeneral 保留当前阶段 immediate dispatch，不声称已成为全 lane 全局排序器。

循环阶段 wall timing 包括 nested reads/commands，不是 CPU profile。减少重复 frontier 查询和缩短任务间空等，不保证任意大规模下 decision latency 的固定上界。

Scout 的旧独立 Recon、危险改道、伤后恢复合同保留。本次真人首 Scout 约 108 秒受伤后没有合法恢复/续航证据，不能用 AirGeneral 的测试宣称它已在真人长局持续侦察。原版自然水下首次发现、独立 WATER/ISLAND 搜索、空海集中歼灭及 native victory 仍按最新 handoff 明确标记证据。
