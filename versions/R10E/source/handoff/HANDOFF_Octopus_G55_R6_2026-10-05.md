# G5.5 R6：重坦、开局侦察、本土修复候选

分支 `codex/octopus-g3-execution-20261002`；起点 `ac5b773`；产品提交 **bbd98fd**。最终文档 HEAD 见 Git。本轮是 G5 运营修正，不是 G6，未 push/deploy/启停桌面游戏/改 Universal Bridge。两个用户 launcher 修改和 backup 保留，整体工作区有意 dirty。

## 真人证据与根因

R5 两次真人输入是 `.jsonl.gz.partial`，只分析已记录前缀，不称完整长局。分别81,146/74,463条展开记录，已解析记录 malformed0；来源/hash见 [human-inputs](../evidence/octopus-g55-r6-2026-10-05/human-inputs.json)。

- 普通生产退回便宜小坦：87/132次 `c_tank` queue receipt，对9/8次重坦。第二局现金390、收入75.2与生产消耗77/s、实际小坦quote350，T1厂长期 `PRODUCTION_BEFORE_UNFUNDED_TECH`。Builder购买也能挤占尚未选中保护的真实升级账单，不能概括为“每到700之前必买Builder”。
- 基地二次否决：第一局8成员、ownHP3771的基地交换估计已准入（loss .3403），之后仍有基地记忆 travel veto。81次相关采样；不能拿旁边易杀目标的低损失证明基地安全。
- 修复站点依赖远端施工者：第一局首个ready repairbay距初始home2372 world；第二局205.9 world，因此不是每局都远建。
- 未知建筑资料用HP*2夸大暴露；该代理不是原版DPS。合法原版CombatProfiles仍保留真实射程/伤害/攻击兼容性。

## 实际修改

1. G5默认禁普通 `tank/c_tank`，所有ordinary购买/fallback/诊断/capacity route一致排除。`rwagent.allowSmallTanks=true`保留给独立平行实验。原版单位、已有小坦和关闭G5的历史模式不被删除。
2. 首只scout来自当前合法T1菜单；accepted后不重复开局订单。实际ready witness后，以独立 `recon:0`租约保留，再转既有 `recon:N`任务；generation实际变化，不被首支General自动吸收。去掉仅针对这只scout的“六兵/先残血小坦”前置，保留合法危险路线、严重受伤回撤和后帧witness。
3. 开局scout后，无法生产任何允许普通产品的T1厂，复用单个现有investment选择**真实native升级报价**筹资。其它购买保护实际账单；自己的升级扣回本项reserve，避免自我锁死。0 replacement quote不是未知重坦价格。接受后沿旧native credit hold，quote失效/超时释放；矿完工只能清对应NEW_MINE，不能误清TECH。
4. 修复需求先于闲置Builder新prospect，选近home闲工；远端先走既有RETURN/MOVE，一次只调一个。后帧到达重新读取当前repair plan，站点限home650 world并保留原生与威胁guard。
5. 未知建筑HP暴露系数2→0.5，已知profile不变。当前可见阻挡基地本身的已知交换可承受时，解除具体攻击的二次记忆否决；ghost和普通小队穿越仍不获攻击授权，严重实际损伤仍MOVE撤离。

契约：[重坦/Recon/本土修复](../docs/OCTOPUS_G55_HEAVY_RECON_HOME_REPAIR.md)。说明书：[Agent用户说明书](../docs/AGENT_USER_MANUAL.md)。没有新增决策频率、全局操作quota、G6 SearchArea或空海General。

## 验证

- focused最终12套PASS，包括新增G55Operations21checks；源码修改后收束才跑完整Windows。
- 完整Windows **93/93**：59 Java harness、31 Python suites /501tests、3build；**failed steps:0**。原始矩阵与transcript hash见 [windows-matrix](../evidence/octopus-g55-r6-2026-10-05/windows-matrix.json)。247个测试输入与最终产品一致；独立full构建JAR所有ZIP entry内容与交付JAR一致。不是把失败步骤补验冒充full绿灯。
- 最终同JAR native9阶段PASS：G53连续生产15、order-reuse47、G51 native295、closure、G3 production168/modes30/competition31、G54独立执行69、G55 native7。seed/0ticks的菜单、queue、order场景是fixture，不证明自然单位完成、移动、胜利。
- 最终Spain原版地图，team0、difficulty0（原版正常/中等）、请求5×、实测约4.8×；标准零普通軍队开局，无cash/HP/position/fog mutation。运行约3607游戏秒，预算结束 **PARTIAL/ONGOING**；clientExit2是预算出口，不是胜利。312,060记录，malformed0。不是与真人同出生位/同条件的受控AB。
- 该自然局515次heavyTank queue receipt，**零tank/c_tank**；scout实际首次ready42.496s、Recon首次accepted42.784s；重坦首次实际ready97.936s。队列接受与ready严格分开。
- 末期当前己方矿 **37**（13 T1、22 T2、2强化T3）、厂7、Builder10、重坦313；factory目标从2增加到7，实际厂升级8、矿升级后帧观察31。不同采样峰值不相加；ready数不是建造订单数。
- 自然出现4个ready修复湾，距初始home约89.4、63.2、170.9、172.0 world。72次实际HP恢复出FREE /61个单位 /56个后续位置witness ATTACHED，未把缺席当死亡。最后150s多个General、Recovery、JOINING等仍有accepted命令。

紧凑证据：[validation](../evidence/octopus-g55-r6-2026-10-05/validation-summary.json)、[natural](../evidence/octopus-g55-r6-2026-10-05/natural-summary.json)。原始在 `G:\deepseek 工作台\_validation\octopus-r6-heavy-recon-20261005`，最终自然 `spain-delivery-3600`，最终native `final-verification/native-delivery`，full `final-verification/final-full/transcript.txt`。

失败/中间边界：首次探索Spain在技术筹资修正前；第二中间Spain在TECH被矿完工误清修正前，主动停下保留前缀，不能叫最终同JAR验证。首次旧AA fixture被新开局scout抢先导致失败，后明确该seed场景是已完成开局的反空实验；首个native g51.log后来被重跑覆盖，不能声称该失败原始文件完整留存。其它focused失败/中间输出保留外部目录。最终交付只认最终同JAR证据。

## 未解决与下一断点

1. **末期现金183,858，消费吞吐仍不足**。持续heavy生产已成立，但不能声称经济问题全解。最后实际decision interval约12,976游戏ms；capacity150,000ms窗口只覆盖10,848ms，`sustained=false/bottleneck=UNKNOWN`，不再增厂。`BattleClient`1487/4608附近丢弃>6000ms区间，真实慢循环会造成覆盖不足。慢在哪里要profile，不能未经证据全归CPU/日志，也不能通过调高cadence或把UNKNOWN伪造为持续已证实修复。晚期真实7厂queue仍非空，非简单armyTarget停机。
2. 155次Builder queue receipt，采样峰值17、末期10：施工补充/可用性流转很大，**不能由缺席自动推断155个在送死**。前5～8min矿节奏、危险扩张保护和实际到矿施工仍应观察，不能用末期37矿掩盖开局不足。
3. 开局scout确实ready并收到独立Recon，但粗采样最大位移仅358world；850.928s仍被观察到HP350，之后缺席不能宣布死。当前不足以证明远距离高效探图或整局存活，标 **NEEDS_EVIDENCE**。
4. 当前基地进攻准入逻辑与native执行链通过，**真人攻城效果/抗空军/胜利未验收**。不以已accepted攻击等于摧毁基地。
5. 用户最新方向：下一阶段正式考虑G6终局，整体两栖编队自主海洋视野，空中补全海岛/山地/全图角落，合法发现后攻击并以native结果确认终局。复用SpecialistPool逐机mode与owner/generation，不能把它们粗暴塞普通地面General。SearchArea本轮仍contract-only。
6. 已重新阅读用户 `C:\Users\Administrator\Desktop\资金链.txt`：屯蛋=资源制造仪，T1→T2→T3经济投资链。下一将真实菜单/施工/升级与晚期富余资金协作，允许增加两栖供给；**本轮没有实现自动屯蛋或规模化终局两栖生产**。不要只花钱增收入而遗漏当前赢局能力，也不要把参考回本数字硬写成native事实。

建议下一断点：先定位大规模真实循环成本与capacity采样覆盖，再落实可运行的G6空海搜索/编队与富余投资；真人同时复测本轮重坦攻城、开局scout、矿Builder保护。不要重新G0～G4全仓审计。

## 人工入口

新隔离目录 `G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G55-2026-10-05-R6`，port47673：

`1-Start-Game.bat` → 用户建Spain比赛 → `2-Run-Agent-7200.bat`（另1200/2400）。1895文件hash复核一致；只是准备，未启动该桌面环境。输出在kit/runs。

JAR SHA256：`6c12cee0b8b6ecfad8b2056111094105527e3547a9f3804c442c7a06a5777fd1`。清单：[candidate-manifest](../deliveries/octopus-g55-r6-2026-10-05/candidate-manifest.json)；[人工观察点](../docs/OCTOPUS_G55_HUMAN_VALIDATION.md)。用户自己运行，尽量完整停止Agent收束日志；保存runs/replay/关键game time。旧R5/原版/存档/回放保留。
