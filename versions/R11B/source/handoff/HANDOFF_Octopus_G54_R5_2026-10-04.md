# G5.4 R5：R4F 执行活锁修复交接

分支 `codex/octopus-g3-execution-20261002`，从失败候选文档 HEAD `a96178c` 出发，产品提交 **`32eab8e`**；最终文档 HEAD 以 Git 为准。本轮是可运行的执行/恢复修正候选，**不是所有实战问题已解决或真人验收通过**。

## R4F 真人事实及根因

原始完整输入：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G53-2026-10-04-R4F\runs\20261004-210836-25444\rw-agent-reports\battle-1791119318237-1e59b983.jsonl.gz`，SHA `849b2c468e7a95d1599921f1b298e25feb66371b71a43a8528f7b20b523c5a34`；242625 条记录，0 malformed。结尾连接超时、比赛 ONGOING，不推断超时原因。详查保存在 `G:\deepseek 工作台\_validation\octopus-r4-feedback-20261004` 和本轮 `human-r4f-input.json`。

- 最后150游戏秒，Recovery接受586次，General4撤退13次，其余General/生产/施工等基本0；约3.99命令/秒吃满4次/秒额度。多个General末期7～10分钟没有接受指令，当前成员无有效 order。Recovery全局占额与逐个重复移动共同造成真实服务饥饿。
- 末期63个恢复成员、9个目标修复湾、实际没有修复湾；规划 affordable 的1500报价存在。409出现 controllable own mobile unit not found。不能把规划/回执/缺席当施工完成、治疗或死亡。
- Builder2752 当前合法矿距57.467，native credits7940.5，仍没有施工；计划和 Prospect 本身不能作为完成工作。
- 工厂升级完成0，技术评估持续 RECOVERY_PROTECTED；有3个T1厂和大量小坦。Recovery被错误当成跨经济、技术的总保护开关。

## 实际修改

1. G5取消全局人为 command quota。保留session/frame、owner/generation/ABA、同actor冲突、重复/有效order复用、资金承诺、producer/military slots与ghost保护。force仍真正collect/priority/dispatch；经济/Strategy/生产仍immediate，不能称全lane全局批调度。未提高decision frequency。
2. Recovery复用后帧真实MOVE，没有湾时一次回家等待；真正湾出现后再接近。409缓存当前负面证据，变化才重试，不宣称单位已死。实际后帧HP≥75%才FREE。
3. 修复施工可以短借无BuildJob的baselineBuilder；复用现有supportReserve选一个真实原生修复项目筹资，保护该报价而不是笼统保护“整个恢复期”。保留既有60秒收入筹资/90秒超时窗口，需求/站点失效等释放，施工接受后转入native spending hold。
4. Spain中间自然局又发现资金反馈锁：Builder原生build会转repair或完工后转下一任务；内部extractor与原生extractorT1也不同。结算改用receipt识别真实product/site，再等后帧唯一新己方建筑或真实进度增长；不同站点不被同Builder的queue歧义一起锁死。重叠/未知仍保留hold；无receipt/现金/时间退款。生产采用实际更新菜单的queue baseline，真实引用报价身份。
5. 普通mixed tank成员不再按精确type丢出frontier；同已知movement domain恢复原mission。已经实际到位的成员不因HP<50%卡路点；ACTIVE残部后帧平静rally可脱离regroup，不永久等补到6。首次FORMING六兵不改。
6. 小规模当前空军继续触发ANTI_AIR，但不自动否定全体地面任务；严重当前暴露/真实恶化交换仍可撤离，全部撤离使用MOVE。当前合法近矿规划同轮尝试施工。
7. 真实资金可负担的升级不再等待全军Recovery/健康六兵，扩厂同样移除全局recovery veto。原版价格/guard/后帧tier仍权威，普通持续生产及组织目标分离保留。

详见 [执行与恢复契约](../docs/OCTOPUS_G54_EXECUTION_FLOW.md)。没有Universal Bridge改动、G6、SearchArea、新General战略类型或随机路线。

## 最终 Spain 自然局：实质进展与明确不足

最终同候选 JAR，原图400×370、5×（实际约4.8×）、difficulty0、本地team0；初始只有commandCenter，builder/军队正常生产，无位置/资金/HP/fog注入。**team0与真人team5不相同，不能作受控AB或胜率改善结论。**

自然运行约3602游戏秒，预算退出 **PARTIAL / ONGOING**，不是FAIL/desync，也不是胜利。原始gzip SHA `039bad9d4d406e816068440d38901479fbf3d9af58ce96c06b59386c6e9f1f14`，79811224 bytes、322818条、0 malformed，来源见 [紧凑自然证据](../evidence/octopus-g54-r5-2026-10-04/spain-natural-summary.json)。

- 原生修复施工实际接受在342.656秒，ready湾在381.936秒的己方采样中出现。**83次真实HP恢复出池、68个不同单位、其中64个后来以位置witness重新ATTACHED。** 不是fixture位置变化或回执模拟治疗。
- 原生升级完成1次，heavyTank在123.536秒的ready采样中出现。但全局queue receipt仅 **3次heavyTank、453次c_tank**；升级入口已通，**重坦主力化明显尚未完成**，低收入下便宜产品持续消耗共享资金仍是下一项真实问题。不能把这条标成已经解决。
- 出现3个General ID，不能据此宣称同时3队；末期当前只有1个General。General1/2/3分别3993/1430/2182次accepted；314个己方单位有超过1000world的实际采样位移。命令数/位移仍不能证明战术有效。
- 最后150游戏秒，General3接受184次、Recovery6次，rule-main28次，JOINING/Recon/LR继续运行；退出时hold数量/金额均0，不再靠伤兵占满全局额度。末期2个ready恢复成员，资金约105，实际MONEY_LIMITED。
- ready采样峰值4矿、3Builder、2工厂；累计报告完成15矿，**不是同时持有15矿**。末期持续丢矿/重建，factoryTarget仍2、没有持续上调，矿升级0。前线保护、占图与持续经济增长仍不足；不得包装为完整经济滚雪球已成立。
- 保留较多STANDOFF/命令、少重坦与低产能等不利证据。legacy summary ownLosses不作为明确死亡或击杀证据，未发明K/D。

中间1800/3600/5400自然局及所有旧Jar/失败输出均留在外部验证目录。它们用于找到queue baseline、施工资金锁和修复筹资缺口，不能冒充最终候选或合成AB数据。

## 验证与交付身份

最终候选 JAR SHA **`524b08232d7037975ad3b328aabde66b8bd9ffcae5370e6a2a94c50ae56230a0`**。它用于最终Spain、最终native和人工kit。

- 最终完整Windows **92/92 PASS，failed steps:0**：58 Java、31 Python /501 tests、3 build。完整原始日志和逐项矩阵见 [validation](../evidence/octopus-g54-r5-2026-10-04/validation-summary.json)。发现修复筹资缺口时仅停止主代理自己的前一回归进程树，原中断目录保留，未算绿灯。
- focused包含G54 94、Scheduler130、Recovery60、Outward37、Tactical67、StrategyEconomic59、Strategy174等；没有用镜像单测代替自然闭环。
- 最终8项原版隔离fixture PASS。G54实际同一observation的64移动actor、4个生产者、1修复施工通过真实HTTP/native.k，随后native move/queue/build witness成立；64actor是原生seed、HP未改，不是自然64伤兵/治疗。G3 credits/slots/ghost、多两栖独立Mode、竞争、ABA/order reuse等保持。
- 原G51native旧fixture要求一架小飞机必然全组STANDOFF，首次失败保留；改为当前小飞机不全组veto、显式较大原生空军＋真实己方HP下降仍正确MOVE撤退，295checks通过。产品未因此再改。
- 最终full独立重建Jar ZIP SHA不同，但全部类/资源entry字节一致（0差异）；产品/knowledge/tools输入一致。245输入中仅上述native fixture在full编译后修正、独立执行最终native覆盖；244项完全匹配。[输入provenance](../evidence/octopus-g54-r5-2026-10-04/validation-input-provenance.json)保留这一差异，未篡改旧失败。

验证根目录：`G:\deepseek 工作台\_validation\octopus-r5-flow-20261004`。完整回归在`final-verification`，早先中断在`release-verification`。

## 新人工入口与下一断点

`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G54-2026-10-04-R5`，port47667：**1-Start-Game → 人工进入Spain → 2-Run-Agent-7200**，另有1200/2400。只准备、未启动；1895文件hash核对一致。见 [人工说明](../docs/OCTOPUS_G54_HUMAN_VALIDATION.md)、[说明书](../docs/AGENT_USER_MANUAL.md)、[候选manifest](../deliveries/octopus-g54-r5-2026-10-04/candidate-manifest.json)。

下一优先从本轮真实“3重坦/453小坦”和矿丢失/重建分析**持续普通购买与技术/主力产品的资金选择**，并看General长期STANDOFF是否拖累矿保护；不能再以加全局quota、提高decision频率、放大armyTarget解决。第二优先降低Builder反复补生产与低价值命令开销。先保持已自然证明的治疗/补员与独立执行，再谈G6。高收入多厂长期并行投资、真人team5收益、较大修复服务规模仍NEEDS_EVIDENCE。

未push/deploy、未启动/关闭/操作用户桌面游戏、未覆盖原版/设置/存档/回放或旧kit。初始用户两个launcher改动与backup原样保留；工作区因此不是全局clean，产品提交已落盘。
