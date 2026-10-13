# R13：施工机会、分队责任与持续导航

起点 `eef352f`，分支 `codex/octopus-g3-execution-20261002`。本轮沿用 General、Recovery、FREE/JOINING、经济与多 AirGeneral 主循环，不恢复全局人为操作额度。验收结果与局限见本轮 handoff；以下是代码契约，不是自然胜率结论。

## Builder：软旅程可以让位，真实施工不被覆盖

`PROSPECT / regional preference → 当前自身位置的 native mine quote → 精确矿点资金承诺 → build → 后续 own witness`。

- PROSPECT 的远端目的地是软意向。即使当前位置仍有 native MOVE，只要当前端点给出真实可施工的本地矿，就保留原 worker owner，将旅程让位给本地施工。不能把静态矿点、区域偏好或旧报价当作当前合法工地。
- 当前 native `build/repair` 先受保护；普通找矿和本地机会逻辑不覆盖已在执行的施工。
- 资金不足时保留真实报价金额。若旧 MOVE 继续前进会离开该报价原有的 native 工作范围，则取消旧远行，等待这笔具体施工资金；当前 idle/本地等待不反复刷 MOVE。
- 新 `readMinePlanStrategy` 把当前 `/expansion/plan` 的 HTTP 409 diagnostics 表达为 `no-current-work`。这是本次 native 拒绝，不包含价格、退款、建成或安全结论。其它旧 optional endpoint 的返回语义保持。
- 当前明确拒绝释放未付款矿预约；网络不可用/UNKNOWN 可以暂存旧预约，但不能拿旧 quote 重试发 build。只有本轮重新验证过的 quote 才可 dispatch。paid construction、unsettled credits、producer slots 不走这条未付款释放分支。
- 区域 backlog 也必须先验证返回值确为合法 quote，才读取报价坐标；`no-current-work` 没有坐标。此遗漏曾在本轮自然局触发空指针，已保留失败并用到区后拒绝工地的回归复现修复。致命异常现在保留 stderr stack，避免只有异常类名。
- 价格 provenance 仍为真实 `costSourceObservationId + costSourceRequestPath`，不以 `/state` 代替报价。最终 native command guard 再检查实际合法性。

## 远程 ThreatTask：分队承担责任，主队保留方向

`CURRENT contact → taskResponse → 必要的 FREE/借兵 → 新视野与当前威胁重新估算 → 归还原组织`。

- 分队使用现有 temporary response 和 owner generation 转移。优先附近 FREE，必要时借 General/JOINING 成员；每个 actor 仍只有一个有效 owner，reservation 不复制。
- 分配量依据当前合法已知需求。明确无武装建筑用最少的 native-compatible 清理者，不按建筑总 HP 拉走主力；武装需求仍使用现有粗粒度 HP allocation proxy，不能称作真实战斗交换预测。
- 新的当前接触可以增援。同一观测不会重复补兵，失联不凭想象增加兵力。整支 General 调向远方需要当前明确武装需求达到附近实际编制规模；借出的原成员仍计入该规模，避免小队借走后把主队变小而错误升级任务。
- 已有分队成员仍持有相同 owner/generation，但暂缺 own row 时，其容量是 UNKNOWN，不能按零容量再借一份。该分队等待 own 证据恢复后判断增援；其它有效当前成员继续响应。这是已承诺兵力的去重，不影响其它独立 controller 吞吐。
- 失联保存 task ID、最后合法区域及责任，撤掉当前目标攻击权限。分队只 MOVE 至最后已知区域，重新取得合法视野；没有 hidden attack、隐藏坐标刷新或 kill 推断。
- 清除要求当前区域视野、其后完整合法敌情样本，以及释放时区域仍当前可见；重新进雾使旧 clear witness 失效。`CLEAR != killed`。
- 完成时复用原 FREE / JOINING / General 回流语义。暂时缺失 own row 时，不能推断死亡，也不能丢掉仍持有 temporary owner 的控制器；合法 own row 恢复后再完成归还。

## Scout：Explore 保持所有权，路线节点提前接续

Explore 仍是同一个长期 task。当前 actor 的实际位置进入合法转弯格后，可在旧 waypoint 中心之前衔接下一段；未进入转弯格时不切过未知拐角。刷新 frontier 后，如果新 route anchor 已匹配当前 own tile，就在同一轮提交下一段，不人为等待下一轮。

owner/task/generation 保持。`receipt != arrival`；路线 cursor 的导航推进不是位置或覆盖证据。路由不可用、实际危险和 owner 丢失仍可停止行动。没有提高 decision frequency，没有用堆叠直线命令假装转弯连续。

同一 observation 内已用过 actor 的情况只是本地发令延期，不能作为 native COMMAND_REJECTED 释放 Scout owner。RECHECK 可以由另一名 actor 执行，期间 Scout 的有效路线继续；RECHECK 结束不清空仍在执行的独立 Explore 路线。测试模拟器必须在到达后提供从当前己方位置出发的新合法路线，重复旧 anchor 不代表产品应绕过路线合法性。

## AirGeneral：物理域威胁、实体攻击与区域清剿

- 对每个成员使用当前 native physical AIR/SUBMERGED witness；requested/desired mode、Dive/Fly receipt 不作为物理域。模式未知时保持 UNKNOWN。
- 当前合法敌人导出原版武器域 flags、射程和取样时间。不能对 AIR 开火的普通矿、工厂和地面炮塔，不因高 HP 或数量进入 AIR 危险总量。未知类型保持保守，不能假装无威胁。
- 在当前合法、己方武器兼容的目标中，优先真正能威胁自身运动域的目标；有可信武器 profile 时使用 DPS，再比较真实射程。不是完整战斗模拟器，也不是固定“AA 塔永远第一”。
- 新 `/command/attack-target?unitIds=…&targetId=…` 让目标选择落到原生实体攻击。1～48 个唯一、已完成、己方可作战 actor；原生再次检查 session、当前敌方合法 visibility 和逐 actor 武器域兼容性。重复 actor、隐藏目标或不兼容会被拒绝。
- `/state.orderTargetId` 只读己方当前原生订单的目标 ID；不由该引用读取隐藏目标坐标、HP 或武器。accepted receipt 不是伤害、击杀或清空证明。
- 已形成 `contactArea` 后，普通 SearchArea 刷新、编队拆分、绕行不抢走它。目标失联先取消 lingering native attack，改为合法区域 MOVE；无法安全继续时使用当前 own position hold，等待合法决策证据。
- 地区责任延续至后帧自身到位、当地当前视野和新的合法敌情共同支持清除。局部 CLEAR 不是全图清空，也不声称击杀。
- 转弯与 detour 延续原 route/cursor/mission endpoint，提前接续合法下一段。绕行 MOVE 不被当作 frontier 到达。天然帧间停顿、寻路和自然生存改善仍须长期实机观察。

## 大陆覆盖后的新增投资

复用 `MilitaryInvestmentPolicy` 的当前合法陆地需求、己方 ready ground strength 和未完成空海空间职责。它仍是粗分配 proxy，不是“大陆无敌人”的探测器。

当前地面需求已有覆盖且存在空海职责时：

- 空闲陆厂不再默认继续填普通地面队列；当前防空需求可保留例外，已付款/已在生产的队列不取消。
- 去掉不存在的地面补充需求对应的 ground runway、矿升级地面替换保留和 provider flow 扣减。
- 不再保留未付的多余陆厂技术发票，资金交回真实矿业、空海 provider 和既有资本投资链。

新的当前压力、己方损失/就绪不足或本土紧急需求会恢复地面生产。不是固定坦克数停机，不以 armyTarget 重新批准每一批；没有新增核弹生产，也没有承诺一定花完现金。

## 验证边界

Focused 控制器测试证明状态/所有权/调度契约；零 simulation ticks native fixture 证明原版字段、合法性 guard、真实 command queue 与 own order witness。它们都不能替代自然运动、施工完成、战斗交换或胜率。

Human Validation 继续默认从 Bridge 当前 `gameTimeMs/frame` 显示 Game/Frame，不由墙钟推算。自然 Spain、候选 JAR SHA、人工入口和未证明项统一记录在本轮 handoff。
