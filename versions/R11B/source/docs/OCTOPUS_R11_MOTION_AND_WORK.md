# R11：真实执行批次与连续行军契约

产品提交 `197310a`；继承 R10 ownership、资金、slot、可见性和两栖证据边界。

## Runtime 的当前 observation

G5 主循环先 confirm/updatePending，再为当前合法 `/state` 建立 ExecutionScheduler observation，随后调用 Strategy.observe。Strategy 的 Worker 状态更新可能续发 MOVE；这些合法命令必须绑定当前 batch，而不能被旧 batch 拒绝。一个决策只 begin 一次；后面的 force collect 不重新清除 actor 冲突记录或报价。G3/G4 legacy 模式保持原来的 begin 位置。

没有新增跨独立 actor 的人为 APM/token 上限。force controllers 仍 collect → priority → dispatch；成熟 Strategy/经济仍 immediate dispatch。这里没有声称全 runtime 全局优先级排序。owner/generation、同 actor 冲突、实际资金承诺、producer slot 和未消歧支付继续有效。

## Builder：矿点目标与路段分开

- `prospectResourceTile/buildX/buildY` 保存工作目的地；`x/y` 是当前合法路线 waypoint。到达一段 waypoint 不意味着到矿、不意味着可建或安全，也不触发从全图重新选矿。
- 当前 `/expansion/plan` 已给出本地合法 native 矿报价时，优先进入原有施工资金/guard 链；否则续同一矿点的合法路线。
- `/scout/resource-approach` 从现有最多六个 BFS steps 中选择最远的完整合法 chord；检查经过格子及对角两侧格子都属于当前 lawful reachable grid，再检查完整已知危险圆交叉。不会直接沿最终矿点直线跨危险或 UNKNOWN。
- 同一个短点、失败的替代点，均不能绕过原有无进展超时。只有己方实际坐标移动刷新 progress；接受不同短 MOVE 也不当进展。危险新出现且无法安全续行时取消旧危险路段；能证明当前返基地 chord 不穿已知危险则使用该方向，否则取消在当前己方点。没有宣称该取消点安全。
- 真实到矿报价仍保护于普通生产/新增 Capability 支出。多个同级矿报价按实时有效现金依次落实；第一座可负担的矿不再要求其他所有矿也同时凑齐资金。实际 accepted 后仍走现有 unsettled credit，不推定花费/建成。

## Scout：在途延伸而非 receipt 完成

EXPLORE task/lease/route 保持。后帧当前 own MOVE witness 加实际走过的同一直线 route cells 推进 cursor，可以在老 waypoint 到达前续下段。转弯仍要求实际位置依据，不能切未知地形。当前合法危险变化可以立即改向；相同失败几何保持已有退避，危险变化不被旧15秒退避吞掉。临时 RECHECK 保留其证据结果语义，但不能冻结原 EXPLORE 的在途移动；UNKNOWN 不被当 CLEAR。

## Ground General：推进与 fog patrol

使用健康当前 ATTACHED 成员中最靠近当前 centroid 的成员作 anchor。anchor 的后帧己方位置接近合法旧 leg 时可以请求下一个合法点，不等待全员75%到达；只有真实 quorum arrival 才完成旧 waypoint。提前接续的 Trace 原因为 `CURRENT_OWN_FRONTIER_LOOKAHEAD_NOT_ARRIVAL`，不记作视野覆盖或信息 frontier 已到达。

原生 army planner `no_frontier` 后复用合法 recon planner 搜深雾/陈旧视野。仍经过现有当前风险、已知基地和合法 native path 检查；找不到合法方向保留 UNKNOWN/不可规划，不伪造移动。

孤立当前明确 `canAttack=false` 的建筑可以进入现有小响应预算，由附近低成本兵/少量临时借兵处理；不会因为 building 标签自动升级成远方 whole-General 任务。未知/武装建筑、基地、超出现有小响应包络的集群仍走战略预算与中断成本。不新增一套威胁评分。

## Air：独立搜索接续和职责避让

Air 的在途 lookahead 只使用本队已接受的 route、严格后帧 own matching MOVE 和合法安全下一段；不提前推进 arrival cursor。独立 owner/generation 和 requested/desired/physical/weapon/terrain 模式证据分开保持。

`groundTargetCommittedStrategy` 只承认当前合法目标、当前独立 Ground ledger、当前所有权及 accepted generation/revision；同帧实际 accepted 可以占目标，后帧必须有当前 native advance。原生 idle `orderType:null` 不能依靠旧 receipt 继续占目标。Air 让出普通 SURFACE 目标后续自己的搜索；AIR、SUBMERGED、native touchingWater 或跨域 Capability Need 保持 Air 职责。

缺失/过期/异局 combat 仍 UNKNOWN，不能当目标消失、死亡、胜利或新攻击许可。SearchArea/搜索覆盖仍须原有实际位置证据；没有新增核弹或 T2 Scout。
