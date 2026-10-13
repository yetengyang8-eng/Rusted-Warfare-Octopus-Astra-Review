# 构建与验证

固定依赖：原版 PC 1.15 Build #28 / Game Code 176。
`game-lib.jar` SHA-256 必须为 `8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`。
原版游戏依赖不随本包分发。

需要带编译模块的 JDK（本次 Linux OpenJDK 17）；产物使用 `--release 8`。

```bash
bash developer/build.sh /absolute/path/to/game-lib.jar
bash developer/test.sh /absolute/path/to/game-lib.jar /absolute/path/to/libs
```

第二条还需 Python 3（仅开发测试使用，用户运行 BAT 不需要）。产物为 `developer/dist/rw-agent-bootstrap.jar`；验证后复制到根目录交付。

- SmokeHarness：引擎外壳、HTTP、游戏线程任务入口。
- BridgeHarness：原版地图尺寸、单位过滤、移动命令、对局隔离与超时清理；开启 39 项、关闭命令 6 项。
- EconomyHarness：每种类型路径 34 项原版动作、占地/视野、资金、命令、去重和生产检查。执行真正生产命令至工厂队列，并核对 350 扣款。分别覆盖无替换 tank 与原版内置替换 c_tank；使用不启动图形/寻路线程的地形夹具；未推进完整世界模拟。
- test_economy.py：12 项独立 HTTP 时间线测试，含成功、未完工、旧单位排除、死亡、换局、503 不重试和干扰拒绝。
- test_client.py：11 项移动客户端回归（部分含多组子用例）。

完整建造计时、寻路到位和实际出兵须 Windows 实机验收。本包 docs/test-results.txt 是本次全套命令实际输出。


0.03-alpha2 OpeningHarness 双路径：旧 extractor 61 项、原版内置 extractorT1 62 项（各包含 c_tank 经济检查34项）；使用实际 custom.l 元数据和 custom.j 预览构造器，核对预览不进入世界表、实际占地、动作类型、占用/视野和原生命令。元数据按随原版资源发布的 name、price、techLevel、footprint、overrideAndReplace 等相关字段初始化，未启动完整资源加载器。test_opening.py 为8项，默认使用 extractorT1，另有旧 extractor 回退；其余12项经济和11项移动测试保留。

回归证据 docs/regression-opening-alpha1.txt 用同一替换夹具加载旧 JAR，在 /opening/plan 复现409；这是预期失败证据，不是本版测试失败。


0.04新增：ProductionPlanHarness在34项经济检查后增加10项只读已有工厂规划验证（包含无资金、忙碌/未完成/非己方、网络、参数和无副作用），合计44。test_development.py覆盖默认8兵3矿重叠执行、待扣款矿点资金预留、矿点耗尽、仅生产、缺厂零下令、未知结果不重试、换局、队列干扰、类型变化、旧矿排除及完成资产死亡。测试是独立HTTP时间线，不代替真实游戏寻路。

## 0.05 接续说明

当前入口文档为根目录 START_HERE_CN.md 和 docs/DELIVERY_CN.md，上方历史开发说明用于解释既有映射。

Linux 构建及验证：

```bash
bash developer/build.sh /path/to/game-lib.jar
bash developer/test.sh /path/to/game-lib.jar /path/to/libs
```

Windows 可在配置 JDK 17 的 JAVA_HOME 后运行 `developer\build.bat "游戏目录\game-lib.jar"`。构建输出 `developer/dist/rw-agent-bootstrap.jar`，复制到游戏根目录后重启 Agent。Windows BAT 在当前 Linux 环境没有执行验证。

新增源码：EconomyBridge 中的 diagnostics/preflight，client/PreflightClient 与 ReportFiles，headless/HeadlessRunner。新增工具：assemble_game.py、analyze_reports.py、run_headless.py。无画面入口编译进同一个 JAR，桌面模式不会主动启动它。

原版对象夹具用于接口回归，不能代替完整游戏模拟。无画面实机证据放在 docs/validation-0.05/native/，使用未修改的原版 JAR、TMX 和内置单位定义。不要使用测试夹具的 Unsafe 初始化方式做真实训练环境。

## Windows 开发回归

Linux 用 `test.sh`，Windows 用测试集合完全相同的 `test-win.ps1`（不要只维护其中一套）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File developer\test-win.ps1 <game-lib.jar> <libs 目录>
```

- 含非 ASCII 字符的路径下不要用 `build.bat`：它用 `for /r ... echo "%%F"` 生成 `@argfile`，cmd 与 javac 读取该文件所用编码不一致，会把路径里的反斜杠吃掉。`test-win.ps1` 直接把源文件列表传给 javac，编译与打包参数和 `build.bat` 一致。
- 9 个 Python 套件按 UTF-8 解码子 JVM 的 stdout；脚本为子 JVM 固定 `-Dfile.encoding=UTF-8`，否则中文 Windows 上会以解码错误的形式误报失败。
- `ReportCommitHarness` 中「移动一个仍被打开的文件」是 POSIX 专有模拟（Windows 以共享冲突拒绝），在 Windows 上显式跳过并打印 SKIP，其余封存校验照常执行。产品路径 `ReportFiles.finish()` 在改名前后均未持有句柄，两个平台都安全。
- 交付版 JAR 由 Linux OpenJDK 17 构建；本机重建后 54 个归档项中除 `MANIFEST.MF` 的 `Created-By` 外逐字节一致，因此该回归流程可信。

## P2-A / P2-B 经济与产能扩展（客户端策略）

同一局内只推进一个变量：先补矿点（P2-A），矿点达标后才补产能（P2-B）。两者都不改动原版桥接 API。

- 建造任务统一为 `BuildJob`（`kind=extractor|landFactory`），矿点事件名保持 P2-A 原样（`economy_expansion_*`），工厂事件为 `production_facility_*`。
- 日志规则不变：`extractor_started/completed`（工厂为 `factory_started/completed`）只在建筑**真正完成**时成对写出，真实首见时间放在 `*_observed` 与 payload 的 `firstSeenGameMs`，避免 PASS 报告被判为 INCOMPLETE。
- P2-B 触发条件（经济触发，非固定时间/资金阈值）：矿点已达 `rwagent.mineTarget`，且现有工厂队列非空（真正饱和），且 `credits >= factoryCost + preferredUnitCost`。`factoryCost` 取自 `/expansion/plan` 或 `/economy/plan` 的真实报价，`preferredUnit(Cost)` 取 `/combat/production` 中该工厂按现有评分规则真正会造的下一个单位（即使队列非空也采集，否则永远拿不到价格）。
- 后方建造：建造者离家超过 `rwagent.rearFactoryRadiusWorld`（默认 400）时先回家，再由 `/economy/plan` 在半径 100/140/180/220 内选址，第二个工厂不会落在前线。
- 预留规则沿用 P2-A：工厂待建期间 `wouldBreachMineReserve` 会阻止出兵把预留资金花掉。
- `production_surplus` 只做诊断、不下令：队列非空且资金已够「一座工厂 + 一个当前单位」时每 10 游戏秒记一次，字段 `credits/completedMines/completedFactories/factoryQueueNonEmpty/preferredUnit/preferredUnitCost/factories/gameTimeMs`。它是「第二个工厂」这一决策的证据，而不是猜测的阈值。
- 新增可调项：`rwagent.landFactoryTarget`（默认 2）、`rwagent.rearFactoryRadiusWorld`（默认 400）。硬上限 32、第三座工厂、兵种评分、进攻策略与维修均未改动。
- 覆盖测试：`tests/test_battle_client.py:ProductionFacilityTests` 5 项（饱和即建第二座并且只建一座、surplus 证据字段、队列空闲不重复建厂、建造者先回后方、builder 丢失只上报一次）。

## 生产调度取证（输出7 §2/§3，只取证未改行为）

`BattleClient.produce()` 在「第二座厂」出现后的真实行为，逐条对应《输出7》的问题：

1. **遍历顺序** = `/combat/production` 返回的 `factories` 数组顺序 = 引擎单位表顺序（≈ 创建顺序）。实测日志里老厂恒在前面（第4局 662 次 `(5,642)`、局A 219 次 `(5,623)`，从未反序）。
2. **T1/T2 谁先得到机会**：老厂（id 5，已升级 T2）排在前面，但它在约 95% 的 tick 里 `queue != 0` 而被跳过（第176行），于是**那一 tick 的生产机会落到新建的 T1 厂**。
3. **每次 `produce()` 最多只下一张单**（第204行成功下单即 `return true`），所以两厂不是并行下单，而是共享同一个 1 条/tick 的配额。
4. **评分**：`c_tank`/`tank` = 1、`heavyTank` = 3、`upgrade` = 5；`affordable` 是硬过滤（第187行）。
5. **买不起 heavyTank 会立刻退化为 c_tank**：没有为高价值单位预留资金的逻辑（第188行）。
6. **T1 厂买 c_tank 会挤压 T2 厂的重坦预算**：两厂共用同一资金池，没有任何全局预算/主力兵种目标/工厂优先级。
7. **`BANKING_FOR_UPGRADE` 的对象是该厂自己的升级**：仅当 `army>=10` 且该厂菜单里仍有 upgrade 时，跳过该厂（第197~200行），是逐厂判断而非全局。
8. **结论：每厂局部贪心，没有全局目标。**

现有日志已经能回答「哪座厂造了什么」：`command_result` 的 `unitId`+`type`（下单）与 `production_queue_finished` 的 `factoryId`+`type`（完成）都是逐厂记录，`production_menu` 逐 tick 给出每厂 `tier/queue/actions[].affordable`。因此**不需要新增诊断事件**就能证明资金争抢；只有「某厂想买但买不起」的逐 tick 记录目前缺失，可用 `production_blocked` 补（尚未实现）。

实测证据（同一资金池、同一评分）：

| 样本 | 厂（tier） | 兵种构成 | cheap 坦克下单 | 下单时 credits 中位 |
| --- | --- | --- | ---: | ---: |
| P2-A #4 | 5(T2) | heavyTank 54 / c_tank 1 | 1 | 758 |
| 第1局 | 5(T2) | heavyTank 26 / c_tank 0 | 0 | — |
| 第2局 | 5(T2) | heavyTank 23 / c_tank 3 | 3 | 720 |
| 局A | 5(T2)+623(**T1**) | heavyTank 18 / c_tank 17 | 18 | 495 |
| 第3局 | 5(T2)+548(**T1**) | heavyTank 13 / c_tank 1 | 1 | 756 |
| 第4局困难AI | 5(T2)+642(**T1**) | heavyTank 17 / **c_tank 103** | 104 | 412 |

单厂局几乎全产重坦（54/55、26/26、23/26），双厂局长局（第4局 900 秒）**86% 变成便宜坦克**，且第二座厂**始终停在 T1**（三个样本都只有 `[1]`）。「heavyTank 买不起但 c_tank 买得起」的菜单状态出现频率：单厂 #4 = 40/821（5%），第4局双厂 = 342/455（**75%**）。

**原生事实（输出7 §8）**：`assets\units\tanks\tank.ini`（`name: c_tank`，`overrideAndReplace: tank`，price 350，techLevel 1）第 52~56 行明确写着

```ini
[attack]
canAttack: true
canAttackFlyingUnits: false
canAttackLandUnits:   true
canAttackUnderwaterUnits: false
```

即 **c_tank 在定义层面就不能攻击空中单位**。`heavyTank` 在本安装中没有对应的 ini（属编译进 game-lib 的原生单位），本次未能从静态数据确认其是否可对空——需要一次只读的能力探针才能定论，我没有猜。

## Pending build 语义（输出7 §10）

- 只有 `buildProgress >= 1.0` 的建筑才计入 `mineTarget` / `landFactoryTarget`；半成品不再是「已完成的矿」。心跳同时给出 `mines`/`minesReady`、`factories`/`factoriesReady`、`buildPending`、`buildStalled`。
- 建造者死亡时按物理状态分两种：
  - 已经有半成品工地 → 状态 `ACTIVE → STALLED_NO_BUILDER`，**只上报一次** `*_state`，保留 `candidateId / site / buildProgress`，不再重规划也不再刷屏；建造者再次出现时报回 `ACTIVE`（P2-C1 的续建挂点）。
  - 尚未开工（没有工地）→ `ABANDONED_NO_BUILDER`，丢弃任务并按间隔上报 `NO_BUILDER`。
- 覆盖测试：`tests/test_battle_client.py:EconomyExpansionTests.test_a_half_built_mine_does_not_satisfy_the_target`、`ProductionFacilityTests.test_an_unfinished_site_is_parked_when_the_builder_dies`、`...test_a_lost_builder_abandons_a_plan_that_never_started`（共 16 项）。

## P2-C1 建造者恢复（输出8 §2 / 输出9 §2-§11）

原版事实（`CapabilityHarness` + `BuilderHarness` 实测）：**指挥中心**通过原生动作 `ActionId(u_builder)`、`type=builder`、**cost=500** 生产建造者，该动作属于原生 FACTORY 动作族，因此工厂队列语义（`queueCount`）直接适用，不需要另造队列系统。同一夹具还读到建造者可建：`turret 500`、`antiAirTurret 600`、`airFactory 1000`、`laserDefence 1200`、`repairbay 1500`、`fabricator 1500`。

新增桥接（专用、最小，不动 `/combat/production` 语义）：

```text
GET  /economy/builder-production
     producerId / producerType / queueCount / buildersQueued / builderActionId / builderActionClass /
     builderActionAvailable / builderCost / builderType / availableCredits / existingBuilders / builderOrderPending
POST /command/produce-builder?unitId=&sessionId=&requestId=
     与既有命令一致的 ownership / session / requestId 去重 / allowlist；
     已有建造者或已在产建造者一律 409；builderCost 全程取自原生 action，不硬编码。
```

客户端（`BattleClient.builderRecovery`，全局优先级 1）：

```text
aliveBuilders == 0 且无在产        -> builder_recovery_started
GET /economy/builder-production    -> builderCost 记入 builderReserve（优先于一切战斗生产）
生产者队列非空                     -> builder_recovery_waiting: PRODUCER_BUSY（不抢队、不取消）
credits < builderCost              -> builder_recovery_waiting: INSUFFICIENT_CREDITS
动作不可用 / 无生产者              -> builder_recovery_waiting: ACTION_UNAVAILABLE
队列空闲且钱够                     -> POST -> builder_recovery_ordered（builderOrderPending=true）
新建造者出现                       -> builder_recovery_completed（释放 reserve）
```

- 战斗生产在做任何购买前都要尊重 `builderReserve`（`wouldBreachMineReserve` 现在同时计入建造者预留与在建工地预留）；被预留挡下的购买写入节流事件 `production_deferred`（`RESERVED_FOR_BUILDER_RECOVERY`，同厂同因 10 游戏秒最多一条），与「钱不够」的 `production_decision: PREFERRED_UNAFFORDABLE_FALLBACK` 明确区分。
- 心跳新增 `aliveBuilders / builderRecovery / builderOrderPending / builderReserve / builderCost / builderProducerId`；汇总新增 `builderTarget / builderRecoveries / builderOrders`。
- 半成品：`STALLED_NO_BUILDER` 状态与工地信息（`candidateId/site/buildProgress`）保留；**新的建造者能否续建半成品是 P2-C1 的验收项**——原生若支持则续建，不支持则显式 `ABANDONED_UNRECOVERABLE` 后重新规划，绝不允许假 `ACTIVE`。
- 回归：`BuilderHarness`（原生端点，含 500 扣款实证）、`test_battle_client.py:BuilderRecoveryTests`（恢复闭环、预留、`production_deferred`）。

## P2-C1-B 半成品恢复探测（输出10 §1-§8）

`STALLED_NO_BUILDER` 的工地不再只是「停在那里」：

```text
STALLED_NO_BUILDER（保留 candidateId / site / buildProgress）
  ↓ 新 builder 出现且该 job 从未探测过
RECOVERY_PROBE：对同一 site 重新下达同名 build 命令（每 job 仅一次）
  ↓ 200                                ↓ 409
RECOVERY_PROBE_ACCEPTED                RECOVERY_PROBE_REJECTED
  ↓ 同一 candidateId 且 buildProgress 严格上涨     ↓ 只读查 builder 的原生动作清单
ACTIVE + build_recovery_resumed                   （`GET /economy/builder-actions`）
                                                    ↓ 若确实没有续建路径
                                                  ABANDONED_UNRECOVERABLE
                                                  + blockedSites 记录（planner 不再选同一 tile）
                                                  + 重新规划
```

- **接受命令不等于恢复**：只有 `buildProgress` 在同一个 `candidateId` 上真实上涨才写 `ACTIVE`；超时或「同位置冒出第二个结构」都按失败处理。
- **续建不得重复收费（输出11 §2/§3）**：探针前后记录 `creditsBeforeProbe / creditsAfterProbe / probeCostDelta / buildProgressBefore / buildProgressAfter / buildCost`；若在建造成本 90% 以上被再次扣除，则判定 `DOUBLE_CHARGED_BUILD_COST`，**不作为恢复路径**，直接走 `ABANDONED_UNRECOVERABLE → blockedSites → replan`。
- **不假 ACTIVE、不永久 stalled、不同 site 死循环**：三条都被测试锁住（`test_accepted_probe_only_recovers_after_real_progress`、`test_rejected_probe_abandons_the_site_and_blocks_it`、`test_a_second_full_charge_is_not_accepted_as_recovery`）。
- `blockedSites` 只在本局有效：只要该 site 不再存在**未完工**的结构（消失、被拆或极少见的完工）就立即解除封锁（输出11 §6）。
- 只读端点：`GET /economy/builder-actions?unitId=`（该单位原生动作清单，含 `hasRecoveryPath` / `recoveryPath`，当前唯一取值 `SAME_BUILD_COMMAND`）、`GET /combat/capabilities?types=`（现役实例的布尔字段 + 原生价格，帧同步可见性，用于 heavyTank 对空的实机差分；字段识别需两个正交锚点：c_tank 与 AA turret）。
- 原生事实：建造者共 13 个动作，其中没有 repair/assist 类动作，只有各建筑的同名 build 动作（`b_extractor 700`、`b_turret 500`、`b_antiAirTurret 600`、`b_landFactory 700`、`b_airFactory 1000`…），因此「续建半成品」只能靠重新下达同名 build 命令这条路。

## 采集工具：按 session 自动分卷（本次只改工具，不改 Agent）

`tools/collect_reports.py` 不再因为「报告总量超过 100 MiB」而拒绝采集，而是按 session 自动分卷：

- **先按 sessionId 分组，再分卷**：同一局（session）的 `economy / development / battle` 永远在同一个 ZIP 里，绝不拆开；
- 排序：session（按 sessionId，再按最早 wallTime）→ 卷内报告按 wallTime → phase → 文件名；
- 每卷默认 **85 MiB（raw 报告字节，即旧检查的口径）**，`--target-mib` 可调；另外硬性校验产出的 ZIP 本身 ≤ `--hard-cap-mib`（默认 100 MiB）；
- **单个 session 超过目标 ⇒ 单独成卷并在文件名与 metadata 里标 `oversize`**；若连硬上限也超过，工具会明确报错指出是哪个 session，而不是静默丢弃数据；
- 每个卷仍然带完整 provenance：`metadata.json`（含 `agentJarSha256`、`bundle` 块：卷号/总卷数/session 列表/phase/时间范围/rawBytes/oversize）、`reports.json`、`reports.md`、`SHA256SUMS.txt`；
- 额外在输出目录生成 **`bundle_index.json` + `bundle_index.csv`**（总账）：bundle 名、sessionId、report 文件、phase、wallTimeMs/Utc、字节数、是否 oversize，并列出目录里已有的旧 bundle；
- **只读采集**：不修改、不删除任何原始 report（`discover()` 只做遍历与校验），产物全部是新 ZIP。

```powershell
REM 直接双击 RW-Agent-Collect-Reports.bat 即可；需要更大卷时：
RW-Agent-Collect-Reports.bat --target-mib 120 --hard-cap-mib 150
```

覆盖测试：`tests/test_frontier_reports.py` 新增 4 项（同 session 不跨卷、oversize 单独成卷并标记、总账逐报告登记、每卷不超目标且 session 不重复出现）。

## P2-B2 全局生产预算（输出13 §3-§9）

优先级（`BUILDER_RECOVERY` 复用 P2-C1，未改动）：

```text
1. BUILDER_RECOVERY              —— P2-C1 已有，builderReserve
2. PRIMARY COMBAT / PRIMARY TECH —— 主厂的主力单位与解锁它的科技
3. SECONDARY INVESTMENT          —— 副厂升级
4. SECONDARY CHEAP PRODUCTION    —— 副厂廉价生产
```

- **主厂不按 factoryId / 创建顺序硬编码**：mainline = 任一生产者能提供的最高评分单位动作（当前即 `heavyTank`，评分 3），主厂 = 首个能提供该动作的生产者；价格与动作全部取自原生 menu。
- **主厂不降级**：主厂想要的主力单位买不起时**不买便宜单位**，改为 `production_deferred`（`RESERVED_FOR_PRIMARY_PRODUCTION`，仅当偏好动作真正变化才重新判断）；`production_decision: PREFERRED_UNAFFORDABLE_FALLBACK` 只可能出现在副厂。
- **副厂只花盈余**：`primaryReserve = preferredMainlineCost`，且仅在**主厂空闲且买不起主力单位**时持有（主厂已在生产时不再空持，否则副厂会永久饿死）；副厂的任何购买都要求 `credits - cost >= primaryReserve`。
- **副厂升级是机会性盈余投资，不是长期攒钱目标**（输出15 §2/§3）：副厂 upgrade 只有在「当前可支付」且「支付后不侵犯 primaryReserve」时才执行；否则记 `SECONDARY_INVESTMENT_DEFERRED` 后**继续检查 lower-cost legal action**——若 `credits - chosenCost >= primaryReserve` 成立则下单并记 `SECONDARY_INVESTMENT_FALLBACK`（新事件，字段 `factoryId/factoryTier/credits/deferredUpgradeCost/chosenUnit/chosenCost/primaryReserve/reason/gameTimeMs`），不成立才 `BANKING_FOR_SECONDARY_UPGRADE` 并放弃该厂本 tick。
- **两种 upgrade 区分**（输出13 §7）：`PRIMARY_TECH_BANKING`（尚无高级生产者、升级用于解锁主力单位）与 `SECONDARY_INVESTMENT_DEFERRED`（已有主厂，副厂升级只能花盈余）；输出15 §4 的边界保持不变：`PRIMARY_TECH_BANKING` 与 `RESERVED_FOR_PRIMARY_PRODUCTION` 都不允许 cheap fallback。
- `production_deferred` 现在可区分四类资金情形：`RESERVED_FOR_BUILDER_RECOVERY`、`RESERVED_FOR_PRIMARY_PRODUCTION`、`PRIMARY_TECH_BANKING`、`SECONDARY_INVESTMENT_DEFERRED`（同厂同因 10 游戏秒最多一条）。
- **副厂 fallback 是行为事件，副厂 deferral 是状态事件**（输出18 §D）：`reportSecondaryInvestment(...)` 只写被 10 游戏秒节流的状态事件 `SECONDARY_INVESTMENT_DEFERRED`，并登记 `pendingFallback`；`SECONDARY_INVESTMENT_FALLBACK` 只在 `/command/queue` **被桥接接受之后**写一条，被拒绝（409）则 0 条。因此 `成功的 fallback 下单数 == fallback 事件数`，不需要 reader 端去重，也没有节流漏洞（输出16 §2B 修的是一次决策两条事件的重复；输出18 §D 修的是节流导致的漏报）。菜单里没有 upgrade 的工厂（例如已 T2）不写任何 upgrade 相关事件。
- **评分体系未改**（1/3/5 不变），只改变「动作是否被允许消费当前 credits」。
- **移动单位上限的当前开发默认值是 40**（`-Drwagent.mobileUnitHardCap`，下限 `activeArmyTarget`；输出19 §2 裁决）。它来自巨岛 32→40 的 A/B：`army≥32` 心跳占比 22.7% → **61.4%**，同期 `lost` 64 → **29**。**但 40 不是已验证的最优值**：那只是一对随机样本，不能当作精确的因果效应；而且 cap=40 时 credits 仍大量滞留（median 5210 / max 10754），说明 40 并没有解决经济投资问题。暂时不测 48、不做动态 cap；`-Drwagent.mobileUnitHardCap=32` 保留用于 32/40 回归对照（冒烟环境提供 `RW-Agent-Match-Cap32.bat`）。
- 只读诊断（不改决策）：`capability_snapshot`（战斗中第 60/600 游戏秒各调一次 `/combat/capabilities`，双锚点映射对空能力；事件新增 `trigger` 字段，取值 `SCHEDULED` / `FIRST_VISIBLE_AIRCRAFT`）、`FIRST_VISIBLE_AIRCRAFT` 探针（输出16 §8：正常合法视野里首次出现 `c_interceptor`/`lightGunship`/`c_helicopter`/`gunShip`/`gunship`/`heavyInterceptor` 时额外采一次，仅在已收到的观测数据上判断，绝不为此改变 scout 行为）、`battle_config` 增加 `builderTarget`、报告头增加 `report_provenance`（实际加载的 `agentJarSha256`）、汇总增加 `gameSecondsPerWallSecond`（实测速度，x5 可由报告直接识别）。
- 覆盖测试：`tests/test_battle_client.py:ProductionBudgetTests` 16 项（主厂不降级且资金被保护、副厂仅在主厂忙时可花、主厂买得起时下单、副厂升级等盈余、主厂科技攒钱被单独标记，输出15 §6 的 5 项，T2 副厂无 upgrade 菜单的幻影事件回归，以及输出18 §D 的三项：3 次快速成功下单 → 3 条事件、被拒绝的下单 → 0 条事件、fallback 事件数 == 成功下单数），另有 `ProductionLadderTests` 验证 cap 默认值 40 与 `-Drwagent.mobileUnitHardCap` override。

## 可达性原始诊断（输出21/22/23，diagnostics-only）

- 只读端点 `GET /combat/reachability?unitId=&targetId=[&dump=full]`：只回**原始引擎结果**（`engineAqAResult` / `engineAqBResult` / `outerCandidates`（每个变体给出三组 raw：`legacyGuessArgs=[0,0,0]` 标注 `ARG_SEMANTICS_OLD_GUESS`、`enginePatternMode0Args=[80,0,1]`、`enginePatternMode3Args=[80,3,1]`）/ `exactTargetPathRawResult` / `rawPassabilitySamples` / `weaponRangeRaw` / `collisionRadiusRaw`（来源字段 `cj`）/ `targetRadiusRaw` / `targetFootprintRaw`；`dump=full` 时附 `fullFloatDump`（攻击者与目标的实例 + metadata 全量 float 原始值）），`diagnosticStatus` 固定 `RAW_ONLY_NOT_A_VERDICT`；响应体里**不得出现** `reachable` / `engageable` / `canAttack` / `reachableEngagementPosition` / `engagementRadius` / `effectiveTargetRadius`（`ReachabilityHarness` 会扫描并断言）。
- 移动类别来源标注：`attackerMovementClassSource` / `targetMovementClassSource` 取值 `y.h()`（引擎自身访问器，首选）或 `FIELD_SCAN_FALLBACK`。
- Agent 侧采样由 `-Drwagent.reachabilitySample` 控制，**默认开**；`-Drwagent.reachabilitySampleIntervalMs`（默认 30000）与 `-Drwagent.reachabilityTypeLimit`（默认 3）限流；目标绑定三级：`DIRECT_SELECTED_TARGET_ID`（最近一次攻击指令实际针对的敌 ID）→ `POSITION_MATCH_FALLBACK_160`（落点 160 内）→ `POSITIVE_CONTROL_VISIBLE_TARGET`。三个参数值都会写进 `battle_config`。每个兵种**首次**采样附一次 `dump=full`，之后只记常规字段。采样结果以 `report_reachability` 事件完整落盘（完整 raw JSON，不压缩）。
- 已确认的引擎事实（反汇编，详见 `助手交接\evidence\engine_semantics_trace.md`）：`aq.a(ao,float×4,int×3)` 逐格步进，三个 int = 预算 / 代价阈值 / 剩余步数，引擎自身取值 `(80,0,1)` 与 `(80,3,1)`；`am.t(am)` 用 `距离² < (cj₁+cj₂)²`，即 **`cj` 是碰撞/邻接半径**（不是 engagement radius）；末段"最近 185 + 零伤害"只作为 `RANGE_CONSISTENT_CANDIDATE` 过滤条件，不是射程证据。
- 覆盖测试：`tests/ReachabilityHarness.java`（第 16 个 Java harness，plumbing / 参数校验 / raw 形状 / 来源标注 / 禁令扫描）。`ReachabilityProbe.java` 仅作调查资产，不并入回归。

## 实机验收固定流程（输出6 §4 裁决）

```text
1. 双击 RW-Agent-Start.bat
2. 在这个游戏进程里进入目标遭遇战（裸开局）
3. 对局开始后只运行 RW-Agent-Match.bat
4. 不另开 RW-Agent-Battle.bat
5. Match 若 FAIL：本局立即作废，不在残局上补跑任何客户端，重开一局
6. 测试结束后再 Collect Reports
```

依据（用户第 5、6 局实机证据）：`#1~#4` 全部基线样本的 economy / development / battle 三份报告同一 session、阶段首尾相接 0.2~0.3 秒，即它们都是 `MatchClient` 一条命令跑出来的（`MatchClient.java` 第 15~19 行：preflight → EconomyClient → DevelopmentClient → BattleClient）；而 `BattleClient` 唯一的建厂路径 `planProductionFacility()` 以「现有工厂队列非空」为前置，`countType(landFactory)==0` 时永远不成立，因此**裸开局跑 Battle 只会建矿、不会建第一座工厂**，不能作为常规评测入口（用户第 6 局的崩溃报告里 `production_menu` 全程 `factories: []`，正是这一点的实证）。

## 建筑损失不再判 INVALID（输出6 §2 裁决）

`tools/battle_reports.py` 的「已观测」语义已修正：建筑（矿/厂）不会产生 `combat_unit_observed`，原先校验器把它当成我方新单位的唯一出生证据，导致「整局完整、只因一座新矿被拆」就被判 INVALID。现在「我方单位」的定义是：

```text
initial units
OR 任意 observation 中出现过（ownUnits）
OR 客户端记录过 completed 的事件（extractor/factory/tank_completed）
```

三项证据要求保持不变：确实曾出现、后续 observation 中确实消失、`own_loss` 的 unitId 与已知单位一致。这是校验语义修正，不是削弱证据要求；只改分析工具，客户端与交付 JAR 行为未变。回归用例见 `tests/test_battle_reports.py`（15 项，新增 4 项：建筑损失被接受、从未出现过的单位仍拒绝、仍存活时仍拒绝、仅凭 completed 记录即可接受）。

