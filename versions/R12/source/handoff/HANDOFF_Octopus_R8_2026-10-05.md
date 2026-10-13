# R8：施工、General 推进与空中搜索持续执行

日期：2026-10-05。分支 `codex/octopus-g3-execution-20261002`。从已交付 R7 `366dabd13eb9280c606be021277b93ab6eadbfd3` 继续，产品 HEAD `f87f7a0956ee69be83e3395d5f23ce5f36b54146`；文档提交后的最终 HEAD 以 Git 为准。

本轮保持重坦、Commander、Recovery、FREE/JOINING、经济和 G6 专家转交主循环，修复实际执行断点。完整 Windows 和原生 fixture 已通过；最终 Spain 自然局跑满 4800 游戏秒，结果 **PARTIAL / native ONGOING**，不是胜利、真人验收或受控 A/B。

## 交付入口

- 候选 JAR SHA256：`1014855760fe9880650e46b8aa960a9a33115f563000fadb16c65c07db90f19e`。
- 隔离人工环境：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R8`，端口 `47689`，G6 / compact / gzip 开启。
- 用户自行 `1-Start-Game.bat` → 选择 Spain、阵营、难度并开始 → `2-Run-Agent-7200.bat`；另有 1200/2400 游戏秒入口。报告在 `runs\<本局ID>\`。
- [人工观察点](../docs/OCTOPUS_R8_HUMAN_VALIDATION.md)、[持续运营契约](../docs/OCTOPUS_R8_CONTINUING_OPERATIONS.md)、[使用说明](../docs/AGENT_USER_MANUAL.md)、[候选清单](../deliveries/octopus-r8-2026-10-05/candidate-manifest.json)。
- 只准备环境，没有启动/关闭/操作用户桌面游戏，没有 deploy / push，没有改 Universal Bridge 掩盖 Agent 问题。

## 真人 raw 与根因

输入：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R7E\runs\20261005-091432-24728`。
raw `rw-agent-reports\battle-1791162874236-63e9c02d.jsonl.gz`，57,057,173 bytes / 250,580 rows / malformed 0，SHA256 `daa2e8980062582c9b00cd0c832ff726d37823e5821f03ae444146d3bc7de925`。入口确认原 R7E JAR；真人 player 5，与本轮自然局 player 0 条件不同。

1. **Builder 到合法矿旁不施工，有预算因果。** Builder 4153 在 505.048s 到达 prospect；510.868s 当前 `/expansion/plan` 有距离 58.596 world、报价 700 的合法矿点，wallet 1529，但 effective 116、unsettled 1400。此前 Builder 568 对同址工厂 `(7190,1910)` 收到两次 accepted，各记 700 承诺：i67/218.778s、i74/219.168s，价格来源同为 o2845。后帧只出现一个工厂 975；一个现场效果不能结算两笔账，也不能猜退款。
   - `BattleClient.advanceBuildJob()` 原 accepted guard 只保护 capacityExpansion。现在所有现代 G3 普通施工 job accepted 后等待自身现场 witness，不重复提交/记账；显式 G3=false 历史行为保持兼容。
   - `StrategyDirector` 对已抵达、当前同 actor/session 合法近矿但 free 预算不足的工作保留本地承诺，输出 `strategy_local_mine_waiting / LOCAL_SITE_AWAITING_FUNDS`，不以小步 MOVE 伪装继续施工。仍用原 retry 和真实报价，不绕过资金/合法性。
2. **攻城通过后被第二套防区判定打断。** 244.548s General 1 五成员获 accepted attackMove，指向当前基地 148 `(5710,1810)`；245.483s 又触发 `LEGAL_BASE_ZONE_TRAVEL_VETO`，MOVE 到距离基地 168.97 world 的位置，仍在已核验 280 范围内。正常 UNKNOWN-defender exposure 已允许战斗，防区却再次要求完整已知攻城交换。
   - `GeneralCombatDirector` 对当前合法可见、native eligible 的阻挡基地复用正常已知 exchange 或可信己方 profile 的既有 UNKNOWN-defender exposure fallback；消除重复否决。失联、防区普通 travel、小编制保护、严重实际损伤与兼容性判断保留。不是全局下调建筑威胁，也不以 receipt 证明开火/击杀。
3. **长期推进被中途路点和重复 native fallback 打断。** 真人后期同一外侧 frontier 46657 反复出现；640 world 中途 waypoint 到达被当成外侧信息前沿完成，下一段又被去重阻挡。soft avoid 被 native exhaustive fallback 忽略，单轮重复查询同一候选。
   - 到达只完成实际 accepted waypoint，外侧 frontier 未到仍可继续；失去 movement anchor 不冒充到达。同一 collect 重复候选复用合法 detour 或停止重复查询，没有新增时间降频 gate。
4. **空中搜索每段过短，且等待新 anchor 时丢掉逃离计划。** 真人有 177 search legs，端点几乎每次仅 260 world / 14 tiles；110 次后帧位置 witness，175 次端点附近样本，另有 35 次己方飞机位于旧端点附近且无订单。稀疏样本不能计算完整 idle 时长。
   - scoped `routeScope=air-search` 返回已核验 AIR 的完整合法路线，AirGeneral 持久保存 cursor，选择连续安全直线路段、不跨 BFS 转角；实际 leader 后帧位置才推进。危险取消旧行进后保留等待新 anchor 的 escape route，原地 hold 不再次当作危险 travel。当前合法目标中断，clear 后继续搜索。旧 unscoped Scout/Ground API 输出和路段 caps 不变。
5. **额外必要修复：当前 actor 缺席不能终止整局。** 真人 source game 2,975,848ms 异常结束，`IllegalArgumentException: Generation snapshot actor not own`，发生在资本菜单链后；compact raw 不足以确认具体缺席 ID。
   - direct dispatch 与 force collect 在 generation snapshot 前局部拒绝当前缺席/越权 actor，混合组整体拒绝。`g3_admission_rejected` 保留来源、reason、nativeAttempted=false；不发 HTTP、不留资金或 slot、不注入虚假 ownership。已 collect Intent 保留 Scheduler 的 generation / ABA 校验。

证据与源码：`BattleClient.java`、`StrategyDirector.java`、`GeneralCombatDirector.java`、`AirGeneralController.java`、`bootstrap/ScoutBridge.java`；紧凑因果索引见 [human-causes.json](../evidence/octopus-r8-2026-10-05/human-causes.json)。

## 分阶段提交

| commit | 内容 |
|---|---|
| `cc1a609` | paid construction 唯一性、当前 actor 局部拒绝、已抵达近矿工作保留 |
| `dd25b5a` | General 中途路点续行、重复基地 veto 和重复 frontier 查询 |
| `be7a1e2` | scoped 合法 AIR 完整路线、持久 search cursor、测试接线 |
| `048ee38` | 取消危险 travel 后保留待续 escape route |
| `b62b52f` | 根代理补 pending hold / escape regression |
| `f87f7a0` | 显式 G3=false 历史 construction compatibility |

子智能体仅复用 Sol 6.1/high 的局部 Builder、Combat、Air 工作；主代理负责集成、兼容修正、最终构建、完整 Windows 与隔离自然运行。

## 最终验证

- **完整 Windows：103/103 PASS，69 Java harness、31 Python suites / 503 tests、3 build，`failed steps: 0`。** 最终 `final-verification-v2`，260 测试输入零漂移。独立 full 构建与冻结候选 JAR 的 215 ZIP entries 内容完全一致；人工 kit 1895 文件零漂移，JAR 一致。
- **新增 R8 focused：96 checks**：ExecutionAdmission 27、BuilderWork 12、BaseAdmission 9、FrontierContinuation 8、AirSearch 40。旧 16 个受影响 harness、Capital 30、NativeCreditWitness 178 同时保持通过。
- **最终原生：11 阶段全部 PASS**：G53 continuous/order reuse、G51 operations、G5 closure、G3 producer/mode/lane、G54 independent、G55 operations、G6 native、Spain 第三类地形。原生对象/HTTP 校验，fixture 人工位置/时钟/fog 等，0 simulation ticks；证明有界原生接口与状态，不能证明自然飞行、实际持续开火或胜利。
- **最终自然 Spain：4800 游戏秒、原 supplied map、normal AI（difficulty 0）、team 0、请求 5×，无 cash/HP/position/fog mutation。** 正常预算结束 `client PARTIAL / native ONGOING`，没有真人旧局的异常退出。早期与同机隔离回归/原生检查有重叠，不作为性能受控 A/B。
- 中间失败原样留存：旧产品的新增攻城/续行 tests 红灯；初次 fixture 未标 preexisting factory；首轮 full 的显式 G3=false compatibility failure；已替换的早期自然 run。未用改旧断言掩盖产品失败。索引见 [intermediate-failures.json](../evidence/octopus-r8-2026-10-05/intermediate-failures.json)。

完整入口：[validation-summary](../evidence/octopus-r8-2026-10-05/validation-summary.json)、[Windows matrix](../evidence/octopus-r8-2026-10-05/windows-matrix.json)、[native summary](../evidence/octopus-r8-2026-10-05/native-summary.json)、[delivery integrity](../evidence/octopus-r8-2026-10-05/delivery-integrity.json)。外部原始证据根 `G:\deepseek 工作台\_validation\octopus-r8-20261005`，大 raw 不入 Git。

## 自然运行看到了什么

最后完整己方 observation 在 4,795.824s：**347 ready 重坦、6 陆厂、31 ready 矿（强化T3 8 / T2 14 / T1 9）、5 Builder、5 repairbay、62 两栖飞机、6 重炮、2 工程师、9 资源制造仪（T2 8 / T1 1）**，wallet 17,297.5。后续独立端点/summary 时间不同，不能拼成原子世界；6 次工厂升级完成。说明中后期主循环仍实际运行，不证明比 R7 更强。

- AirGeneral 有 73 个不同单位实际转交、71 个后帧 ATTACHED；峰值当前 roster 62。29 search legs / 15 后帧己方 arrival witnesses；最长已计划连续直线 109 tile steps，即 2180 world（tile20），不再每段260。170 attack receipts 不能当击杀或实际开火次数。
- 预算结束 intentional shutdown 会清空 summary Air roster；清理前事件成员62，不能把空 roster 当整军消失。
- 909 credit settlements，最终 **7 项 / 12,200 unsettled**；没有同 exact construction path 的并存未结账重复。尚不能说所有付款完全结清，继续保守保留，不 timer 退款。
- Scout 601 的当前己方 samples 62.832～123.056s，3 个完整样本，离首样本最远 1137.72 world。之后缺席不证明死亡；本局没有它长期生存、伤后恢复或持续覆盖全图的证据。
- 末期15 General。全局有效均值约1099ms game，单 observation最多118命令；后半段已记录 runtime windows median1952ms / p95 8144ms / max12496ms game。窗口是日志样本，不是全部决策。慢循环仍存在，不能说全解决。
- 末期150s capacity窗口支持完整覆盖/sustained，仍因约29%利用率给 UNKNOWN/HOLD；不再是 >6s 合法区间丢弃。不能用 UNKNOWN 冒充已找到产能瓶颈。

自然 raw：`spain-delivery-4800\work\rw-agent-reports\battle-1791165977343-ca3005eb.jsonl.gz`，107,660,172 bytes / 462,936 rows / malformed0，SHA256 `c4f2589185df10b1ebfd1bc331c95a9ce6555a7a297ea3c720b368e2ec2fce4b`。紧凑派生见 [natural-summary.json](../evidence/octopus-r8-2026-10-05/natural-summary.json)。legacy ownLosses 不是确认死亡/KD，不在本交接用于战果判断。

## 调度现实、未解决与下一断点

现代 runtime 已取消 independent actors 的全局人为操作额度，本轮没有“放大每秒次数”。保留同 actor 冲突、owner/generation/ABA、有效订单复用、资金、slot 与军队分块；Ground force 真 collect→priority→dispatch，经济/Strategy/Recon/Air 仍按当前阶段 immediate dispatch，不宣称所有 lane 全局优先级。

真人 max75 commands/observation 与自然 max118 已排除旧固定操作额度作为本轮慢循环的解释。末次 StrategyObserve约510 wall ms、serviceOperations约552ms、forceCollect约120ms，含 nested endpoint/commands，不是 CPU profile；去重查询改善一个成本来源，但大规模 IO/扫描仍需精确定位。

**NEEDS_EVIDENCE：** 真人攻城犹豫是否消除；Builder长期等待是否减少；Scout存活、恢复/补侦察与持续广域侦察；空海目标清理后不断续行；未知水下目标首次发现、WATER/ISLAND 独立搜索与 native victory；7项未结账能否取得唯一 effects。

下一轮先读这份交接和 R8 真人 raw，保留已通过主循环，不重新 G0～G4 审计。优先五个实际观察：到矿施工与报价/真实预算、基地圈内订单是否被重复打断、战斗后实际续行、AIR路段/危险绕行/clear后恢复、Scout当前状态与全图最后残敌。终局仍未达成，不用人工位置 fixture 代替自然胜利；若继续优化 cadence，先拆端点耗时/重复查询，不恢复全局限额或盲增频率。

## 工作区保护

产品、测试和本轮文档按阶段 commit；整体工作区有意不 clean。接手前已有的 `tools/RW-Agent-Octopus-Test.ps1`、`tools/rw-agent-human.py`、`tools/rw-agent-human.py.bak-20261003` 三文件原哈希全部不变，保留修改/backup，不 reset/clean、不纳入本轮提交。原版、冻结基线、旧 R7E kit、真人 raw/回放保留；源码偏离不授权回滚。
