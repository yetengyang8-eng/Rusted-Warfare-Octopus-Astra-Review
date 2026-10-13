# G5.4 R5：独立执行与恢复资金闭环

这次从失败的 R4F / `a96178c` 出发。产品提交 `32eab8e`；没有提高 decision frequency，没有新增 G6 / SearchArea / 空海 General，也没有改 Universal Bridge。

## 执行入口

G5 默认 `ExecutionScheduler(arbiter, 0, 0)`，零 interval 表示独立 actor 准入。`availableTokens=Integer.MAX_VALUE` 是旧接口兼容哨兵，不是新的实际额度。原有 `commandBudgetIntervalGameMs` / `executionBurst` 不再限制 G5；显式关闭 G5 的历史路径仍保持原来的有界门控。

保留：当前 session/player/frame、owner 和 generation/ABA、同一 observation 同一 actor 冲突、重复 Intent、有效原生命令复用、实际资金承诺、producer slots、军事槽位和 paid-before-visible / transport-unknown 保护。取消的仅是跨独立 actor 的人为操作预算。HTTP 传输仍顺序执行，原版最终 guard 仍权威；这不等于实现并发 native transport。

G4 force controllers 继续真正 collect → priority → dispatch，critical/current response 先行。施工投资、普通生产和普通 force 每轮各获服务，不再按剩余 token 轮转。经济、Strategy、生产仍是成熟调用路径内 immediate dispatch；没有宣称全 lane 全局批优先级。priority 真实裁决 force proposals 的竞争，不能追溯推翻已接受命令。

## 后帧资金效果

`NativeCreditWitness` 仅接收合法当前己方 `/state`，要求同 session/player、严格晚于 receipt frame、game time 不回滚。回执、经过时间、钱包下降或 queue empty 均不能解除未知承诺。

- 新的真实 production menu 可以提供比 `/state` 更新的 before-command queue/tier；必须与 Intent 的实际报价 observation ID 和 request path 完全相同。后续原生队列非空或真实升级 tier 增加才可结算。菜单更新不补回已经尝试过的 producer slot。
- 构建 receipt 的原生 `type/targetX/targetY` 只用于识别真实动作/guard 修正的 footprint，不是执行 witness。后帧必须看到对应 site envelope 内唯一的新己方产品，或同 ID 产品的 buildProgress 增加。
- Builder 建出施工点后可以变为原生 `repair`、去做下一项工作，甚至不再出现在当前快照；这些不否定已经出现的独立己方建筑效果。施工按 product/site 消歧，不能把同 Builder 的不同站点全部当成 queue lineage 歧义。
- 多个候选、重叠多个未结算施工 Intent、缺类型/坐标/进度仍 UNKNOWN，继续保留承诺。site spending effect 不声称 producer lineage、建筑 ready 或完成。

## Recovery

原有 `<25% HP → force:recovery`、严格后帧 `≥75% HP → FREE` 保留，转交继续 generation++。实际 HP 恢复是唯一出池依据。

首次撤离和覆盖仍在攻击的命令保持 critical MOVE。相同 generation、后帧明确相同 native MOVE 的命令可继续使用。有真正 ready 己方 repairbay 时选择实际最近站点；后续接近为普通 Recovery 优先级。没有修复湾时只安排一次回家等待，不能每八秒再次制造 critical MOVE。409 的当前负面 actor 证据缓存至己方位置/HP/order/generation 或实际恢复目标变化；不把 409、缺席或到达视为死亡/治疗。

修复施工复用 Strategy 的 Worker、native construction plan、paid construction 和已有 supportReserve。实际需求存在且当前原生安全合法站点可规划时，可以选定一个修复湾筹资；沿用已有收入预计 60 秒可筹得、90 秒到期释放的 support funding 窗口，而非新增固定现金门槛。该项目保护自己的真实报价，施工时不能把自己的 reserve 再扣一次。实际接受施工后，预算转入未执行资金承诺；需求消失、当前站点失效/不安全、超时/严重本土中断会释放。一次选择一个筹资项目，富余资金仍可立即独立施工多个合法站点。

没有闲置额外施工者时，可在没有成熟 BuildJob 的情况下短借 baseline Builder，真实 claim owner/generation。恢复施工/资金承诺结束后归还已有经济链；不能与已有建矿任务争同一 Builder。现有严重伤兵需求并非笼统阻止所有工厂升级或扩厂的条件。

## 推进与技术

- 普通混合坦克 roster 的 frontier 不再按精确单位 type 过滤；相同已知 movement domain 的成员都能恢复原 mission，native guard 仍逐实际 actor 校验。anchor 证据不是全体静态可达性证明。
- 已实际到达 waypoint 的正常成员不会只因 HP 低于健康计数阈值而永远阻止路点消费。撤退剩余小队经既有后帧 rally/平静/健康比例规则可退出 regroup，不永久等待重新补到六兵；六兵首次成军契约保持。
- ANTI_AIR 缺口继续产生能力采购需求；只有当前暴露已达到原有危险标准或实际交换已严重恶化，才让它阻止地面机会/促使撤离。少量无覆盖飞机不自动整队回家；不授权攻击不兼容空军，不使用失联位置。
- 当前原生升级报价实际可支付、保留真实选定支出与替补资金时，可以升级；不再等待整个军队停止 Recovery 或已有六个健康兵。升级完成仍看原生 tier，主力产品仍来自真实 T2 菜单。
- 当前合法矿点规划后，近处 Builder 同轮尝试施工；不再只记录 plan 然后等待下一次重规划。普通生产保持 R4 已有持续语义，armyTarget 只组织军队。

## 验证边界

确定性 focused 与原生 fixture 证明控制权、资源与实际命令/order/queue 效果，fixture 的 seed/clock/HP 明确不是自然战果。Spain 自然局从 commandCenter 零普通军队正常开始，未修改位置、现金、HP 或 fog；主证据与最终结果见 R5 handoff。

自然局采用 team0、原图 400×370、5×、difficulty0；真人失败局是 team5，二者不是受控 AB。改善了执行/恢复闭环不等于证明真人胜率、经济占图优势或重坦合理占比。完整 Capability Search、主动两栖长期编制及 SearchArea 继续后置。
