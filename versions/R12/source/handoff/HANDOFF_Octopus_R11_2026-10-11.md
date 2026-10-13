# R11 handoff：施工目标与路段分离、真实 batch 和在途接续

分支 `codex/octopus-g3-execution-20261002`，起点 R10 `d776f442215bb1b7e253c2bd67feff781eeda511`；产品 **`197310aca8afade588d68524aae7ec4439c338ff`**，文档 HEAD 见 Git。R11B 是可运行工程候选，**尚未真人验收，也未证明困难对战质量提高**。

## 输入和根因

只读冻结用户 R10E 困难反馈 run `20261011-010215-11528` 的 gzip.partial：SHA256 `4d4042a87d5c1cf0db7c970536822ddb30b90022d8c4c07c6756b295715841d9`，506094行、malformed 0、预期未闭合 gzip EOF。它是 prefix，不是完整终局。用户报告困难，启动记录未独立证明所选难度。末期完整 own 有20 Builder/5矿；1037 prospect order 与1034 waypoint arrival/observed 记录支持“短路线完成被当工作完成”的生命周期错位，不能把订单数当损失/击杀。

修正的共同执行原因：

1. **Builder 工作目标与 MOVE waypoint 混在一起。** 现在保留同一资源 tile/最终建设目标，路段到位后继续同目标；当前合法本地矿报价优先进入原有 bank/native guard。旧全局重新选矿只在真实无进展、失效或风险取消等已有条件下发生。
2. **新续行暴露了当前 G5 主循环的批次顺序。** Strategy.observe/updateWorkers 可以发 MOVE，但此前位于当前 beginExecutionObservation 之前，原型自然局续行被 `STALE_EXECUTION_BATCH` 拒绝。将 G5 当前 batch 建立移到 Strategy.observe 之前，一个 decision 只 begin 一次。这是本轮新续行暴露的缺陷，不能把原型拒绝计数回填成 R10 真人原始根因。
3. **Builder BFS 短转角与 progress 误判。** 保留原最多6步局部合法路线，选择经过格子/对角两侧都合法的最长 chord；不切未知地形和已知危险圆。只有实际 own 坐标移动更新 progress，accepted 或反复不同短 MOVE 不更新。near waypoint 不能绕过旧45秒无进展检查。新危险无法绕行时取消旧危险 leg，取消点不被宣称安全。
4. **同级矿报价互相等待。** 单矿执行时扣除其他受保护用途，但不要求整个 mine bank 同时筹齐；实时有效现金能负担的第一矿先落实，实际 accepted 后使用既有 unsettled credit。普通连续生产仍保护到矿真实报价，不计时退款。
5. **小威胁分类误将 building 标签当整队战略任务。** 当前明确 `canAttack=false` 的孤立建筑进入既有小响应预算，附近 FREE/JOINING/少量借兵及 origin 恢复复用 R10。未知/武装建筑、基地、超出原小响应包络的集群仍战略分配；未将所有远方单炮塔一律降级。
6. **各 controller 等旧目的地完成。** Scout 使用后帧实际在途直线路径 witness 滚动续行，当前合法危险可即时改向；临时 recheck 不冻结原 EXPLORE。Ground 用 centroid 附近真实成员作 anchor，靠近旧 leg 即可规划下段；75%真实到位才完成旧 waypoint。无 unseen frontier 时用既有合法 recon planner 做 deep/stale fog patrol。Air 在本队 matching native MOVE 的严格后帧安全路段上 lookahead，普通 Surface 目标有真实 Ground 执行时让出并恢复原搜索。

后帧 Ground claim 要求当前真实 native advance，原生 idle `orderType:null` 不能靠旧 receipt 一直占目标。AIR/SUBMERGED/touchingWater/跨域 Capability Need 保持 Air 职责。缺失或过期 combat 是 UNKNOWN，不当 clear。

## 实现与保护

| 提交 | 内容 |
| --- | --- |
| `1eae27d` | 施工目标持久、Commander 小建筑响应、Scout/Ground/Air 在途连续性、5个 focused suites |
| `fe790f3` | 原生独立撤退 fixture 对齐“小建筑少量 detach + 原 General 余部继续”的合法输出 |
| `3d829fa` | 自然局复现的 Builder 短转角活锁、near timeout 与合法 chord 修正 |
| `197310a` | G5 当前 execution observation 顺序、真实位置 progress、危险旧 leg 取消、native-null Ground claim 负例 |

没有提高 decision frequency 或恢复跨独立 actor 的人为 APM 配额。force controllers 仍真 collect→priority→dispatch；成熟 Strategy/经济仍 immediate dispatch，未声称全 runtime 全局排序。owner/generation/ABA、同 actor 冲突、有效命令复用、当前报价、credits/slots/burst、paid ghost 保持。receipt 不当位移、到位、花费或建成。

契约：[OCTOPUS_R11_MOTION_AND_WORK.md](../docs/OCTOPUS_R11_MOTION_AND_WORK.md)。主要源码：`BattleClient`、`StrategyDirector`、`ScoutBridge`、`CommanderDirector`、`GeneralCombatDirector`、`AirGeneralController`。

## 最终验证

候选 JAR SHA256：`798b88778921cda6fc0e39fc3546a564286dfab2d41f7bb6b952a2ae9dedb832`。证据根 `G:\deepseek 工作台\_validation\octopus-r11-20261011`；正式结果见 [validation-summary.json](../evidence/octopus-r11-2026-10-11/validation-summary.json)。

新增 focused：Frontline18、General10、Air82、ScoutMotion42、ScoutBridgeSafety8，合计 **160** checks。另相关 R10 funding16/temporary135、G51 prospect92、R8 frontier8通过。最终13阶段 native exit0，覆盖 production168、mode30、lane31、独立执行69、G51行动311、ScoutBridge26、G6链42及Spain第三地形/超时等。均明确 fixture/`simulationTicks=0`，实际 native 对象与 HTTP receipt，不代替自然运动/战斗/胜利。

最终同源码 Windows **119/119 PASS，84 Java harnesses、32 Python suites/506 tests、3 build，failed steps: 0、exit0**。277输入/1895 kit文件无漂移，210 JAR内容 entry 核对，测试独立构建与候选/交付完整 SHA 相同；不是用中间源码回归冒充最终产物。中间 native 测试旧断言失败及两个自然诊断停止均保留，不删除证据。原型1/2的1573/388 Strategy stale-batch、0 accepted旅程续行，仅用于定位新续行顺序问题；它们由根代理停止自己的隔离引擎，不是自然胜负或用户桌面崩溃。

## 困难 Spain 自然局：真实执行恢复，比赛仍败

最终同 SHA、原 Spain MP97地图/player0、5×、原版 **difficulty1=Hard**，4800秒预算，标准零普通军队，无资金/HP/位置/fog注入。原版结果屏和最终 Bridge 为 **DEFEAT**，gameTime `4136592ms`（68:56.592）。Agent exit0、原报告 summary `outcome=PASS / matchOutcome=DEFEAT`：PASS 只是正常运行结束，**不是获胜或策略通过**。

raw SHA256 `0f951ed6b3698cd6142aab3f74d51f3f84839ceb4e709c9a34e2790429b014bd`，394441行、malformed0、gzip闭合。真实行为：150条同矿点旅程续行、24笔 native build-extractor accepted、109条 Ground 后帧提前接续转换。没有 `STALE_EXECUTION_BATCH` 拒绝。记录是事件/receipt次数，不是150个矿点或24座建成矿；本局始终未建立稳定经济优势，不能称 Builder 罢工全部解决或困难战力已提高。

首Scout601在完整own采样里62.320～92.384s可见，距初次观测最大982.228world；后期另有Scout采样。只能证明局部实际位移，不能由缺席宣称死亡，也未证明长局续航。Air本局没有 search lookahead 或 Ground-duty-transfer事件，规模/条件未自然闭环；新Air语义目前只由 focused/native局部链证明，**自然效果 NEEDS_EVIDENCE**。

runtime记录窗口间隔 median1264ms、p951952ms、max3984ms game time，不是所有decision/单位命令间隔；当前样本不是 R9 千单位规模，不能称晚期性能已解决。本局解析未结账0不代表历史胜局付款UNKNOWN已全解决。不同难度、玩家位、随机种子的局不能用于受控前后胜率比较。

## 人工入口与下一断点

仅准备新 kit：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R11B`，port **47697**、上面同 SHA。用户 `1-Start-Game.bat`→自己选择Spain/难度/速度/开始→`2-Run-Agent-7200.bat`，另1200/2400。控制台标题与 `runs/<run-id>/rw-agent-live.html` 顶部默认直读 Bridge Game/Frame；7200入口 wall safety14400，不由wall估时。

[人工说明](../docs/OCTOPUS_R11_HUMAN_VALIDATION.md)。旧中间 R11 kit 已标注更替，不能交叉归因。没有push/deploy/启停或操作用户桌面游戏，原版设置/存档/回放未改。用户launcher修改/backup三文件原哈希保留，未stage，整体有意dirty；不reset/clean。

下一轮先看用户同条件真人真实矿施工延迟、主力小威胁分兵后继续推进、Scout长期探索/绕行/复出、Air实际在途接续与职责避让。当前困难自然败局说明前期生存与建设前沿仍需要事实诊断，不要把回归绿灯当对战质量验收。付款历史消歧、千单位真实耗时仍保留后续；不新增核弹、T2廉价空Scout，不推倒成熟重坦/Repair/经济/AIR主体。
