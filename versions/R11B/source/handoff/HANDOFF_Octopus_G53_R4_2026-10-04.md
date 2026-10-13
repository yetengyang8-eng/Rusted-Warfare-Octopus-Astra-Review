# G5.3 R4 持续运营交接

仓库 `G:\deepseek 工作台\GitHub发布\Rusted-Warfare-Agent`，分支 `codex/octopus-g3-execution-20261002`。R3起点 `faa656e`；最终产品 `b09feb9`，交付文档在其后。最终HEAD以Git为准，不能从本文件内自引用commit求SHA。

## 从什么证据出发

主证据是用户 R3 Spain 真人长局：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G52-2026-10-04-R3\runs\20261004-180347-23808\rw-agent-reports\battle-1791108229177-ee4d7029.jsonl.gz`。SHA `6dd70f2ccd5ab55e89e50edb89c7c31f6a6fd289433ffaffb0dab8f58a416aa3`；232214566 bytes，1234365有效记录，0 malformed。同记录refs展开后流式统计，不补恢复compact省略的字段。

约120游戏分钟内，观察到最高375593现金、93 ready armed、2 Builder、4工厂、5矿、4 General。晚期350801现金、58 armed/组织目标96、4个有合法可负担产品的idle producer；150秒窗口利用率约15.66%。因此 quota是真问题，但不是全部停产原因。move尝试23643/queue364，GENERAL budget denied131669，STANDOFF_MOVE_ACCEPTED_NOT_ARRIVED12072。晚期普通生产只189次accepted、Strategy2/Economy8，支持实际服务饥饿而非“不醒”的诊断。以上是记录计数，不是持续时间/击杀/损失归因。

## 施工选择及提交

| Commit | 改动 |
|---|---|
| 56cf61c | 每General持久OutwardMission、native frontier连续消费、稳定STANDOFF与正常推进指令复用；基地圈只保护≤11健康地面编制 |
| 816d3c8 | 当前小ThreatTask先真实FREE/LR coverage，再Commander缺口；允许已有General按当前需求/供应逐步扩编 |
| 4cafcb4 | 普通生产不再armyTarget审批；实际有限token在critical优先后轮转施工/生产/普通兵力；默认软件兵/厂上限移除；原生报价及观察消费速率驱动产能；独立施工域/收入驱动Builder与矿并发 |
| b09feb9 | 同一原生撤退/JOINING目标跨全局敌情变化复用；保留错误order/真实rally/ABA/UNKNOWN期限；新增原版后帧order fixture |
| 062406c | 对齐两个完整回归旧fixture：生产选择提供当前库存；独立B队用后帧真实accepted目标指令witness证明，不再要求重复发令 |

完整接口见 [持续运营契约](../docs/OCTOPUS_G53_CONTINUING_OPERATIONS.md)。没有新全局planner、提高频率、增加token、修改Universal Bridge或激活G6/SearchArea。

## 代码事实与可解释因果

- `GeneralCombatDirector.OutwardMission`记录当前目标/方向、实际receiptframe与任务变化。临时combat/crisis/Commanderhold结束后仍有原推进任务；不是每轮选择自己附近坐标。当前明确native有效order继续用，未知destination仅原boundeddefer。
- `BaseThreatZones`只对当前owned ATTACHED NORMAL ready HP≥50% LAND/HOVER普通成员≤11阻止普通旅行；>11不再由基地圈做完整攻城估值。FightEstimate仍评价当前具体目标，ghost不授权攻击。
- `BattleClient`顺序：当前combat事实→Commander.prepareThreatTasks→Force.prepareLocalResponse→刷新roster→Commander.reconcile。小任务现有范围内兼容FREE先响应，实际部分coverage后General补缺口；不会同轮又把这些FREE补进General。旧pending+LR/clear→JOINING等契约保持。
- `g5_operations_service`记录有限token真实服务；priority≥70 force先执行，之后施工投资/普通生产/普通force轮转。G4force是真collect/arbitrate；其余仍轮到本lane时immediate dispatch，没有伪称全局批裁决。
- `production_capacity_assessment`记录CONTINUOUS policy、organizationTargetIsProductionLimit=false、实际报价/有效资金/槽位/承诺。真实选定builder/investment/capability reserve合计保护，假想昂贵主力或升级不挡便宜合法普通产品。
- `strategy_static_construction_domain`和`strategy_construction_capacity`记录合法静态map域、原生approach、独立工作/总收入/真实Buildercost。固定矿点只支持探索施工需求，动态空闲/安全/可建仍UNKNOWN，实际建设照旧guard与后帧witness。
- 真实生产spend/观察busyfactorytime估计工厂转换能力，避免用策略空闲来要求更多空厂。150秒原窗口和真实资金/报价保护保留，默认5厂上限移除，显式override有效。

## 验证及证据界限

最终Jar SHA `6b6d5089278f2ae3ce28c629e92b8333e224736565d9c70b3fc4887b952ff87c`。完整Windows最终结果、输入hash及kit核对见 [validation-summary.json](../evidence/octopus-g53-r4-2026-10-04/validation-summary.json)；[manifest](../deliveries/octopus-g53-r4-2026-10-04/candidate-manifest.json)标识同一Jar。最终验证目录 `G:\deepseek 工作台\_validation\octopus-r4-snowball-20261004\release-verification`。

完整Windows原始 **89/91，failed steps=2**：57 Java harness、31 Python suites/501 tests、3 build步骤。两个旧fixture修正后，同一Jar补验 G51Operations37checks、G5runtime4tests，**2/2 PASS，有效91/91、未解决失败0**；不是第二次full，也不把原始failedsteps2改写成0。[逐项有效矩阵](../evidence/octopus-g53-r4-2026-10-04/effective-windows-matrix.json)列出每项原始结果与实际补验来源。

输入快照243项，其中241项与当前工程一致；仅上述2个test fixture按新行为修正，产品/knowledge/Jar无漂移。[repair provenance](../evidence/octopus-g53-r4-2026-10-04/fixture-repair-provenance.json)保留原/新hash和日志来源。R4F kit1895个文件hash全部一致。G53 focused核心四套34/52/68/25共179checks已通过完整矩阵。

最终同Jar原版隔离575 checks PASS：G53持续生产15、新order复用47、G51原版272、Closure12、G3多厂168/两栖30/lane31。四厂在48兵/fixture组织目标12下真实独立queue/payment，paidghost不得释放；原版native.k后真实laterframe提供撤退/JOINING move目的地，20秒及远敌变化追加samegoal move为0。单位/HP/位置/fog/clock是显式seed，simulationTicks=0，不能冒充自然到位、治疗、战果或真人胜率。

最终同Jar自然SmallIsland 5×600游戏秒，`PARTIAL / ONGOING`：43个新战斗单位、首次6兵约93.1秒、1次厂升级、897accepted命令、连续exactmove50。mine completion累计3但当前观察峰值2，不能把累计完成当作同时持有；后期普通消费56.7/s高于收入51/s，最后资金受限。自然扩厂、多Builder执行、多General新生及完整经济滚雪球未由该短局证明。

前一中间Jar `290891...`自然短局产生73新兵但531重复move，发现问题后主代理仅停止自身完整回归进程树，保存首轮部分输出，收束上述复用修正。中间full是INTERRUPTED，不算绿灯。两局非受控AB，不能声称某个百分比改善；不用新兵下降或重复move下降替代真人验证。

## 交付与保护

人工kit `G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G53-2026-10-04-R4F`，独立port47665，仅准备未启动。`1-Start-Game.bat`→人工进入Spain→`2-Run-Agent-7200.bat`；1200/2400也有。见 [人工验证入口](../docs/OCTOPUS_G53_HUMAN_VALIDATION.md)。R3及中间R4保持原样。

用户原始dirty保留：`tools/RW-Agent-Octopus-Test.ps1`、`tools/rw-agent-human.py`、`tools/rw-agent-human.py.bak-20261003`，没有stage/reset/clean。kit包含用户human launcher现有修改并逐文件hash；它不是该工具修改的Git提交。只读frozen baseline verifier按预期报告历史产品源码偏离，没有回滚。未push/deploy/操作桌面/修改存档回放设置。

## 下一断点与G6

优先新Spain同设置真人长局：独立General持续方向及任务后恢复、强军基地圈准入后的真实交换、附近FREE低成本骚扰、修复FREE再利用、Builder多域和矿/厂/升级吞吐。保留完整runs+replay+异常game time；先看actual command/queue/busy/spend与已选reserve，不追求APM或频率。

剩余参数是公开政策：11小编制保护，nativefrontier3次候选，75%/140到位消费，局部LR既有3目标/6人/1500；Builder收入30秒规划窗口、产能150秒观察窗口。它们不是原版战力/最佳效率定理，下一真人证据校准，不再加gate套娃。

两栖保持SpecialistPool。当前CapabilityTask集合目标与逐机CapabilityUnitMode、真实Strategy lease/owner可复用；长期空海General适合作为G6方向，但须typed roster、移动域frontier、与CapabilityNeed任务借用/归还协调。不能直接放入普通General或假装requested mode=physical/weapon/terrain。完整CapabilitySearch/A7/A8、空海独立长期编制、SearchArea仍未实现。原版unit cap、动态安全和未观察敌情保持UNKNOWN。局部LR仍首阶HP需求，不宣称精确混编战力模拟。
