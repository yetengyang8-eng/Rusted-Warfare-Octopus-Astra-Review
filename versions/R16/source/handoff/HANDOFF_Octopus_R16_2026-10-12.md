# R16：陆军局部接战力量 / 保留远端 Builder

**状态：工程候选，不作为已验收稳定升级。** 完整回归最终组合 139/139 通过，但同参数自然对照 R15 约45:32胜利，R16 约60:14仍ONGOING；本轮没有证明整体更强。保留这条负面证据，不得用focused或未改Air源码替代实战验收。

- 分支 `codex/octopus-g3-execution-20261002`，起点 `1311f2a`，产品 `9d9d294`，交付文档HEAD见Git。
- 备份标签 `backup/r15-before-r16-20261012`；R15人工环境完整保留。
- 本轮只看用户第一局 `20261012-013746-24200`；第二局 `20261012-015115-11320` 没有分析。
- 用户两个launcher改动及backup三个dirty输入SHA保持；未push、未deploy、未操作桌面游戏。

## 确认的原因与修复

1. **名册兵力被当成同时接战力量。** 第一局 raw line181148/181152，Game33:28.337，General1对基地283下达CURRENT_VISIBLE_TARGET。后续trace181189-181190按9名成员、4046.697HP准入；最近own关键帧比命令早2.740秒，4辆在西南，另外5辆仍在出生基地一带，相隔数千world。不能把这个近邻关键帧伪装成同帧世界，但足以与源码“全名册汇总”对应。R12至R15的GeneralCombatDirector/TacticalOpportunity/FightEstimate/BaseThreatZones没有直接改动，不能声称已经证明是R15空军改了共享威胁参数。
2. **普通frontier可以绕过当前基地接战评估。** 原路径在非cautious时直接通过，基地记忆的>11人放行也使用全名册。新路径在目标/探索两处检查实际actor经过的已知基地圆；用前部附近400半径内的实际成员评估基地，复用既有阈值、native eligibility、UNKNOWN fallback。不足则原有MOVE离开火力圈/聚拢，后帧条件成立仍可攻城。没有修改共享FightEstimate、Air或调度额度。
3. **Builder任务完成空档被当成本土闲置。** 第一局26条repair/capital召回意图，其中23条能匹配同一次观测的native accepted回家MOVE；资本召回24条，修复召回2条。现在远端Builder保留区域职责，不因prospect清空/矿点暂时不可见就被普通本土施工征调；native repair等活动也不当成capital闲置。实际危险撤退路径保持。

契约：[OCTOPUS_R16_GROUND_APPROACH.md](../docs/OCTOPUS_R16_GROUND_APPROACH.md)。新增 `baseApproach` trace解释名册、实际近邻、基地与准入理由。

## 验证

- 修改前R9攻城130 checks、Builder20、R15Air116通过。
- 新R16Ground10、Builder6在R15旧编译产物上分别复现失败，修改后通过。R9连续攻城/ABA/真撤退130、R8准入15、G5Combat57、Builder20、G6资本34、R15Air116保持。
- 完整Windows一次矩阵：初轮138/139。唯一失败G55旧用例要求跨图召回Builder，与此次用户方向相反；改为远端保留 + 新本土工人完成repair。以**同一产品JAR**编译/重跑该用例21checks通过。最终 **PASS_COMBINED_MATRIX 139/139，104 Java harness、32 Python suite /509tests，failed steps:0**。初次失败保留；不声称首轮全绿。
- 原生fixtures包括R15AirNative、R14重炮、Strategy/terrain等在完整矩阵通过。新接战/区域用例是fixture；真实部队位置不由fixture伪造。
- AirGeneralController、AirResponsePolicy、FightEstimate、CombatProfiles、TargetCatalog及内部类共14个class与冻结R15逐字节相同。这证明隔离了代码变更，**不证明空军自然表现等价**。

## Spain自然对照（并行、普通difficulty0 / player5 /5倍）

两把均原版地图、无位置改写、无人介入、各自独立原生过程，没有固定随机种子，不能用一对宣称因果改善。

| 项目 | R15 对照 | R16 候选 |
|---|---:|---:|
| 结果 | 原生VICTORY | 3600秒预算结束，PARTIAL/ONGOING |
| 最后完整own样本Game | 45:32.240 | 60:14.336 |
| ready矿 | 44 | 37 |
| ready陆厂 / 重坦 | 9 /108 | 4 /97 |
| ready两栖 | 128 | 305 |
| ready修复湾 | 3 | 5 |
| 屯蛋召回事件 | 15 | 0 |
| 修复召回事件 | 2 | 3 |
| Air攻击accepted事件 | 135 | 29 |

R16继续生产、扩矿、修复和Air增长；没有宏观停摆。但Air清理效率低于本次对照，不能靠“源码未改”忽略这个结果。修复召回3条仍可能来自基地半径内的就近回位；本轮没有承诺消灭所有回家行为。两局长度不同，上述计数不作为APM/损失率/战斗交换对比。

完整原始验证目录：`G:\deepseek 工作台\_validation\octopus-r16-ground-20261012`。自然子目录 `spain-r15-normal-p5-v2`、`spain-r16-normal-p5`。初次R15路径写错在复制JAR阶段退出、未启动引擎；保留失败目录，不计为对局。证据及source hash：[evidence](../evidence/octopus-r16-ground-2026-10-12/validation-summary.json)。

## 人工候选

`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-12-R16`

`1-Start-Game.bat` → 手动Spain出生点5 → `2-Run-Agent-7200.bat`。port47707，G6开启，原生Game/Frame默认显示；Agent sample单列。未启动。

SHA256：`3a1eeb33725c93279818e90a9812e21bfb0bbf04f079407143835054d8ee8ad5`。

## 下一断点

R15仍为稳定参照。R16先做有针对性的人工对比：基地前部分兵是否停止借远方兵力冲塔、真实聚拢是否恢复攻城、Builder是否继续当地施工。若新版更拖沓，优先检查本次基地接近约束是否过度影响推进，以及地面任务对Air区域责任的间接影响，不能继续抬高保守阈值或改Air掩盖它。

自然R16的单队大量两栖/清剿较慢仍NEEDS_EVIDENCE，本轮未追改成熟Air主循环。远端彻底无工可做时的明确再部署、本土缺工补充速度也留作观察。不把未结束局标为胜利；不从own缺席/HP差分编造击杀。
