# R9 handoff：攻城连续性、施工前沿、两栖制造吞吐、局部生产容错

起点 R8 `775e5c460dcfea411dee786a8dbd7fe1f78e809a`；分支 `codex/octopus-g3-execution-20261002`。产品 HEAD `1446c239c6a7d8942a11ed1c51429279d1865bbd`；文档提交后的最终 HEAD 以 Git 为准。状态：**可运行人工候选，desktop acceptance PENDING，native victory NEEDS_EVIDENCE**。

## 已确认根因与施工

真人输入：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R8\runs\20261005-103957-5088`。raw SHA256 `f5ec9f33bee7af5f54ade5a9ee47b790bd654d2a6c1aecf096844509d29bf40c`，399616 行、损坏行 0；以 raw 为准，不重审旧系统。

- **攻城授权缺乏执行连续性，hold 几何不安全。** 真人 582.805s 的 STANDOFF 目标距当前可见基地约121.406world，基地范围280；前一轮已有 accepted ADVANCE。现复用 ledger 的实际 receipt、同 generation/roster/目标、后帧当前 own 进入共同证明已开始攻城，避免普通重新估值反复中止。真正撤退仍生效；失联不攻击 ghost。所有退出/重新定位目标检查已知基地圈，危险当前位置不再直接作为 hold。目标在圈外不证明移动路径全程安全。
- **施工需求没有跟随实际空间前沿，IDLE 被误读为空闲。** 真人远端 builder 的 prospect/长途成本仍存在，却被屯蛋回拉。现导出 ACTIVE General 当前健康、ready、真实 owner 的 ATTACHED 地面成员空间，结合静态资源形成 prepared fronts；按实际区域工作及已有工人覆盖计算需求。保护远端区域承诺，普通 repair/fab 使用基地附近或未承诺工人，缺家附近施工能力走现有采购。不是1 General=1 Builder，也不因全地图潜在资源大量买人。
- **后期空军只有单个服务瓶颈，资本模型低估真实收入。** 真人57次终局两栖施工均落在同一provider；高阶蛋不在旧收入模型中。现使用自身滚动现金净流和真实 accepted 报价→后帧 ready 产品的制造服务样本推导额外provider需求；已有独立工程师并发制造，未补齐空中制造缺口时新蛋让位。不按固定坦克数停产，不写固定两栖比例，不宣称已证明大陆清空。
- **局部生产预期超时直接抛异常终止整局。** 180游戏秒改为局部 `production_witness_unconfirmed/UNKNOWN`，按当前明确 queue/tier及未结账状态处理。计时器不退款；忙队列/未知效果保留ghost，其他actor继续。历史 queue-finished 日志原形保留，额外证据走 Trace。

产品 commits：

1. `a24c516` — local production witness timeout / unknown funds retained。
2. `7384540` — witnessed siege continuity / unsafe hold exits。
3. `dc51ffb` — spatial construction fronts / multiple air providers。
4. `1446c23` — restore historical queue-finished event compatibility without weakening tests。

具体接口见 [R9契约](../docs/OCTOPUS_R9_CONTINUITY_AND_CAPACITY.md)。owner/generation/ABA、FREE/LR/JOINING、当前合法敌情、资金/slot、两栖模式证据分离保持；没有提高decision频率或恢复全局APM额度，没有改Universal Bridge。

## 最终验证

- 完整 Windows：**107/107 PASS，73 Java，31 Python suites /503 tests，3 build，failed steps: 0**。R9新增 focused 186checks。初次full的两个历史G1/G2日志形状失败由1446c23修复，wire命令原本相同；失败日志保留。
- 最终 native 12阶段全部 exit0，包括既有G3/G4/G5/G6、西班牙第三地形、新增局部超时25checks。新增fixture：真实双工厂报价/队列receipt，人工181秒clock跳跃、保留1600预算、无关真实Builder MOVE仍通过、真实队列付款随后结清。fixture不是自然生产时长或自然位置变化证明。
- 264 tested inputs零漂移，1895 kit文件核对，独立full JAR的208文件内容一致（ZIP时间戳导致文件SHA不同），受保护三文件哈希不变。
- 最终同SHA Spain自然4800游戏秒预算、5×、difficulty0/player0、原始地图、零初始普通军队；未注入资金/位置/HP/fog。约1004wall秒后预算结束 **PARTIAL / native ONGOING**，clientExit2为预算结果，无Agent异常。不是真人受控A/B或胜利证明。

最后完整own 4811.920s：**512ready重坦、455ready两栖、22工程师、7Builder、4重炮，共1000移动单位；5陆厂、44矿、5修复湾、12蛋（8T3/4T1），现金1124464**。5陆厂升级全部完成；21名后方provider被分配，531两栖制造accepted receipt不能当完成数。

自然证据：82次当前合法基地receipt+后帧己方进入后的连续攻城选择；收集到的STANDOFF MOVE目标中位于已知基地圈内者0，不声称0实际挨打。AirGeneral75search legs、43后帧实际位置witness；508历史distinct attached不是当前飞机数或击杀。后期两名北部Builder距家约5736/5713world；家附近工作仍有13次rally，并非所有回拉都消除。自然局没有触发180秒unconfirmed，新容错由受控native证明。

## 必须携带的剩余边界

**终局未闭环，且长局施工付款证据有重要缺口。** 最后执行预算4786.288s仍有439笔/876700未结账承诺。`NativeCreditWitness` 当前施工effect匹配依赖产品仍在施工点附近及同组一对一消歧，没有持久保存已经合法见到但后来飞离的移动产品effect。多个provider/重叠施工点会放大歧义；原生人口已满后的accepted也可能没有实际执行。不能据此把439笔全判已付，更不能计时退款。这是下一优先工程断点，R9没有冒险放宽账务。

用户确认原版有单位上限；本次实际当前移动单位正好1000，末期停产与满员相符。**精确当前合法 native capacity API /队列在满员时的最终语义仍NEEDS_EVIDENCE**，不把1000硬编码为所有地图/配置上限。Agent政策 militarySlots INT_MAX 不是原版容量。因此下一步应先让实际容量与陆军/空海空间职责对齐，并持久化一次性消费的合法施工付款witness；不能继续只扩厂/买provider。仍有112万现金不是“花钱问题已解决”。

大规模日志窗口仍可达几十游戏秒：末期记录median6.224s/p95 22.8s/max35.632s，不等于完整decision频率；没有全局人工APM quota不等于运行性能全部解决。真正清图、隐藏水下残敌首次合法发现、native victory、真人改进幅度仍NEEDS_EVIDENCE。

## 交付与入口

候选：`G:\deepseek 工作台\_validation\octopus-r9-20261005\rw-agent-bootstrap.jar`

SHA256：`9cd0b8a345c9b23ed0d763859019bb50555829d83e206dc06de5c5cf22b8250a`。

人工环境：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R9`，port47691。用户 `1-Start-Game.bat` → 选择Spain/难度/开始 → `2-Run-Agent-7200.bat`（另1200/2400）。仅准备、未启动；说明及观察点见 [人工入口](../docs/OCTOPUS_R9_HUMAN_VALIDATION.md)。

证据索引：[validation-summary](../evidence/octopus-r9-2026-10-05/validation-summary.json)、[真人根因](../evidence/octopus-r9-2026-10-05/human-causes.json)、[自然摘要](../evidence/octopus-r9-2026-10-05/natural-summary.json)、[最后执行预算](../evidence/octopus-r9-2026-10-05/last-execution-budget.json)。完整raw/failures/full/native保存在 `G:\deepseek 工作台\_validation\octopus-r9-20261005`，自然raw SHA256 `2802ca99e9655f89beb6ca27c409756d10bc50a1fb0ca52ed46cff7c131023fa`，436455行、损坏0。地图SHA256 `342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746`。

没有push/deploy/操作用户桌面游戏，旧run/回放/配置保留。产品源码均已commit，整体工作区**有意非clean**：用户原有 `tools/RW-Agent-Octopus-Test.ps1`、`tools/rw-agent-human.py` modified及 `tools/rw-agent-human.py.bak-20261003` untracked保留，不stage/reset/clean。
