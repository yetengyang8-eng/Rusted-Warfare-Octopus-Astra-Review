# Rusted Warfare Agent：G5 收尾 / Pre-G6（候选 G5.2）

从现场 HEAD17d3328 开始，分支 `codex/octopus-g3-execution-20261002`。未 reset/clean 用户改动，未 push/deploy/修改或启停桌面游戏。旧 frozen baseline 是只读核对对象，不回滚当前八爪鱼产品。

## 结果与提交

- e8d0e01：Commander 响应预算、General 交换估计与基地防区记忆、经济库存/成军目标分离、完整矿尾链、恢复池和原版承诺见证。
- fff1883：原版独立推进/撤退场景以当前可见经济建筑替代未分配移动目标；保留绕过 Commander 禁止的语义。
- 32064c6：G5 关闭时保持旧报价读取顺序与日志，严格保住 G1/G2 frozen baseline 等价。
- 7cd9111：旧危机 HTTP fixture 的虚构10万HP tank 改成明确未知 profile；检验“未知能力 + 真实己方受损”的保守撤退，不拿虚构 tank HP 当原版火力。
- 8a4cad3：所有远处目标，包括建筑，都遵守Commander许可；近处当前目标仍允许本队处理，取消已撤销目标的旧指令用同owner移动。
- 最终文档/evidence 提交在其后，最终 HEAD 以 Git 为准。

最终 JAR SHA256 **7de6527303a06cf6613028fea31901ffb00435460a86bab1828f9edf338032b7**。产品/测试冻结断点8a4cad3。最终产物：`G:\deepseek 工作台\_validation\octopus-g5-closure-20261004\release-verification\final-source\agent\dist\rw-agent-bootstrap.jar`。人工 kit：[R3 测试链](../docs/OCTOPUS_G52_HUMAN_VALIDATION.md)。

## 现实审计与设计选择

逐行读取新 G51R 真人 raw，不用旧G5摘要替代：首份461918条/0 malformed，最大现金565045.5、普通armed128、策略目标128、General6；第二份400870条/0 malformed，现金116060.5、armed/目标112、General5。首份硬上限空闲51条、目标空闲30条；第二份目标空闲53条。出现同任务吸走5/6 General 的快照；统计是记录数，不推算持续时长。

`CommanderDirector` 原来没有需要/覆盖/缺口预算，`GeneralCombatDirector` 又独立遍历当前目标；`TacticalOpportunity` 的 HP exposure 会高估厚建筑。`BattleClient.evaluateProductionCapacity` / `GlobalStrategyPolicy` 的软件上限与目标使现有路线空转，不是单纯“醒得慢”。StrategyDirector、MineInvestmentPolicy、NativeCreditWitness 已有 native quote/付款/队列承诺，继续扩展这些机制，不另建资金“大脑”。旧工程语义的 legal visibility/owner/generation/receipts 保持；用户新恢复方向优先于旧G6 Scout/Reclaim。

当前控制图：合法观测 → registry/恢复池 → native兼容LR预备与真实分兵 → 刷新当前General roster/中心 → Commander统一预算 → General当前交换/防区/任务决定 → force collect/priority/dispatch → 后帧原版 witness。成熟经济、生产、Strategy、Recon 仍有 immediate dispatch，没有宣称全局统一调度。

### 为什么派这支队、为什么只派这么多

ThreatTask 明确 demand/localCovered/assigned/deficit。当前真实LR先扣覆盖，FREE没有实际owner/兼容承诺不算已解决。选择足以补齐缺口的一队优先；不足才扩大，距离加原目标中断成本、既有任务保持偏好，撤退/FORMING/陈旧队伍不被普通调动。General所有远处目标均受本队任务许可限制，建筑不再有绕过预算的例外；贴身220政策距离内可自行处理当前合法目标，包括自卫和经济机会。失联CLEAR不是kill；最多3秒在己方当前位置非攻击注意，LR仍立即clear，pending仍保留。

`g5_commander_transition` 中 COMMANDER_THREAT_BUDGET 给每队记录 selected/reason/strength/distance/interruptionCost/cost。混合域暂用可兼容所有当前目标的保守容量交集，不是完整组合能力求解。

### 为什么不踩基地、何时又能攻入

合法见过基地后保留旅行防区，失联只作 veto，不授权隐藏攻击；明确当前销毁才移除。记忆阻止普通frontier穿越，被拒点输入已有avoid机制；已处圈内允许持续向外退出。当前基地仍可见且 FightEstimate 已准入才允许突破。原版实际路线安全、被毁基地是否有明确dead行仍 NEEDS_EVIDENCE。

FightEstimate 分离于Commander粗HP预算；已核验四种原版profile的兼容DPS、HP、射程/速度、接近损失、TTK/预计交换决定准入。厚无武装建筑DPS0。星星库用于校准，原版基地4000HP覆盖社区3000说法。未知机制不伪造DPS，保守fallback标签保留，真正逃离仍要求可信己方实质伤害/显式损失。move撤退/保持距离不回退黄线。

### 为什么高收入还会继续出兵、投资与恢复

G5默认软件保护512，显式配置不覆盖，native硬上限UNKNOWN。收入、当前普通原版报价与己方已有任务/补员共同决定总库存目标，成军目标继续沿用既有公式；增长有步长/窗口/紧急态保护，军事floor不得穿过经济目标。原版路线UNKNOWN不补猜。保留paid ghost、资金、slot、burst、ABA、150秒/80%利用率扩厂窗口及默认工厂上限5。

T3尾链支持超频8000/强化3000的当次原版报价；超频需连续保守安静窗口与预算/收益，风险或证据不足地区可做防御强化，强化收入0。跳过队列的同级转换需后帧同actor、实际产物类型改变、ready buildProgress才能结算，钱包/空队列不释放钱。

严重伤兵<25%进入真实 force:recovery owner，generation++，释放General/pending/LR，停止旧进攻；后帧HP≥75%才FREE重新分配。repairbay目标结合伤兵数与缺血量，约8人一湾只是容量参考，小规模不强制建湾。运营复用extra builder/engineer/native plan/lease/credit/slot，扣已有/在建/未显现承诺。自然施工、治疗与恢复重编尚未证明。

完整参数、字段和证据边界见 [工程契约](../docs/OCTOPUS_G5_CLOSURE.md)，面向人的解释见 [说明书](../docs/AGENT_USER_MANUAL.md)。

## 验证与诚实边界

最终完整Windows **87/87，failed steps 0**；53个Java harness、31个Python suite、501个Python tests。原始日志在release-verification/full-console.log；不是忽略失败后的有效计数。本轮首遍84/87、中间87/87、远处目标硬化后最终87/87，三份原始完整日志均保留。17个实施focused通过，中间4/4 G5 mainloop、远处目标focused39通过，最终整轮注册focused再次通过。

首遍完整Windows原始 **84/87，failed steps3**：G1/G2发现关闭G5的额外报价复用/字段，32064c6修产品隔离，未放宽两份严格对比器；G5 runtime fixture 原把单台已知tank虚构成10万HP火力，7cd9111改为显式未知profile，中间Jar4/4focused及最终Jar完整回归中的4/4均通过。首遍失败、归一化对比和native旧场景失败全部保留。

最终隔离原版 **513 checks PASS**：G51作战/反空272，G3多厂168/两栖30/lane31，新矿尾链/修复报价/伤兵move12。native fixture人为seed单位/地形/HP/clock，simulationTicks0；真正RuntimeBridge HTTP、command.k、付款、队列与后帧order已证，不声称自然生产/修复/航行收益。

最终同Jar Small Island5×自然600秒 **PARTIAL/ONGOING**，无人工位置/HP变更。新战斗单位42，工厂完成链1，矿完成链3（关键帧最高同时2矿），工厂目标增长0；接受命令1512，连续同路径move记录130。关键帧最高伤兵7、修复湾目标2，同威胁最多分配1队。入恢复池22条是状态记录，不是成功治疗数。尚未证明自然扩厂/修复完成。首遍不同Jar另有536.64秒VICTORY，只保留为过程证据，不作为最终Jar验收或真人改善。

证据根：`G:\deepseek 工作台\_validation\octopus-g5-closure-20261004`。最终回归/native/smoke在 `release-verification`，中间兼容性修正后的一轮在 `final-verification`。最终8a4cad3全部237输入冻结且与工作区字节一致，无测试或产品漂移；最终kit全部1895文件逐项hash验证，原版gameJar一致。中间7cd9111的Python fixture执行前同步也独立记录，不冒充最终冻结。旧G51R raw/replay、所有中间kits、用户launcher两个改动与backup保留，Universal Bridge未改。

## 未解决 / G6边界 / 下一断点

这轮证明了预算/交换/防区/资金/owner状态机的可运行契约，尚未证明真人胜率、现金消化效率、APM下降、基地绕行自然路径收益、矿转换自然完成或修复吞吐。FightEstimate只有四种verified profiles，是首阶模型；Commander混合域、鬼影保守性、base明确销毁输入与生产工厂上限仍有现实限制。

G6下一应处理完整Capability Search/A7/A8与两栖任务生命周期、合法覆盖/终局SearchArea契约、恢复的实际服务能力与可达修复点，以及必要的跨兵种组合估计。两栖保持SPECIALIST_POOL；不为了闲置并入普通General。动态编制/将军数量策略、复杂Combat Performance/击杀归因没有借本轮启用。

当前最值得做的是一次R3 Spain真人长局：小骚扰只派必要队伍、已知基地弱兵绕行/强兵准入、112/128之后持续收入转兵力/投资、T3强化/超频完成、伤兵自然修复回FREE。保留本局完整runs/replay，注明异常game time、倍速、出生位；不用重新从G0审计，也不要再提高decision frequency。
