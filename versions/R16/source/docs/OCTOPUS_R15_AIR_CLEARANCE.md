# R15 Air 区域清剿连续性

起点：R14 `6637cdd`。不改 Ground General、Builder、Repair、经济和生产链。

## 真人根因

R14 真人 `20261012-001307-31880`，用户声明 Game 40:05.4 / Frame 94415 截图以后手动集中两栖、优先拆防空、清东岛后清东南岛。原生胜利在 43:50.157；这是人机混合胜利，不能计为自主胜利。具体命令源归属无法从每一条 native order 反推。

三条已溯源冲突：

1. 原侦察 `safeSegment` 对记忆防空射程加缓冲作硬绕行；安全观察站位无法覆盖目标周围 visibility 时，清剿永久保留但只有 `handoffNeeded`，没有执行闭环。
2. 一个点合法清空后立即恢复旧 Search 路线，可能反向飞离仍有已知建筑的岛区。
3. 清点后又被当成 idle search donor 拆队，已经集中的清剿力量重新零散化。

原始诊断：`G:\deepseek 工作台\_audit\r14-human-intervention-20261012\REPORT.md`。没有把所有 MOVE 都归为抢人，也没有把 clear 事件当击杀。

## 现有 controller 内的职责

仍使用独立 AirGeneral owner、逐单位 generation/实际 mode witness 和原生 attack guard，不增加新 General 类型或全局命令额度。

| 当前条件 | 行为 | 证据边界 |
|---|---|---|
| 有合法历史建筑记录，属于空军能力需求或当前陆地职责不足 | 先领取区域调查/清剿，再考虑泛搜索拆队 | 历史位置只给 MOVE 目的地 |
| 多支已有编队 | 优先不同的尚未被其他有效编队负责的区域；不足成军人数或已撤退的责任允许支援 | 同区多 owner 各自只能控制自己的成员 |
| 当前清剿 force 分散 | 向实际己方集群所在点集结；仍保留区域责任 | 名册数量不是集结证明 |
| 至少现有 `FORM_MINIMUM` 健康、ATTACHED、当前 physical AIR 成员在 `FORM_RADIUS` 内 | 允许受控 MOVE 接近旧点，突破普通侦察的记忆武器圈硬排除 | 不声明未知防御已消失或可轻松获胜 |
| 晚到成员 | 先向当前实际清剿编队汇聚；到位后加入 own HP 预算 | 不因远处名册人数虚增接近能力 |
| 重新合法见敌 | 复用现有 native engagement；优先当前对自身运动域有攻击能力的兼容目标 | 可见 packet、兼容性和 native guard 均继续有效 |
| own 接近预算损伤超出现有 `FightEstimate.MAX_EXPECTED_LOSS_FRACTION` | MOVE 退出，保留责任；恢复/增援后再评估 | 缺席按不可用保守处理，不记成死亡；敌情 UNKNOWN 不挡 own 损伤撤退 |
| 当前局部 visibility + 后续当前合法 empty packet | 清掉该点责任，优先继续其他合法已知建筑区域 | CLEARED != killed；局部 empty != 全图 clear |
| 没有剩余 eligible known duty | 恢复已有独立 Search 路线及自然分队能力 | 不宣布战争结束，胜利只认原生结果 |

普通单机侦察/小队的安全观察绕行不变。受控接近是有损伤反馈的调查行动，不是完整攻城交换模型；一次观察间隔内仍可能遭受较大损失，选用退点也不是原生路径安全证明。没有为支持手动控制而绕过 ownership；本轮修的是自主决策与清剿意图之间的冲突。

## Trace

- `air_general_clearance_continued`：真实历史来源状态、最后观测时间、区域位置、`memoryIsAttackPermission=false`。
- `air_general_clearance_probe`：已到位成员、起始 own HP、损伤预算、退出点；敌战力保持 `UNKNOWN_NOT_ZERO`。
- `air_general_clearance_retreat`：起始与当前可用 HP、原因、`missingActorIsKilled=false`。
- `air_general_clearance_complete`：没有剩余 eligible duty；`wholeMapClear=false`。
- MOVE reason：`CLEARANCE_ASSEMBLE_CURRENT_OWN_FORCE`、`CLEARANCE_JOIN_CURRENT_FORCE`、`CLEARANCE_APPROACH_CURRENT_VISIBILITY`、`CLEARANCE_WITHDRAW_TO_SAFE_POINT`。
- runtime 增加 `clearanceMission / clearanceProbe / clearanceRetreat`；不替代 mode/request/receipt/position witness。

## 测试边界

`R15AirClearanceHarness` 验证确定性生命周期、区域分配、集结后接近、敌情未知时 own 损伤撤退、恢复后重试。模拟位置变化只证明状态转换。

`R15AirNativeHarness` 用真实 1.15 对象、HTTP Bridge 与当前 controller，验证 LOST_CONTACT 调查 MOVE、fog attack 409、当前 AA 优先、原生队列及后帧 own order。显式设置 fog/位置、零自然 ticks，不证明自然发现或胜利。

自然测试固定 Spain、普通 `difficulty=0`、`--player 5`、5×、原版初始资金/位置。第 0 tick `native-bootstrap.json` 核对基地 `(7370,1950)`。默认玩家0旧路径保留；地图自动创建的8/9 AI 槽不删除。测试结果及候选 SHA 见 R15 handoff。
