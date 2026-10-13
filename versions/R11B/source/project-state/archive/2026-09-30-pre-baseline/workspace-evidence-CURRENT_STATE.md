# CURRENT_STATE（当前状态速查）

> 只记录"现在是什么状态"。历史与分析过程见 `助手交接\对话N.txt` / `输出N.txt`。

## 当前候选

**KnowledgeBacked World Model / Capability v0（2026-09-29）** 已完成本机资料包接入、完整回归、P1F 安装和一局隔离原生验证。执行依据是 `助手交接/请Codex开始_KnowledgeBacked_v0_2026-09-29.md` 指向的施工版主合同；新交接见 `助手交接/HANDOFF_Codex_KnowledgeBacked_v0_2026-09-29.md`。

- 当前 P1F 安装件 = `developer/dist/rw-agent-bootstrap.jar` = 原生局 staged JAR，SHA256 `5741e241ce8ec1b921f36ed3274087609d0430f9281c44f6f62c096de84c5f54`；当前源码的 contentDigest `82438a7b373111dca88b38e7a1c2779527a9cef136cdb9a1297bdf4c511273a7`。JDK 13 的 A/B 独立重建内容相等，`aVsInstalled=[]`、`aVsDist=[]`，见 `助手交接/evidence/candidate_sha_lineage.json`。其中 C 臂仍是历史 capability diagnostic guard 回退，不是新 Target Guard 消融。
- 冻结 `game-lib.jar` SHA256 `8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`；冻结环境根 Agent SHA256 `0681b4f7ef632a9c9372fff418fec591f240894fd99373ff6c7ca3b72d4a9eb1`，均未修改。P1F 旧 Recon v0.2 回退件在 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\p1f-before-knowledge-backed-v0-20260929.jar`，SHA256 `0c53bdfa1b545088e15fa6ab4269216b5c3fa586b00738bf00f5770ac5adc71a`。
- 新功能：合法可见敌人当前 AIR/SUBMERGED/SURFACE → 三态 Target Guard 并进入候选/混编分配；合法可见格保留 terrain flags 与分 movement 的原生 `d[]` 代价，未探索 UNKNOWN；Recon 路线消费 terrain 与 movementType，保留 v0.2 任务和主力保护边界。
- 回归：改前完整基线 3 构建 + 17 Java + 14 Python = **34/34**，改后最终完整 3 + 19 + 15 = **37/37**，failed 0。最终原始日志 `助手交接/evidence/knowledge_backed_v0_regression.log`。E2 原生地形对照 3 地图（含非方图）及隔离 PathingOverride 资产、6 移动域、438,450 格值匹配；两格 LAND 代价 -1→0、0→-1。见 `knowledge_backed_v0_terrain_e2.json` 与 raw ZIP。
- 隔离原生局：`work/kb-v0-native-final/run-20260929T081835-5e0d86/episode-001`，Big Island (2p)、难度 1、请求 4x，**PASS / VICTORY**；战报 SHA256 `211839c4c2953deaa4969c95a085d058018589769d5b59aa3c7a0fd7d0011cc1`，独立审计 `PASS`、`issues=[]`。797 次观察、209 条命令、4 座完成矿、2 座新厂、121 条确认的攻击命令、1 个 Recon 任务和 1 条观察到的 Recon 命令。Target Guard 实局事件 86（兼容 78、UNKNOWN 3、不兼容 5），两次混编对空保留 heavyTank 兼容子集；潜水目标明确拒绝。Frontier 计划 2 次，其中一条 LAND 路线有 `KNOWN` 地形证据，但本局没有创建 frontier 任务。详见 `knowledge_backed_v0_native.json` 和 raw ZIP。
- **边界**：LAND/HOVER 同格差异及 PathingOverride 为 E2-only，本局没有自然触发；一局隔离胜利不代表因果性能增益或用户桌面游戏验收。动态敌情观测过期时返回 UNKNOWN，不用静态目录补全隐藏实例。没有开展合同外策略扩展。

## Recon v0.2 交付时快照（历史）

> 以下旧条目中的“当前”仅指 2026-09-27 的 Recon v0.2 交付时点；今日实际安装候选以上方 KnowledgeBacked v0 身份为准。

- 安装位置：`游戏环境\P1F-冒烟环境\rw-agent-bootstrap.jar`
- 当前安装件 whole-file SHA256：`0c53bdfa1b545088e15fa6ab4269216b5c3fa586b00738bf00f5770ac5adc71a`（Recon v0.2 最终 JDK 17 构建；P1F 与 `developer/dist` 同字节）
- contentDigest（当前源码/安装件身份，忽略 manifest/zip 元数据）：`b292e168c3cabe71ba5bada3159d14d027d324cc7794d99fe80fba6ac396aa20`
- 谱系校验：安装件 == 当前 `developer/dist` == 当前源码经 JDK 17 重建的内容（`aVsInstalled=[]`、`aVsDist=[]`，`助手交接\evidence\candidate_sha_lineage.json`，由 `_analysis/sha_lineage.py` + `_analysis/assemble_lineage.py` 重新生成）。完整回归用项目 JDK 13 编译；其产物不宣称与 JDK 17 交付 JAR 逐字节一致。
- **注意**：whole-file SHA 只是构建产物，同一份源码重新构建就会变。引用候选优先写 contentDigest；写 whole-file SHA 时必须说明它来自哪一次构建。
- 冻结交付物（**永不修改**）：环境根 `rw-agent-bootstrap.jar` = `0681b4f7…d4a9eb1`；`game-lib.jar` = `8a550a37…620deec9`
- P1F 回退件：最终安装前的旧 v0.2 候选 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v021-before-final.jar`，SHA256 `e594770f7cf31494d5245c87a6dc5353a3b6cc4a83ae269ceed5ba4459ecac8f`；更早的 v0.1 备份 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v02-before-2026-09-28\p1f-agent.jar`，SHA256 `9394bdc52b449400d3ec332bf23e959c1a0ce9a8c28d8bbddaf255f276432a52`。
- **Recon v0.2 最新回归**：改动前完整基线 Java **17** + Python **13**、失败 **0**；最终完整 `developer/test-win.ps1` Java **17** + Python **14**、失败 **0**，`ScoutHarness` **160** checks，前沿客户端 **16/16** tests。JDK 17 最终候选已装入 P1F；当前源码经 JDK 17 重建的内容、当前 `dist` 与安装件匹配，`aVsInstalled=aVsDist=[]`；回归原始日志 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v021-final-regression.log`。
- **旧 v0.2 候选原生样本（历史）**：whole SHA `e594770f…` 的三局 JDK 13 原生样本分别是 Small Island 难度 1 `PARTIAL/ONGOING`、Small Island 难度 0 `PASS/VICTORY`、Big Island 难度 1 `PARTIAL/ONGOING`；三局 `frontierTasksCreated=0`、`expendableTransfers=0`，详见 `助手交接/evidence/recon_v02_trigger_forensics.txt`。旧候选战报不作为最终候选验收。
- **最终新候选隔离原生局**：`C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v021-native\run-20260927T184202-5237df\episode-001`，Big Island (2p)、难度 1、4x、1200 游戏秒预算。原始 battle SHA256 `d68a3083734e45a36a9942939d882e7fd4a102274d1cd25a8459a129a68cd38b`，staged JAR 与当前候选 whole SHA `0c53bdfa…` 一致；`助手交接/evidence/recon_v02_native_final.json` 审计 `PASS`、`issues=[]`。前沿任务 36、残血转岗 1、单兵 queued 30 / observed 24 / 己方位置进展 24、团队合法区域重见 23 / refreshed 23、完整转岗至刷新时间链 23（`NEVER_SEEN_EDGE` 14、`DEEP_FOG` 9）；193 次主力 `attack-move` 中 75 次有转岗侦察者排除检查，无违规，阻塞 12 次。episode `FAIL` 是预算到期分类，battle 为 `PARTIAL/ONGOING`，**没有原生胜负**。全队合法视野更新仅与已观察单兵移动及位置进展有时间关联，不能归因于该侦察者；桌面游戏和因果性能均未验证。下一步先做更强 Recon 的风险/活性收尾，经济与多人工作后置。
- **最终原生局的具体边界**：首条完整链为任务 #9、`c_tank` #345（转岗时 HP 2.485/210）：696192 毫秒创建，697616 毫秒转岗/queued，698992 毫秒己方 `move` 与位置进展，700432 毫秒后续全队合法视野刷新；原始战报行 4093、4110–4118、4124–4125。刷新与单兵移动仅有时间先后，不能归因于 #345。36 个任务中 23 刷新、12 阻塞、1 个在预算终点未结；阻塞为路线锚点漂移 9、已知威胁走廊 1、航点到达却无合法清空 2。任务 #24 的短航点 `move` 出现在原始己方观察中，但决策采样错过；下一轮优先处理 Recon 风险与活性，而非经济或多人。
- **上一轮固定条件 A/B v0 回归（历史）**：改动前完整基线 **17 Java + 10 Python，failed 0**；改动后用游戏自带 JDK 13 跑完整 `developer\test-win.ps1` 为 **GREEN**（Java harness **17** 项 + Python **11** 套，failed steps = 0；`test_battle_client` **78** tests、`test_headless_parallel` **22** tests、`test_ab_aggregate` **14** tests；详情见 `助手交接\evidence\ab_comparison_v0.txt`）。交付 JAR 随后用项目原定 JDK 17 重建，contentDigest 仍为 `6fec7a0c…`，谱系 `aVsInstalled/aVsDist = []`。该轮改动限于 Python 运行器/聚合器、测试和文档，Java 源码未变。Codex 子进程直接跑 JDK 17 harness 的历史 `Selector.open()` / loopback 环境故障见 `助手交接\Codex_接手核查_2026-09-27.md`。

- **上一轮回归（A/B campaign v0）**：改动前完整基线 Java 17 + Python 11、failed 0；改动后完整 `developer/test-win.ps1` Java **17** + Python **12**、failed 0。新 `test_ab_campaign` 14 例、`test_ab_aggregate` 17 例；`test_headless_parallel` 22、`test_battle_client` 78 均通过。该轮 Java 策略未变；证据 `助手交接/evidence/ab_campaign_v0.txt`。
- **历史回归（World Model v0）**：改前完整基线 Java **17** + Python **12**、failed 0；最终完整回归 Java **17** + Python **12**、failed 0，`CombatHarness` 105 checks。JDK 17 交付件与 dist/安装件 whole SHA 相同，源码 contentDigest 匹配，`aVsInstalled/aVsDist=[]`。新接口只记录合法可见敌人的会话内历史，客户端仅记录计数，策略未改。隔离原生对局产生 29 条有效心跳，但到 300 游戏秒仍是 `PARTIAL / ONGOING`；详情 `助手交接/evidence/world_model_v0.txt` / `world_model_v0_native.json`。
- **上一轮回归与原生局（World Model v0.1 / Recon v0，历史）**：改前完整基线 Java **17** + Python **12**、failed 0；最终完整回归 Java **17** + Python **13**、failed 0，`CombatHarness` **122** checks，新 `test_recon_client` **7** tests。当轮 JDK 17 JAR 曾安装到 P1F 与 dist，whole SHA `9394bdc52b449400d3ec332bf23e959c1a0ce9a8c28d8bbddaf255f276432a52`；当轮源码 contentDigest 与两件匹配，`aVsInstalled/aVsDist=[]`。隔离原生小岛 2p、4x、难度 1，战斗 433,424 游戏毫秒原生 **PASS / VICTORY**；7 个 Recon 任务、9 个单兵 queued、7 个后续己方 move、12 条位置进展、7 次合法重见（其中 4 次与已观察移动命令有时间先后）、2 次撤回。不能把重见或胜利归因于 Recon；自然旧址 `recon_site_cleared` 仍未观察到，只在 E2 夹具验证。详见 `助手交接/HANDOFF_Codex_WorldModel_v01_Recon_v0_2026-09-28.md` 与 `evidence/world_model_v01_recon.txt` / `_native.json`。

## 诊断旋钮（可改，需记录在报告里）

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `-Drwagent.reachabilitySample` | **true（默认开）** | 可达性原始采样；`=false` 完全关闭 |
| `-Drwagent.reachabilitySampleIntervalMs` | 30000 | 同一目标的最小采样间隔 |
| `-Drwagent.reachabilityTypeLimit` | 3 | 每次采样的代表兵种上限 |
| `-Drwagent.noProgressWindowMs` | 20000 | 目标无进展判定窗口（游戏毫秒），下限 2000 |
| `-Drwagent.noProgressCooldownMs` | 30000 | 降权后的冷却/重试间隔（游戏毫秒），下限 2000 |
| `-Drwagent.noProgressEngageRange` | 300 | "主力已接近"的世界单位距离，下限 40 |
| `-Drwagent.reconEnabled` | **true（默认开）** | 仅控制客户端 Recon v0 任务/命令；`=false` 关闭该新行为，桥接只读历史仍可用 |
| `-Drwagent.reconFrontierEnabled` | **true（默认开）** | `=false` 只关闭 Recon v0.2 前沿任务，保留 v0.1 旧址核查；`reconEnabled=false` 则关闭两个通道 |

## 开发默认值

| 旋钮 | 值 |
| --- | --- |
| `mobileUnitHardCap` | **40**（`-Drwagent.mobileUnitHardCap` 可覆盖；32 为 A/B 对照臂） |
| `activeArmyTarget` | 24 |
| `reserveTarget` | 8 |
| `mineTarget` | 3 |
| `landFactoryTarget` | 2 |
| `builderTarget` | 1 |
| 战斗阶段预算 | 900 游戏秒（`RW-Agent-Match.bat 1800` 可延长） |
| 启动入口 | 默认 `RW-Agent-Match.bat`（cap=40）；对照 `RW-Agent-Match-Cap32.bat` |

## 冻结模块（非回归不得改动）

- P2-B2 生产预算优先级：`BUILDER_RECOVERY` → 主厂主力单位/科技 → 副厂升级 → 副厂廉价生产
- 副厂 upgrade 语义：机会性盈余投资；`SECONDARY_INVESTMENT_DEFERRED` 为节流状态事件
- `SECONDARY_INVESTMENT_FALLBACK`：行为事件，**一次被桥接接受的 fallback 下单 = 一条事件**（拒绝 = 0 条）
- 单位评分 1/3/5（c_tank / heavyTank / upgrade）
- P2-C1 builder 恢复优先级与探针语义
- 单一军团战术（single-army tactics）
- 主厂不降级、`PRIMARY_TECH_BANKING` 不允许 cheap fallback

## 实验旋钮（可改动，需记录）

- `-Drwagent.mobileUnitHardCap`（32/40 对照）
- 其余旋钮目前不作为实验变量；改动需先写进交接

## 最近的正式样本

| 样本 | 条件 | 结果 |
| --- | --- | --- |
| `battle-1790444931065-1b7fa4c7`（新局C） | 180×180 格 / **5.0x** / cap=40 / 候选 `f3d63e75` / 901 游戏秒 | ONGOING/PARTIAL；矿 6（+1 末段在建）、`minesBeyondFloor=5`、意图 5/完成 1/取消 4、`measuredIncomePerGameSecond=80.08`、builder 闲置 59.9%、UNEXPLAINED 0.0s、掉 1 座矿 |
| `battle-1790445226048-8149ed41`（新局D） | 180×180 格 / **1.0x** / cap=40 / 候选 `f3d63e75` / 900 游戏秒 | ONGOING/PARTIAL；矿 5、意图 6/完成 3/取消 3、builder 阵亡并恢复 1 次、`measuredIncomePerGameSecond=69.68`、掉 2 座矿、UNEXPLAINED 2.1s |
| `a820d9ad` | **巨岛 180×180（用户提供）** / 非常困难 / 1x / cap=40 / 战斗 776.5 游戏秒 | **PASS / VICTORY**；ownLosses 34；newCombatUnits 68；升级 2；矿 2；厂 1；22 个采样批次 |
| `fe3aa2bb` | **湖陆 130×130（用户提供）** / 非常困难 / 1x / cap=40 / 战斗 550.7 游戏秒 | **PASS / VICTORY**；ownLosses 18；newCombatUnits 51；升级 1；矿 2；厂 1；12 个采样批次 |
| `26d18c53` | 巨岛 180×180 / 非常困难 / 1x / cap=32 / 900.4s | ONGOING；heavy/c_tank 51/22；produced/lost 73/64；army 25/33；`army≥32` 心跳 22.7% |
| `89b254f3` | 巨岛 / 非常困难 / 1x / **cap=40** / 900.2s | ONGOING；heavy/c_tank 48/15；produced/lost 63/29；army 36/41；`army≥32` 心跳 61.4%；末段 41 兵压 `seaFactory` 51s 零伤害 |
| `4d07a05b` | 冰岛 145×145 / 非常困难 / 1x / cap=32 / 738.8s | **VICTORY**；heavy/c_tank 33/9；builder 恢复 2 次 |

> 地图名与 AI 难度**报告里没有字段**（observation 只有 width/height/tiles），属于**用户提供**，上表按用户口述记录；`gameSecondsPerWallSecond = 1.0` 与 cap=40 是报告自证的。

## 已知已验证缺陷（已修，保留记录）

1. 副厂 fallback 重复上报（两个 block 各写一次）→ 已收敛到唯一 owner；冰岛局 8 次下单曾写 16 条。
2. fallback 被 10 秒节流导致漏报 → 已改为"下单成功后写一条"。
3. T2 副厂（菜单无 upgrade）写幻影 defer/fallback → 已抑制。
4. `raw_flag_table.py` 的 aircraft 判定大小写 bug + 人工对列错位 → 已改为表头驱动取值；**此前基于该表的 `am.cI` 结论作废**。
5. 白名单口径与实现不符（Astra 对话29 §5）→ 已按"精确 descriptor + 父类链只认已审计 owner"修复；第 1 版修复曾引入 movement-class 功能回归，已修复并补回归用例（见上）。
6. `reflectionGuards` 自报计数漏 `notAllowedSkipped`、且序列化位置早于同响应内的拒绝调用 → 已改为响应末尾输出并补用例（`the refusal count covers every refusal in this response`）。
7. `_analysis/evidence_index.py` 解析自己写出的索引时未去掉反引号 → sha 永不相等 → 全部历史证据被标 UPDATED、delta manifest 一次列出 48 个文件要求上传 → 已修（去反引号 + 要求 5 列），修复后当轮 SEND 由 48 降到 5 个文件。

## 已验证主线与当前候选边界

**`Economy v1b`（动态陆厂，产能瓶颈驱动）= DONE_AND_LIVE_VALIDATED（对话43 实机：巨岛 5x 建出第 3 座厂；冰岛 5x 正确拒绝并获胜）。**

这两局 E4 战报的候选 whole-file SHA 前缀为 `73a09229`。**当时** v1b.1 候选的 contentDigest 为 `6fec7a0c…`，包含随后裁决的 Q2 承诺语义与 Q3 显示精度；它已有宿主机完整回归 GREEN，**尚未单独补 E4 桌面实机证据**。旧 E4 不代表当时 v1b.1、随后 `63db4e65…` 或当前 `23e6f0fb…` 候选的桌面实机验收。

证据 `助手交接\evidence\economy_v1b.txt`（实现）× `economy_v1b_live.txt`（实机）
× `v1b_trigger_feasibility.txt`（预注册可达性）；
代码在 `BattleClient.considerFactoryTargetIncrease / observeFactoryLoad / factoryLoadWindow /
productionConsumptionPerGameSecond / modelledIncomePerGameSecond / sustainableSurplusPerGameSecond`。

- **实机验收链（巨岛 5x，候选 `73a09229…`，900 游戏秒 PARTIAL）**：
  `factory_target_increased` @**316.2s**（目标 2→3；满载 **82.2%**；收入 63.1 − 消费 50.3 = 剩余 **12.8**/游戏秒，
  三项自洽；余额 860.5 ≥ 700；两个硬储备均为 0）→ `production_facility_planned` @363.5 →
  `started` @368.8 → `factory_completed` @**387.3** → `production_facility_finished(factories=3, target=3)`。
  **触发那一刻 army = 19 / cap 40** —— 正是裁决要求的"第 3 厂可以在 army<40 时合理出现"。
- **新厂确实提高了产能**：已接单生产消费率 46.0/游戏秒（第 3 厂完工前 150 秒）→ **74.7**（末段 150 秒）。
- **硬储备未被吃掉**：`investment_reserve_stuck` **0 条**；增加目标时 `builderReserve=0`/`investmentReserve=0`；
  结束 `investmentReserveAtEnd=0`；389.9s 的一次拒绝明确带着 `investmentReserve=700` 参与判定。
- **冰岛 5x（403 游戏秒 PASS/VICTORY）未触发，且拒绝可解释**：唯一卡点是满载 **76.8% < 80%**
  （收入 63.1 > 消费 48.0，剩余 +15.1/游戏秒；余额 1134 ≥ 700）——**只差 3.2 个百分点**；
  该局不需要第 3 座厂也赢了。
- **收入常数获得样本外确认**：把这两局加入合并（6 局 25 窗口），逐档中位数仍为
  39.26 / 51.06 / **63.02** / **74.96**；稳健拟合 **12.06**/矿/游戏秒、基础 **26.91**，R²=0.999。
  巨岛(5x) 单局独立拟合 11.96 / 27.14（R²=1.0，4 矿档 438 秒窗口实测 74.98）。
  ⇒ 代码里写死的 `26.9 / 12.07` **在未参与拟合的新样本上误差 <0.2 资金/秒**。
- **收尾轮（对话39 裁决 Q1/Q2/Q3，已完成）**：
  ① **Q1**：只读复算否掉了 demand-active 代理（六局中位数 93~100、P90 全 100，几乎恒成立；增量来自
  `RESERVED_FOR_*` 预算按住 与 `NO_AFFORDABLE_ACTION` 收入不足 —— 两个方向都指向"加厂没用"），
  **维持 saturationPct 与 80%**；② **Q2**：动态提升出来的目标改为**容量承诺**，
  不再被单帧 `FACTORY_QUEUE_EMPTY` 否决（实测曾多等 47.3 秒），**基线第 2 厂路径一字未改**，
  仍需覆盖「厂价 + builderReserve + investmentReserve」；③ **Q3**：`saturationPct` 改 2 位小数，
  不再出现「80.0 却拒绝」。证据 `助手交接\evidence\economy_v1b_tail.txt` 与 `q1_demand_active_proxy.txt`；
  新增汇总字段 `factoryTargetCommittedAtEnd`。
- **唯一行为变更**：`landFactoryTarget` 可变，四道长窗口闸门同时成立才 +1（上限 `-Drwagent.landFactoryTargetMax`=5）：
  ①窗口预热（`time-startTime ≥ -Drwagent.economyWindowMs`，默认 150000 游戏毫秒，下限 60000）
  ②长期满载（已完工陆厂"队列非空"的**时间加权**占比 ≥ `-Drwagent.factorySaturationPct`=80%）
  ③可持续剩余（`建模收入 > 生产消费率`）④余额 ≥ 厂价 + `builderReserve` + `investmentReserve`。
- **收入进代码为可替换实测常数**：`26.9 + 12.07 × 已完工 T1 矿数`（`-Drwagent.baseIncome` /
  `-Drwagent.incomePerMine` 可覆盖），`report_provenance.economyModelSource` 标注来源与口径。
  消费率只用已闭合 `spend` 账本的长窗和，**不用余额差分**（批处理下单/逐件扣款会污染短窗读数）。
- **明确不做**：不以"军队触顶 + 钱没地方花"为触发依据（新局D 514~607s 就是这种状态：army 40/40、
  余额 1953→10184、两厂队列全空、利用率 26~55%——那是没有需求，不是瓶颈）；
  不加临时 `credits > X`，不加"剩余 ≥ Y"这类拍脑袋裕度；矿升级/厂升级/防御塔/Scout/Role 不动；
  紧迫度四态阈值与 `army ≥ 24` 门槛不动。
- **顺手补的诊断字段**：`own_loss` 增加 `type`/`x`/`y`；`extractor_completed`/`factory_completed`
  增加 `completedAtGameMs`（`firstSeenGameMs` 是开工不是完工，差一个建造周期 ≈17.5 游戏秒）。
- **本轮登记的独立 backlog**：`ABOVE_FLOOR_ECONOMIC_LOSS_REEVALUATION`（越基线矿被摧毁后是否重新评估投资；
  1x 局 6→4 矿未重建，因矿数仍高于下限 3 且当时军事压力关着经济通道。语义：压力期间可不补，
  压力解除后重新评估**当前最佳投资**，不要求原地补回旧矿）。

**`Builder Utilization v0`（减少建造者空转）= DONE_AND_LIVE_VALIDATED（对话42，两局实机：5x + 1x）。**

- **实机验收（E4）**：候选 `f3d63e75…`，新局C（5x，901 游戏秒）与新局D（1x，900 游戏秒），无崩溃、账本闭合。
  **"没有任何闸门挡着却闲置"的时间：旧候选 26.3 / 60.3 秒 → 新候选 0.0 / 2.1 秒。**
- **60 秒退避缺陷的精确指纹（旧候选 `c7bf22e5`）**：`investment_released(COMPLETED)` @448.7s → 下一个
  `investment_intent` @509.0s，间隔 **60.3 秒**（期间紧迫度 `OPEN_EXPANSION`、army=34、余额充足）。
  新候选同位置 **1.0 秒**（1x 局两次）、**2.6 秒**（5x 局）。
- **双重门槛已消除**：`NO_SURPLUS` 事件数 旧局2 **7 次** → 新局C **0 次**、新局D 2 次。
- **builder 位置不动占比**：77.1 / 62.8% → **59.9% / 59.3%**；剩余闲置全部设计内（军事压力 356~358s、
  army<24 为 108~116s、意图在手探路/等待 40~61s）。**施工本身只占 101~110 秒**（17.5 秒/座）。
- **1x 局额外验证**：builder 于 579.9s 阵亡，`builder_recovery_started → (9.3 秒后) completed`，P2-C1 恢复链实机走通。

**`Economy v1a`（投资意图与预留）= DONE_AND_LIVE_VALIDATED（对话40，巨岛 5x）。**

- **验收链实机证据**（`battle-1790441059316-57d35d69`，PARTIAL 900s，5.0x，无崩溃）：
  `extractor_completed minesCompleted=3 unitId=808 armyAtCompletion=34 belowHardCap=True investmentReserve=700`
  ——**额外矿在 army=34 < cap=40 时完成，付款来自被预留的 700**。三次越基线开矿时的 army/已建矿 = **(32,3) / (35,4) / (32,5)**，全在 cap 以下；对照 v0.1 是 41/41/40/34（几乎全在触顶时）。
- **四局合并复核（对话42）**：19 座矿里 **16 座在 army 低于 hardCap 时完工**，另 3 座在 army=40/40
  （触顶=纯剩余产能，正是 above-floor 想要的情形）。
- **本局汇总**：`minesBeyondFloor=3`、`newCombatUnits=83`、`ownLosses=49`、意图 4 / 完成 1 / 取消 2 / 结束时挂起 1、`measuredIncomePerGameSecond=77.12`。
- **军事紧迫度真实触发**：两次意图被 `MILITARY_PRESSURE` 撤销，时点吻合战况（`sinceLoss=0`、army 40→21）。"正在挨打就把钱还回去"这条路径被真实走到过。
- **5x 下无进展检测器仍活着**：`target_no_progress=2`，决策间隔 2665 game ms、推导容忍度 7995（旧的写死 3000 只剩 ~12% 余量）。
- **判据修正（我的过严）**：比赛结束时意图仍在进行是正常的（本局末次意图 ageMs=44475，远未到 180 秒超时），原来的护栏却报 `investment_reserve_stuck`。现改为：结束时以 `MATCH_ENDED` 正常释放并记 `investmentPendingAtEnd=true`；**只有 `time > deadline` 才记 stuck**（真正的卡死）。回归用例 `test_an_intent_pending_at_match_end_is_not_a_defect`。
- **前一轮的 NPE**（`investmentCandidate` 返回空 map → `reportProductionDeferred` 取 cost 崩溃，曾打崩 3/4 局）已修复：无匹配返回 `null`，且 `reportProductionDeferred` 对无 `cost` 的候选按"没有候选"处理——**诊断记账不得终结比赛**。
- **本版不做**：`NEW_BUILDER` 候选、升矿、升厂、机会成本模型、完整威胁评估。
  （其中"动态新厂"已在 `Economy v1b` 落地；紧迫度阈值仍未调参。）

**`Speed Readiness`（倍速就绪）= DONE_AND_LIVE_VALIDATED（1x→5x 渐进实机，对话36）。**

- **实机确认**：候选 `9939dd7f…`，巨岛，**`PASS / VICTORY`**，580 游戏秒 / 186 现实秒，平均 **3.12x**。速度曲线由报告逐帧重建：2.00x → 4.00x → 4.99x → 3.99x → 1.00x → 5.01x，与用户按键过程一致。
- **新增字段自证**：`gameSecondsPerWallSecond 3.12`、`effectiveDecisionIntervalGameMs **2690**`、`noProgressAttentionGapMs **8070**`（=3×2690）、`maxObservedGameTimeJump 2690`、`observationCount 358`。
- **关键量化**：5x 下实测决策间隔 **2690 游戏毫秒**，而旧的写死容忍度是 **3000** —— 余量只有 **310 毫秒（10%）**。也就是说这个用户常用的 5x 距离"检测器每帧重置、静默失效"只差 10%，再加半档就会死。之前"5x ≈ 2.6 游戏秒"的推算与实测吻合（差 3.5%，即 HTTP 往返）。
- **没有漏报（独立核对）**：`target_no_progress = 0` 但检测器是活的——`target_progress` 11 次证明追踪器在运行；审计脚本独立复算本局最长"接战但血量不动"仅 **5.0 游戏秒**（共 11 段 ≥1s），远低于 20 秒窗口，**本来就没有停滞**。
- **其余健康度**：命令 **181 请求 / 181 回执**、`command_rejected = 0`（无丢命令）；186s ≪ 2400s 墙钟上限；矿数 1→2(180s)→3(360s) 后持平（基线行为未变）；越基线拒绝 24 次全是 `NO_SURPLUS`（本局余额始终 <1500，门槛拒绝正确）。
- **本版不做**：不改 `pollMs` 自适应、不重做调度器、不动任何策略语义、不改开局客户端的 wall-time 截止时间（已登记的口径不一致）。
- **已知局限**：字段只记终值与最大值，"档位 → 决策间隔"的对应是事后由 `observation` 时间戳重建的；6x 及以上仍未实测（"≥6x 会静默失效"是算术推论）；一局不足以排除桥接并发读取的半更新快照风险。

## A-lite：账本与单矿收入（只读一轮，已完成）

证据 `助手交接\evidence\alite_ledger_builder.txt`（`_analysis/alite_ledger_and_builder.py`，未改产品代码）。

- **账本 5/5 完美闭合**：`spend` 事件之和 == `summary.spendTotal`（67400 / 56550 / 21650 / 28600 / 42150），分类为 `UNIT_PRODUCTION / NEW_MINE / NEW_FACTORY / FACTORY_UPGRADE / BUILDER_RECOVERY`。**账本可用**。
- **单矿收入（口径已修正，对话42/四局合并）**：**`收入 = 26.9 + 12.07 × 已完工 T1 矿数`（资金/普通游戏秒）**，四局 20 个窗口 R² = 0.999、残差 σ = 0.59；**单矿回本 = 58.0 游戏秒**，机制库先验 12.1 / 58 秒获实机确认。
  证据 `助手交接\evidence\two_speed_economy_builder.txt`（`_analysis/two_speed_economy_builder.py`）。
  - **上一轮报的 10.14 / 10.09 作废**：那是旧口径（固定 60 秒窗口 + 矿数中位数）的系统性偏差（斜率低约 15%、基础项高约 8 资金/秒、残差大 10~20 倍）。同批报告上新口径给出 11.82 / 11.89 / 11.97 / 12.61，逐档中位数拟合 **12.07**。
  - 逐档中位数（矿数恒定窗口，分母用真实跨度）：1→39.26、2→51.01、3→63.02、4→74.95、5→86.45、6→100.09；相邻档差 11.5~13.6，**1x 与 5x 给出同一个数**。
- **口径教训（已写进脚本）**：`measuredIncomePerGameSecond` 是**长窗口余额恒等式**——客户端按"下单批次"整笔记账，游戏按"逐件完工"逐笔扣款，所以只有跨过整批生产周期（**≥150 游戏秒**）才等于真实收入能力；短窗/EMA 读数会被批处理节奏污染（"无支出观测对"在低矿档实测为负收入）。
- **建造耗时实测 17.0~17.9 游戏秒**（23 座结构，观测序列 `buildProgress` 0→1 直接测量，1x/5x 一致，中位数 17.5）：机制库《建造者列表》16.7 秒在 5% 内命中，**可作先验**。
  - **坑**：`extractor_completed` / `factory_completed` **不带 `gameTimeMs`**，其 `firstSeenGameMs` 是**开工**不是完工；完工要 +17 秒。
- **builder 闲置占比（计入施工后）**：旧 62.8~77.1% → 新候选 **59.9%（5x）/ 59.3%（1x）**。归因后剩余闲置全部设计内：军事压力 356~358s、army<24 为 108~116s、意图在手探路/等待 40~61s、释放后冷却 0~26s。


## `Builder Utilization v0`（减少建造者空转）= DONE_AND_LIVE_VALIDATED（对话42，两局实机：5x + 1x）

- **实机验收（E4）**：候选 `f3d63e75…`，新局C（5x，901 游戏秒）与新局D（1x，900 游戏秒），无崩溃、账本闭合。
  **"没有任何闸门挡着却闲置"的时间：旧候选 26.3 / 60.3 秒 → 新候选 0.0 / 2.1 秒。**
- **60 秒退避缺陷的精确指纹（旧候选 `c7bf22e5`）**：`investment_released(COMPLETED)` @448.7s → 下一个
  `investment_intent` @509.0s，间隔 **60.3 秒**（期间紧迫度 `OPEN_EXPANSION`、army=34、余额充足）。
  新候选同位置为 **1.0 秒**（1x 局两次）、**2.6 秒**（5x 局）。
- **双重门槛已消除**：`NO_SURPLUS` 事件数 旧局2 **7 次** → 新局C **0 次**、新局D 2 次。
- **你提的三条结构性原因里，15 秒探测间隔不是主要来源**：`NO_VISIBLE_LEGAL_SITE` 仅 13 / 16 次。
- **builder 位置不动占比**：77.1 / 62.8% → **59.9% / 59.3%**；剩余闲置全部设计内（军事压力 356~358s、
  army<24 为 108~116s、意图在手探路/等待 40~61s）。**施工本身只占 101~110 秒**（17.5 秒/座），
  建造者另有 252~261 秒在移动——本配置下稀缺的不是建造工时而是"该不该开矿"的决策。
- **v1a 链条健康**：19 座矿里 **16 座在 army 低于 hardCap 时完工**，另 3 座在 army=40/40（触顶=纯剩余产能，正是 above-floor 想要的情形）；`investmentPendingAtEnd` 正常以 `MATCH_ENDED` 释放。
- **1x 局额外验证**：builder 于 579.9s 阵亡，`builder_recovery_started → (9.3 秒后) completed`，P2-C1 恢复链实机走通。

- **成功不再继承失败退避**：60 秒 `investmentRetryCooldownGameMs` 本为 `TIMEOUT`/`MILITARY_PRESSURE` 防抖动，
  但 `releaseInvestment()` 对所有原因都写了时间戳，于是**成功建成矿后也要空等一分钟**。现在只有非 `COMPLETED` 才启动退避。
- **意图在手时以矿价本身为准**：两处闸门原先都要求 `余额 ≥ 矿价 + 一辆首选兵的钱`；既然意图已预留矿价，
  再要一辆兵的钱等于把软储备变成绝对门槛（实测 32–108 闲置秒）。现在只看 `余额 ≥ 矿价`；硬储备不受影响。
- **顺带补一致性**："厂先于矿"原先只在**探路**闸门里，而探路只在没有可见点位时运行 → **可见点位会绕过它**、
  把建造者从建厂通道抢走。现在建造闸门也检查。单位价格不再参与该判断，`UNKNOWN_UNIT_COST` 分支随之消失。
- **E4 已完成（对话42，两局实机）**：**5x 局 `battle-1790444931065`（候选 `f3d63e75`）+ 1x 局 `battle-1790445226048`（同候选）**——
  报告自证倍速 5.0 / 1.0，用同一个 `_analysis/two_speed_economy_builder.py` 复算，见本节顶部的实测表
  （UNEXPLAINED 0.0 / 2.1 游戏秒）。**注意**：后来的巨岛 / 冰岛两局（都是 5x、候选 `73a09229`）是 **Economy v1b** 的 E4，不是本项的。

## 历史任务：World Model v0.1 / Recon v0 = **本机回归与隔离原生行为链通过，待独立验收**

- GPT 最新合同授权 `RECHECK_INTEL` 单兵核查合法失联高价值建筑。本轮桥接补 `CLEARED`（旧址 3×3 邻域合法可见，只证明旧址清空）、客户端增加单任务/可退开关、至少 7 名可用武装单位才分兵、紧急时释放与召回、威胁圆直线停靠。`FRONTIER_SWEEP`、`SUICIDE_PROBE`、原生寻路安全验证和因果 A/B 未做。详见 `助手交接/HANDOFF_Codex_WorldModel_v01_Recon_v0_2026-09-28.md`。
- 原版小岛 2p、4x、难度 1 的隔离原生局 `PASS/VICTORY`，7 个任务、9 次 queued、7 次己方匹配 move、12 条位置进展、7 次合法重见、2 次召回；50 次主力 attack-move 未包含当时分配的侦察者。自然 `recon_site_cleared=0`，该分支只在 E2 夹具验证。胜利和重见不归因于 Recon。原始报告/时间线 SHA 在 `evidence/world_model_v01_recon.txt`、`world_model_v01_recon_native.json`。

## 上一轮任务：World Model v0 只读历史敌情 = **本机回归与原生接口闭环通过**

- `CombatBridge` 从已有合法 `visibleEnemies` 快照建立持久 `enemyIntel`：同 ID 记录首次/末次目击与最后已知位置、HP、类型、标记；本帧未见则标 `LOST_CONTACT`，不推断仍存活或死亡。旧位置清空后短期 `rememberedEnemies` 可消失，历史接触仍保留；会话变化即清空。通常保留至多 256 条，超限先淘汰最旧失联项；同帧可见量超过 256 时暂时保留全部可见。无新反射、无隐藏单位回查、无运行时引擎状态写入。
- `BattleClient` 只在心跳记录三个情报计数；进攻、经济、搜索仍沿用现有输入，**策略语义未改**。网络局桥接端点仍 409；这轮不是 self-play 或多玩家视角实现。
- 改前完整回归 17 Java + 12 Python、失败 0；修正测试夹具中的换局/旧 sessionId 顺序后，最终完整回归仍 17+12、失败 0，CombatHarness 105 checks。隔离原生 match 运行 300 游戏秒，29 条心跳：可见情报峰值 5、失联峰值 35；在 114s/135s 短期战术记忆与当前可见均为 0 时仍保留 17/18 条历史。原生结果 `PARTIAL/ONGOING`，没有胜率结论。`evidence/world_model_v0.txt` 与 `world_model_v0_native.json` 可复查原始 SHA。
- replay 的实验 recorder 虽写出原生 `.replay`，独立播放三次都从 frame 6452 起 checksum 不一致，故**未交付**；实验源代码撤回。多人 Match/self-play 和经济 vNext 只读分析后未施工。随后 GPT 已交接 `RECHECK_INTEL` 的行为合同，本轮完成了上节窄范围的 Recon v0；其他策略语义仍需交接。

## 已完成任务：A/B campaign v0 = **独立验收通过（ACCEPT，E2+E3）+ 一处原子写加固**

- 依照 DeepSeek 对话47 的 ACCEPT 与 GPT 的工程安排，在既有固定条件 A/B v0 上新增 `tools/run_ab_campaign.py`：输入 N，AB/BA 自动交替，逐批串行调用现有双实例 match，写 `campaign.json`，断点续跑只补缺口。跨批结算用原聚合器，检查 attempted / valid / partial / invalid / n，不做自动赢家或策略调参。当前最多同时 2 个独立无头实例。
- 安装版最小 N=2 原生验收：`_analysis/ab-campaign-v0-final` 的 AB 与 BA 各一批，proof 均 PASS，四份 battle raw 全 `VALID/PARTIAL/ONGOING`。每臂 attempted 2、valid 2、partial 2、invalid 0、nativeCompleted 0、正式指标 n=0；重复执行两槽 `launches=1,1`，聚合 SHA 不变。此 N 是工程冒烟，不是策略样本量裁决。
- 故障注入 `_analysis/ab-campaign-v0-recovery`：仅结束本次 campaign/runner，旧批两引擎留存；续跑按 claims 回收 2 个登记引擎、释放旧端口、保留旧 run、第一槽补跑一次、第二槽正常完成。清单最终 `launches=2,1`，重复执行不增加；两臂仍各 valid2/partial2/invalid0/n0。证据与原始报告在 `助手交接/evidence/ab_campaign_v0.txt`、`.json`、`_runs.zip`、`_sources.zip`。
- Windows episode 的 `libs/assets/res` junction 指向 campaign 自身一次性复制的 `resource-cache`，不直连游戏原件；原件与缓存指纹一致。安装版两批的实占 52,460,830 B（不跟随 junction）；批前有空间预算，超限停在 BLOCKED 且不删证据。完整但无效的报告或回收身份不明也 BLOCKED，须人工审查。
- **DeepSeek 独立验收（2026-09-27，判定 ACCEPT，E2+E3）**：对两个 campaign 各自跑**我自写的原始目录审计**
  （`_analysis\ab_v0_audit.py`：profile→客户端 JVM→游戏内 `battle_config`、AB/BA 位置、session/SHA/种类、proof、固定条件）
  与**独立重聚合**，结果与自带 `aggregate.json`、交接副本 `ab_campaign_v0.json` **逐字段相同**；
  episode 的 `assets/libs/res` 经 `os.lstat` 确认是**指向 `<campaign>\resource-cache` 的 junction**（不直连游戏原件），
  原件最新 mtime 仍是 2026-09-20（未被写入）；恢复现场 `reap-report.json` `killed=2`，旧 run 原样保留（pair-0001 下 3 个 run）。
  **我自跑的最小 campaign**（`--pairs 2`，120 游戏秒预算）`COMPLETE`、两槽 `launches=1,1`、聚合 SHA `7df348d0…`。
- **WinError 5 根因（已复现）**：`os.replace` 在**目标文件被不含 `FILE_SHARE_DELETE` 的句柄打开**时会被
  ERROR_ACCESS_DENIED 拒绝（`_analysis\winerror5_forensic.py`：同一失败目录重做原子写**成功** ⇒ 非路径/权限问题；
  并发只读句柄下**复现同一条错误**，释放后成功）。**最小加固**：`write_bytes` 对 `PermissionError` 做有界重试
  （≈3.75 秒，`RW_WRITE_REPLACE_ATTEMPTS` 可调），占用持续存在时仍原样抛出；新增 2 用例（`test_headless_parallel` 22 → **24**），
  回归 **17 Java + 12 套 Python failed 0**；runner 两处副本同步（新 SHA `40dbeb1a94f6e5d2…`）。
  **后果**：plan 绑定 `runnerSha256`，旧 campaign 不能用新字节续跑（会被明确拒绝）——
  用户那个 BLOCKED campaign 的恢复路径是 `evidence\ab_campaign_v0_sources.zip` 里的旧 runner（SHA 正是绑定的 `7355d5be…`）。
- **原生回放（本段为旧探针）**：引擎在启动时重写 `preferences.ini`，启动前预置 `allowGameRecording:true` 被丢弃，
  那次探针无 `.replay` 产出。后续 vNext 实验直接启用 recorder 已产出原生文件，但播放从 frame 6452 起 checksum 不一致；
  因此当前仍未交付可用 replay，正式代码也已撤回实验入口。以本文件“当前任务”的结论与 `evidence/world_model_v0.txt` 为准。
- 下一步如涉及 N、地图、统计口径、策略语义、self-play 或 seed 写入，先交接裁决；不将无原生终局的 n=0 解释为谁赢或谁更好。

## 已完成任务：固定条件 A/B 对比 v0 = **独立验收通过（ACCEPT，E2+E3）**

2026-09-27 的 `助手交接\Codex_AB_v0_现在可以开始_2026-09-27.md` 授权范围已完成。`tools/run_headless.py` 新增 `--profiles` 与 `--profile-order AB|BA`，仅用于两实例并行 match；JSON 白名单 v0 只接受既有 `rwagent.mobileUnitHardCap` 整数 24..80。每个 episode/batch 写 profile 规范化内容与 digest、实际客户端 JVM 属性、固定条件、原生 seed、原始报告 SHA/session，battle_config 必须回显 cap。只读 `tools/aggregate_ab.py` 跨批校验候选 contentDigest、地图/难度/倍速/时限/迷雾、AB/BA 交换、双实例证明、暂存 JAR 和 battle 原始报告，输出逐局索引及每 profile 指标分布，无自动优劣裁决。

- 两批真实原生 match：AB 批 `run-20260927T100638-0e98c0`（ep1=cap32、ep2=cap40）；BA 批 `run-20260927T100850-0811b5`（ep1=cap40、ep2=cap32）。四局均 `PASS/VICTORY`，两批 `parallelProof=PASS`，4/4 battle 报告通过聚合器 SHA/session/JAR/运行条件/实际 cap 复核。实际 seed 四局不同，`seedReproducibilityVerified=false`。
- 每组 n=2：cap32 平均 battle 游戏秒 316.952、newCombatUnits 16、ownLosses 14.5、accepted-order spend 17450；cap40 分别为 269.240、14、11、15500。只是描述性指标，不能据此裁决或调策略。
- 安装位置无 profile 双实例 smoke 2/2 PASS、proof PASS；对其完成目录 `--reap` 连续两次 PASS/killed=0。改动前后完整回归与 JDK 17 候选谱系均已核对。证据 `助手交接\evidence\ab_comparison_v0.txt`、`ab_comparison_v0.json`、`ab_comparison_v0_runs.zip`、`ab_comparison_v0_sources.zip`，交接 `助手交接\HANDOFF_Codex_AB_v0_2026-09-27.md`。
- 剩余边界：四局随机状态不同、每组仅两局；`--reap` 依赖 Windows 查询且跳过无法证明身份的外部进程，需读 `skipped`；保留原有启动后登记前的短窗口风险。策略语义未改，下一步是独立复核与是否扩大样本的裁决。
- **DeepSeek 独立验收（2026-09-27，判定 ACCEPT）**：用自写 `_analysis\ab_v0_audit.py` 从两批**原始目录**重推——
  profile → 客户端 JVM → **游戏内 `battle_config`** 三者一致；引擎命令行**无** profile 参数；AB/BA 位置确实交换；
  两批固定条件逐字段相同；报告 SHA/session/种类与 parallel proof 全部通过。
  另用 `_analysis\ab_aggregate_rejection_test.py` 的 7 个变异样本验证聚合器**两档拒绝语义**
  （跨批/身份不一致 → exit 2 硬拒绝；单份报告损坏或被追加外来 session → 结果内标 `INVALID` + 具名理由并排除出指标，
  **不静默混算**）。**我自跑的最小 campaign**（AB+BA、240 游戏秒预算）四局全 PARTIAL/ONGOING，
  聚合器按设计记为 `partial=2`/臂、指标 `n=0`、分母保留——**PARTIAL 不是编排失败**。
  当前树完整回归 **17 Java + 11 套 Python，failed steps = 0**（我重跑）。谱系：安装件 == dist == 源码
  （whole `efd51831…`、contentDigest `6fec7a0c…`），冻结件 `0681b4f7…`、game-lib `8a550a37…` 未变。
  证据 `助手交接\evidence\ab_v0_ds_acceptance.txt`、`助手交接\对话47.txt`。
- **两条独立发现及本轮处理**：① **迁址陷阱**——DeepSeek 的变异夹具在复制 run 时还改写 raw 中绝对路径，因此 raw SHA 才失配；单纯字节不变的复制不会改变 SHA，但聚合器仍会因绝对路径身份不符而拒收或标 INVALID。本轮 campaign 在原目录原位引用，**验收看 `validBattleReports`/`invalidOrMissingReports`/`n`，不只看退出码**。② **磁盘成本**——原来 `stage()` 因 Windows 无符号链接权限而复制资源，DeepSeek 测得每个 run 约 76 MB；本轮先复制一份 campaign 专用缓存，再将每局三目录 junction 到缓存，并设空间预算，安装版 N=2 实占见上。

## 已完成任务：2-instance parallel runner v0 = DONE（原生无头验收）

2026-09-27 的 `助手交接\Codex_现在可以开始_2026-09-27.md` 已授权并限定此轮范围。`tools/run_headless.py --episodes 2 --parallel-pair` 已实现恰好两个独立实例，默认 `--episodes 2` 仍顺序执行。

- 任务边界（ChatGPT 预设）：只做「两实例完全隔离且可重复跑」的基础设施 ——
  `port / reportDir / session / lock / workingDir / 进程生命周期 / 失败清理 / 证据自证`；
  **先 2 实例**，不直接扩 4 实例，不直接做完整 self-play。
- 验收（无桌面实机）：双实例 smoke 与 suite 均为 2/2 PASS；两进程同时存活并各自推进帧，端口、会话、工作目录、报告目录和锁路径不同；suite 两边均生成 `economy.lock` 和各自会话的原始报告。故障注入强制结束 A 后，A=FAIL、B 的 suite=PASS，两个引擎均无残留；下一批双实例 suite 再次 2/2 PASS；安装位置的运行器和 JAR 又完成 2/2 smoke。可复核证据见 `助手交接\evidence\parallel_runner_v0.txt` 和 `parallel_runner_v0_runs.zip`。
- 边界：这是同机两局独立无头实验，不是同一局双 Agent 或 self-play；未对桌面游戏做双开验收。JDK 13 回归构建与 JDK 17 交付构建的内容摘要不同，交付件使用后者并已核对为既定 `6fec7a0c…`。
- **DeepSeek 独立验收（2026-09-27，判定 ACCEPT）**：我自写 `_analysis/parallel_pair_audit.py` 重新推导不变量，
  对我**新跑的 match 模式长负载**（900 游戏秒 ×2 并发、当前安装件，两局 PASS 且都 VICTORY、退出码 0）
  与 **Codex 归档的 4 个运行目录**（N/K/R/I）逐份复核：port/session/workDirectory/reportDirectory/lockPath
  两两不同、屏障采样窗内两引擎各自 frame 前进、每份原始报告只含本 episode 的 session 且 SHA 与登记一致、
  记录 PID 已消失、端口已释放、遗留 `economy.lock` 均可立即取得 OS 锁。**5/5 判 ACCEPT**；
  宿主完整回归 17 Java + 10 套 Python、failed steps = 0（确认 Codex 沙箱那 16 项 loopback 失败属执行环境）。
  证据 `助手交接\evidence\parallel_runner_v0_ds_acceptance.txt`。
- **强杀残留：已修（`orchestrator crash recovery v0`，2026-09-27）**。上一轮实测到"杀掉 Python 编排器后
  两个引擎仍存活、仍占端口、继续推进模拟"；本轮加了最小恢复机制：
  编排器在 run 目录写 **`live-claims.json`**（角色/PID/端口/工作目录/episode/session/state，进程创建与退出时更新，原子写），
  并新增 **`tools\run_headless.py --reap <run 目录>`**：只读这份登记簿、**逐条复核命令行标记与端口归属**后才
  `taskkill`，之后复核 PID 消失与端口释放，重复执行幂等（第二次 killed=0 仍 PASS），产物 `reap-report.json`。
  端到端实测：强杀编排器后 A/B 两个引擎均存活 → `reap` `killed=2`（正好两个引擎）、两个 PID 消失、两个端口释放 →
  再 reap 一次 `killed=0` → 紧接着新批次 `smoke --episodes 2 --parallel-pair` **2/2 PASS、exit 0**。
  证据 `助手交接\evidence\crash_recovery_v0.txt`；**该轮**回归 17 Java + 10 套 Python failed 0，
  `test_headless_parallel` 7 → **12** 用例（后来随 A/B v0 增至 **22** 用例、Python 共 **11** 套；本轮套数见最新回归）。
  **剩余风险**：① 引擎刚 Popen 就被杀可能少登一条记录（最坏漏清理，不会误杀；reap 报告会列出端口未释放）；
  ② `--reap` 依赖 Windows PowerShell 查询，查不到身份时**一律跳过而不杀**；③ 端口 TOCTOU 窗口仍在（见上一条）。
  `Ctrl+C` 路径本身正常（`stop.request` + `force_stop`，既有用例覆盖）。
- 已完成、勿重问的路线修正：上一轮"v1b 用 income/spend **EMA**"的方案**已被证据否掉**
  （短窗余额读数被"批量下单 / 逐件扣款"节奏污染），v1b 改为显式实测模型；
  `demand-active` 代理也已在 Q1 复算中否掉（几乎恒为 100%，且增量来自方向相反的信号）。

## `Economy v0`（矿上限降级为最低基线）= DONE_AND_LIVE_VALIDATED（巨岛实机，v0.1）。

- **实机结果（E4，`1663738e…`，巨岛 981s）**：终局矿数 **3 → 6**，`minesBeyondFloor = 4`，未动资源点 **10 → 4**；矿数轨迹 `1→2(180s)→3(240s)→4(600s)→4(840s)→5(900s)`，**不再在 240 秒冻死**。证据 `助手交接\evidence\economy_v0_live.txt`。
- **门槛都在真正参与判断**（越基线拒绝分布）：`NO_VISIBLE_LEGAL_SITE 26`（窗口内无合法点位 → 去探路）、`NO_SURPLUS 13`、`BUILDER_NEEDED_FOR_FACTORY 5`、`SITE_UNSAFE 2`、`NO_BUILDER 3`。四次越基线下单余额 2996 / 13586 / 14175 / 15464；探路 6 次，距离 689～1798（远超 ±600 窗口）。
- **产量下降的归因已核查，不是劣化**：`newCombatUnits` 54（v0 之前 57～74），但按项目自身评分（c_tank 1 / heavyTank 3）加权战力为 **134**（heavyTank 40 + c_tank 14），**高于** v0 之前的 123（33+24）；军队中位 35 处观测区间上沿；第二座厂照常建成；`production_idle` 是四局最低。未发现与开矿有因果关系的生产劣化。
- **验收四条**：① 矿数 > 3 且 240s 后仍增长 —— **通过**；② 余额不再堆积 —— **部分通过**（终局 16365→11720，−28%，但 P90 仍 14108：**3 座额外矿只花 2100，矿太便宜，吸收不了 1.5 万盈余**）；③ 军队/第二座厂不劣化 —— 通过；④ 每次扩张/拒绝都有原因 —— 通过。
- **②的结论指向下一步**：后期大额盈余需要 Economy v1/v2（升矿/升厂/新厂触发/intentional banking），本版没有顺手加这些。
- **第一版为何没生效（保留记录）**：`/expansion/plan` 只在建造者 ±600 内找点位，而窗口内那唯一的资源格正是**我方矿自己占着的那一格**（409 诊断 `visibleResourceCandidates=1, nativeRejected=1`）；真正未开的资源点在 1122～1902 单位外。基线以下靠 `prospectForResource`（走向记忆资源点）才能开满 3 矿，而第一版把它写成了**只在基线以下探路**，导致基线以上结构性走不出去——45 / 49 次探测全部 `NO_VISIBLE_LEGAL_SITE`。
- **进代码的未核验常数 = 0**：价格全部来自引擎实测（`extractorCost` / 生产通道学到的单位价），安全复用 `nearRememberedThreat` 与 `prospectForResource` 自带过滤。`星星版铁锈机制库` 把经济回本全部标为"待实测/待复现"，回本数字一律不进代码。
- **本版明确不做**：升矿、升厂、新厂触发条件、多建造者、收入率 EMA、intentional banking（属 Economy v1/v2）。

- **裁决与依据**：`助手交接\经济方向裁决_DeepSeek.md`；取证 `助手交接\evidence\economy_baseline.txt`（`_analysis/economy_current_state.py`）。三局长局：矿数在约 240 游戏秒冻结在 3，侦察视野中已有 **8～12 个资源点**却只建了 2 座矿，终局余额 **16365**；达到上限后旧代码**再也不会调用 `/expansion/plan`**，所以"还有没有矿位"根本没有数据。
- **语义变更**：`mineTarget`（默认 3）从"经济终点"改为**最低恢复基线**。基线以下照旧无条件扩张（开局与恢复行为未变）；基线以上继续向引擎询问合法点位，可开第 4、5……座矿，但必须过两道判据：`SITE_UNSAFE`（记忆威胁在点位旁）、`NO_SURPLUS`（付完矿价后买不起一个首选生产单位）。单位价格未知时记 `UNKNOWN_UNIT_COST` 并拒绝——用 `cost>0` 守卫会静默取消盈余判定。
- **进代码的未核验常数 = 0**：矿价来自引擎 `/expansion/plan` 的 `extractorCost`，安全复用 `nearRememberedThreat`，盈余复用生产通道学到的 `lastPreferredUnitCost`。`星星版铁锈机制库` 把经济回本全部标为"待实测/待复现"，因此回本数字一律不进代码。
- **本版明确不做**：升矿、升厂、新厂触发条件、多建造者、收入率 EMA、intentional banking（属 Economy v1/v2）。
- **实现期间抓到并修掉的真回归**：基线以上的探测原本与建厂通道**共用** `lastExpansionAttempt` 限速，于是每次被拒的矿探测都会重置建厂时钟、把第二座厂饿死；现改用独立的 `lastAboveFloorProbeAt`。另外删掉了一道不可达门槛（`builderReserve` 只在建造者缺失时非 0，而那时 `findBuilder` 已先拒绝）——留着等于虚报保护。
- **验收（必须肉眼可见）**：① 长局完成矿数 > 3，240s 后矿数仍会增长；② 终局余额与 P90 明显低于基线（对照 16365 / 10790）；③ 军队规模、`newCombatUnits`、第二座厂不劣于基线；④ 每次越基线扩张与每次拒绝都有事件与原因。
- **E4 已完成（对话40/41 两轮实机）**：本项的验收局是候选 `1663738e…` 的巨岛 981 游戏秒那一局
  （数据见本节的实机结果段与 `助手交接\evidence\economy_v0_live.txt`）。之后几局（含巨岛 / 冰岛两局）
  的矿数为 4~6，与"基线不再冻结"一致，但那些属于 v1a / v1b 的候选，**不并入本项验收**。

## 已完成（归档）

- **Mainline 第一项 `NO_PROGRESS_TARGET_HANDLING v0` = DONE_AND_LIVE_VALIDATED**（巨岛实机）：最长"持续接战但目标血量不动"从基线 **276.7 游戏秒** 收敛到 **21.7 秒**；实机暴露的 `stalledMs` 计时缺陷已修并用变异测试证明回归用例有效。证据 E51–E54。
- **明确不做（v0 边界）**：不判断目标是否**真的**打不到（那需要 reachability 语义，属 Lab）、不处理跨海/跨域不可达、不改变跨域能力。

## P0 收官记录（已归档）

- **P0 = CLOSED_AND_ARCHIVED（2026-09-26，两局实机 PASS）**；Reachability = DIAGNOSTIC_LAB。

- **收官依据（E4 实机回归）**：`a820d9ad`（巨岛，22 批次）+ `fe3aa2bb`（湖陆，12 批次），候选 `6e001729…`，两局 `PASS / VICTORY`。六条验收全过，证据 `助手交接\evidence\live_smoke_acceptance.txt`（E50，可复跑 `python _analysis\smoke_acceptance.py`）：
  - ① first representative 采样后 0.6s 内消失：**0/22** 与 **1/12**（事故 14/14，Δ=0.511–0.546s）。
  - ② 请求失败 **0**，目标 id 采样后仍被观测到 **12/12** 与 **10/10**（事故 14/14 永不再现 + 同批第二请求 400）。
  - ③ 死前满血且无挨打迹象的死亡：**两局全部分组都是 0**；所有死亡都发生在 **≤40% 血量**（死亡时 HP 占比上限 0.399）。同一脚本跑事故样本得到 **9/14**（first rep）对 **0/35**（未被采样），信号极强。
  - ④ registry：实机报告无 registry 计数器，只有间接证据——每个采样响应 `notAllowedSkipped = 0`、`SKIPPED_NOT_ALLOWLISTED = 0`，即 live 路径**没有尝试**任何白名单外反射调用。**不宣称**已测到世界单位总数绝对值。
  - ⑤ observation 中 `hp <= 0` 的存活单位 **0 条**；采样响应 `fullFloatDump` 里 `am.cu = 210.0`（正常值，非 -1）。
  - ⑥ `liveMatchSafe=True`、`groupsEnabled=[A_FIELD_READS]`、`attackerMovementClassSource=GROUP_B_DISABLED`、`collisionRadiusRaw.readVia=Field.get`，guard 计数全部自洽。
- **③ 的判据演进（重要，别再用被淘汰的粗判据）**：`own_loss` 在事故样本里 **14/14 都有**（引擎按真实死亡登记）→ 零区分力；"死前三次观测 HP 没变"会把**残血待毙**算进来（湖陆局那两例实际是 9/600 与 47.3/600）→ 假信号。有效判据是**死亡时 hp/maxHp ≥ 0.8 且死前无挨打迹象**，并按"是否被采样"给基准率。
- 参数自证：`mobileUnitHardCap=40`、`gameSecondsPerWallSecond=1.0`、采样开关 `enabled/30s/limit=3`、`agentJarSha256=6e001729…`；地图名与 AI 难度无字段，按用户提供记录。
- **反停滞计数器从本轮回零**：P0 收官这一轮算起点，之后每 3～5 个 GREEN 迭代必须产出 A/B/C 之一（见下）。

- **P0 事故根因（已判定，已收官留档）**：端点用 `rawMethod(am.class,"cj")` 去"读碰撞半径字段"，但 `am` 上同时有 **字段 `cj`** 与 **`public void cj()`**，按名解析命中的是后者；该方法**全部字节码就是 `cu = -1`**（`ldc -1.0f; putfield cu`），而 `cu` 就是引擎 hp 与桥接导出的 `hp`。因此每次采样都把被探测对象置死：`cu` 立刻 -1，下一次 observation（约 +0.52 游戏秒）登记消失，同批第二个请求随后得到 400 `target unit not found`。正式状态 `AM_CJ_VOID_MUTATOR = CONFIRMED_ROOT_CAUSE_OF_DIAGNOSTIC_KILL`。**不再继续解释旧 `0beaf504` 的每一层内部 tick（输出30 §2）。**
- **第二个独立缺陷**：`movementObject()` 扫描含工厂访问器 `ar.a()`（每次调用创建并注册新单位，registry +1）。正式状态 `AR_A_FACTORY_ACCESSOR = CONFIRMED_DIAGNOSTIC_STATE_MUTATION`（**不是**死亡根因）。两者均已从 live-safe path 移除。
- **已施工的修复**：① `collisionRadiusRaw` 改为字段读取（自证 `readVia=Field.get`）；② `rawInvoke()` 加**显式白名单**——白名单 = `owner#name -> {真实参数化 descriptor -> 用途}`，`isAllowlisted()` 精确比较参数类型+返回类型；沿具体类父类链匹配但**只认本身已在白名单里的 owner**，未放行一律 `SKIPPED_NOT_ALLOWLISTED`，`void`/工厂拒绝作为第二道保险；③ 默认只跑 Group A，B/C/D/E 与 `&stages=` 均需进程级 `-Drwagent.reachabilityDiagnostics=true`（未 arm 时 400）；④ 跳过计数写入 payload（`reflectionGuards.voidMethodsSkipped / factoryAccessorsSkipped / notAllowedSkipped / counterScope / notAllowedSignatures`），且**序列化在响应末尾**，保证计数覆盖同一响应内的全部拒绝。
- **白名单修复史（重要，勿重犯）**：Astra 发现"口径写了 descriptor 匹配、实现只按 owner#name 匹配"属实。第 1 版修复把父类链匹配整条删掉 → **功能回归**：`getMethod("h")` 解析到子类声明 `…game.units.e.j#h`，`am#h` 条目永不命中，`attackerMovementClass=null` / source=`FIELD_SCAN_FALLBACK`。第 2 版改为"父类链只认已审计 owner"并实测恢复（`LAND` / `y.h() via y#h`）。证据：`助手交接\evidence\allowlist_verification.txt`（可复跑：`python _analysis\allowlist_verification.py`）。
- **祖先 owner 白名单的正式定性（输出30 §1.2，勿拔高）**：`movement-class 功能回归 = 已修复`；`祖先 owner 白名单 = 仍有未审计 override 风险`。签名相同不等于实现相同——子类 override 祖先的同名同签名方法时，当前实现会因祖先条目而放行，而子类实现未审计。**在补齐之前，不得把"祖先 owner 已审计"表述成"实际执行实现已审计"。** 该风险登记在 Diagnostic Lab（见下），**不阻塞**当前默认 live-safe smoke，但**若后续要把 B 组开到真实比赛，必须先关闭该风险**。
- **CURRENT_SAFE_PATH 正式口径（输出30 §1.1 已确认冻结）**：`ENGINE_METHOD_CALLS = { r(), i(), bI(), cW() }`（桥接代码的直接 Java 调用，不经反射）；`FIELD_READS = cj/eo/ep/eh/bX/cu/cv + declared floats`；`REMOVED = am.cj(), ar.a()`。**永久废弃 "METHOD CALLS = none"**。
- **Lab 冻结项**：`C = INCONCLUSIVE_FIXTURE_LIMITATION`；`k.l.a(k.i,boolean) → k.i.k` = `KNOWN_STATE_WRITE_SUSPECT`（非根因）；capability 映射 UNRESOLVED；**祖先 owner 白名单 override 风险 = UNAUDITED_OVERRIDE_RISK**（输出30 §1.2；关闭方式：① 审计并显式列出真实 `declaringClass + name + descriptor`，或 ② 对借祖先条目放行的 override 做实现级证明）。**这些不再阻塞 Mainline**，但 ①/② 之一未完成前不得把 B 组开到真实比赛。
- **归档**：`0beaf504 = QUARANTINED`；`56ba2eff = INCIDENT_FORENSIC_SAMPLE`。
- **禁止**：在真实比赛对象上调用 `aq.a(y,am)` / `aq.b(y,am)`、任何 `void` 方法、任何工厂访问器、任何不在白名单内的反射方法；把 raw boolean 当 targetable/reachable/engageable。
- 未施工：`TARGET_REACHABILITY_GUARD`、`LOW_VALUE_TARGET_PURSUIT_LIMIT`、分队、海军、登陆、动态 cap、48
- **AI 难度仍是"用户提供"**：报告只能自证 speed / cap / game duration / sampling config / candidate provenance，**不推断难度**
- **证据等级（输出28 §十）**：E0 观察 / E1 单条日志 / E2 可复跑脚本证据 / E3 独立交叉核对 / E4 实机回归。P0 需要 E3+E4；普通 bug E2+回归即可。

## 未决/挂起 backlog（只记录）

- **反停滞硬规则（输出30 §3）**：每 3～5 个 GREEN 工程迭代，至少产出一种**可见进展** —— A. 新的肉眼可见 Agent 行为；B. 新的运行规模能力（2x / 并发 / self-play）；C. 同条件下可复算的统计改善。**只有日志/文档/字段/forensic/审计覆盖增加，不计为能力进展**（RED 事故处理除外）。连续 3～5 个 GREEN 迭代没有 A/B/C → 自动升级 YELLOW，停止扩审计、收束主线。工程时间倾向：Mainline ≈70% / 自动化与高速并行自对弈基础设施 ≈20% / Diagnostic Lab ≈10%。
- **MAINLINE（输出30 §4/§5）**：**① `NO_PROGRESS_TARGET_HANDLING v0` —— DONE_AND_LIVE_VALIDATED（E4，巨岛实机：最长停滞 276.7 → 21.7 游戏秒）**；② 随后从"修理/重建"与"长期经济/投资协调"中**择一**（不同时铺两个大系统）；③ 2x speed smoke（再决定 4x）→ 2-instance parallel runner v0 ✅（本轮完成；4-instance 待裁决）→ 限制规则 self-play（固定地图/位置/设置/单位，Agent-0 vs Agent-1）→ 批量自对弈 + 参数比较/自动调参。高速先验证 game-time/wall-time、决策频率、命令漏失与 APM/反应约束；并行先保证 port/reportDir/session/lock/workingDir 完全隔离，2 局不串数据再扩 4 局。**多人联机暂不作为近期主线**（`network games are not supported` 不简单解除）。
- **能力路线（输出30 §5，防停滞用，非死日期）**：P0 smoke closure ✅ → `NO_PROGRESS_TARGET_HANDLING v0` ✅（E4 已收）→ 修理/重建 或 长期经济（择一）→ 2x speed smoke（再决定 4x）→ 2-instance parallel runner v0 ✅（本轮完成；4-instance 待裁决）→ 限制规则 self-play（固定地图/位置/设置/单位，Agent-0 vs Agent-1）→ 批量自对弈 + 参数比较/自动调参。高速先验证 game-time/wall-time、决策频率、命令漏失与 APM/反应约束；并行先保证 port/reportDir/session/lock/workingDir 完全隔离，2 局不串数据再扩 4 局。**多人联机暂不作为近期主线**（`network games are not supported` 不简单解除）。
- **DIAGNOSTIC LAB（不再阻塞 Mainline）**：Reachability B/C/D/E、`aq` 语义、movement class、武器/目标语义、cross-domain 底层可达性、**祖先 owner override 风险**、Astra 剩余审计项 (3)(4)(5)（冻结语义 vs 测试覆盖、全 JAR 零参 void/工厂普查、独立复算事故证据链）
- `TARGET_REACHABILITY_GUARD` v0（等 Lab 结论）
- `Cross-Domain Recovery v0`（判定定义五条；真实样本 `89b254f3`，标签 `CROSS_DOMAIN_UNRESOLVED` / `GROUND_ENGAGEMENT_UNREACHABLE_SUSPECTED`）
- `LOW_VALUE_TARGET_PURSUIT_LIMIT`
- Unit Role / Task Group Manager（MAIN_FORCE / LOCAL_GUARD / PURSUIT_DETACHMENT / WOUNDED_RESERVE / BUILDER_ESCORT）
- Budget / Investment Manager（income-rate estimate、hard reserves、committed spending、intentional banking、各分配项）
- 矿升级候选（无新矿点 + 军队充足/触顶 + 储备安全 → T1→T2；T1 枯竭 → 考虑 T2→T3）
- capability air/ground 映射 = **UNRESOLVED**（`FIRST_VISIBLE_AIRCRAFT` 已取 live sample；候选布尔字段不足以区分 air/ground；暂不投入）

## 下一次验证

- **下次桌面实机验证的对象**：P0 与 `NO_PROGRESS_TARGET_HANDLING v0` **均已 E4 收官**（后者：最长"持续接战但血量不动" 276.7 → 21.7 游戏秒），
  不再是待验收项。**历史** `6fec7a0c…`（v1b.1，含 Q2 承诺语义 + Q3 显示精度）未单独获 E4；**当前** `23e6f0fb…` 也尚无用户桌面实机 E4。
  World Model v0 的隔离原生 `PARTIAL/ONGOING` 只证明接口运行；本轮 Recon v0 的隔离原生 `PASS/VICTORY` 证明合法行为链与原生结算发生，但仍不等于桌面实机验收或策略增益证明。v1b 部分的 `73a09229…` 两局不能算作当前候选的验收；需要时由用户再打一局。
  条件与既往一致：巨岛 / 非常困难（用户提供）/ 1x 或 5x / 正常迷雾 / cap=40；正常退出让 Match 控制台收尾。
- **smoke 期间的条件（供下次复现）**：巨岛 / 湖陆，非常困难（用户提供），1x，正常迷雾，cap=40；正常退出让 Match 控制台收尾（`finally → ReportFiles.finish(...)` 才 commit）。
- **回归口径**：`developer\test-win.ps1`，当前 Java 17 项 + Python **13** 套，failed steps = 0 才算通过；改动源码后必须重跑并把 JDK 17 交付 `dist` 复制到 `P1F-冒烟环境`。
