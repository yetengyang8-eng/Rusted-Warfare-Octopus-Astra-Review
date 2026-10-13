# G5.1R 真人反馈修正交接 — 2026-10-03

## 身份与入口

分支 `codex/octopus-g3-execution-20261002`。开始 HEAD `de80607`：产品仍是上一轮 G5.1，后续只修过 launcher 字体。启动时已有两处用户改动和一份 backup，完整保留，没有 reset/clean，也没有将其混入本轮提交。

实现 commits：`058960d`（谨慎与逃离分离，STANDOFF move）；`e6ba1d6`（compact/gzip 与隔离环境准备）；`8e694e7`（真人面板发现 gzip、入口 CRLF）；`2f3975a`（G5 runtime 旧撤退 fixture 对齐新损伤契约）。最终文档/evidence commit 在其后，HEAD 见 Git。

产品构建源码 `e6ba1d6`，JAR **`c5f90ed319e2281b1f4f98ccc0a27f5e32216a810ca0c5e60d247cb09adee0b0`**。后续 8e694e7 只变 Python/launcher/tests，agent 产品源码未变。隔离测试环境：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G51R-2026-10-03`，1897 个准备文件逐项 SHA 校验通过，附 `KIT_MANIFEST.json`。

操作者两步启动：`1-Start-Game.bat` → 在新窗口实际进入本地 Spain 对局 → `2-Run-Agent-4800.bat`（也有1200/2400）。使用独立端口47661，同 bridge/client SHA guard、controller lease；不启动旧目录的游戏或旧候选入口。4800为游戏秒。运行目录是环境 `runs/时间编号/`，新游戏回放在环境 `game/replays/`。工程代理只准备，未执行桌面入口。

接手先读：[说明书](../docs/AGENT_USER_MANUAL.md)、[测试链](../docs/OCTOPUS_G51R_HUMAN_VALIDATION.md)、[candidate manifest](../deliveries/octopus-g51r-2026-10-03/candidate-manifest.json)、[验证摘要](../evidence/octopus-g51r-2026-10-03/validation-summary.json)。不要重新 G0～G4 全仓审计。冻结9/30 verifier仅报告合法源码drift，不授权回滚。

## 真人证据与根因

这次用户报告第一波看基地就撤、怕空军、黄色反向进军沿途送兵；肯定早期小坦克压力、侦察和 scout 反空。不要将上一轮谨慎改动称为已证明收益。

实际运行确认是273a861f…上一版 G5.1：`_human_runs/20261003-214005-9816/human-launch.json`。raw `rw-agent-reports/battle-1791034888137-3b62fa62.jsonl.partial`，**1,073,739,467 bytes / 171,071有效行 / 1末尾截断行**，SHA `6c76801902b07f3b75f7b40ac1b637f68c802fe2580d361604f4a4c173853c85`。日志达到1GiB磁盘上限，使 BattleClient exit1；不是普通预算结束。原始 raw/spool 保留，不删除。

- `GENERAL_RETREAT` 原本已经是 move。黄色反向攻击实际对应 G5.1 STANDOFF 复用了 attackChunks→attack-move；不能把所有退却笼统说成旧实现都发错命令。
- `observe()` 将当前 AIR deficit、静态HP加权暴露或 HP overmatch 本身升级为整队 crisis，没有要求实际承受有效损伤。目标准入与正在输掉战斗混成一个条件。
- raw 重复输出大观测、Intent 与上下文；前三大事件 g2_world_update168.6MB / g1_command_attempt130.6MB / g3_intent130.2MB。回放与诊断记录粒度不同，但这局实际撑爆了控制器磁盘预算，必须修复。

## 实际行为变化

1. **STANDOFF 使用逐单位 move。** 原版暂没有 group move 接口，不能发明。保持现有 GENERAL lane/priority30、generation/receipt/后帧复用。attackMove 不可作为该动作的后帧有效订单。
2. **目标谨慎与危机逃离分层。** 危险代理保留目标排序/准入和反空采购影响，但不独立触发整队 retreat。现有5s可信损伤窗口，HP下降达到当前General最大HP总量8%或至少一名明确己方损失，才可结合当前局部危险触发逃离。原有25%损伤/两次明确损失 losing exchange 保留；损伤不归因给某一架飞机。1HP chip不使整队撤退，有其他可准入目标可继续ADVANCE。
3. **不因看到基地就机械向后跑到固定700点。** 已在政策边界外时可以在当前centroid取消不合适进军。已有合法pathKnown frontier，端点可准入且直线政策几何排除当前visible armed hazards时可以继续探索；不回落到被拒基地后方。700是既有政策距离，不是native射程，直线排除不是实际路径安全证明。
4. **精简与无损压缩分开。** 新人工入口固定compact+gzip。compact保留全部命令尝试、回执、见证、转换、错误及G2派生事件；连续raw GET/G4单位快照在30game秒或15wall秒关键帧保留，其余明确标为OMITTED，不作negative/death证据。G5观察面板状态逐条保留。重复部分用同记录 `$rwReportRef`，无跨记录悬空引用。
5. **gzip流提交与观察面板。** 输出 `.jsonl.gz.partial`，sync flush支持现场增量读取；正常原子提交 `.jsonl.gz`，SnapshotOutput磁盘预算计存储字节、8MiB内存/分块保留。full默认兼容旧审计，旧人工入口没有覆盖；新kit的完整数据审计可选full+gzip。`expand_battle_log.py`输出新JSONL解压/解引用，不能恢复被省略字段。

G3 credits/unsettled/slot/burst、owner/generation/ABA、FREE/LR/JOINING/ATTACHED、反空采购、局部矿承诺、既有经济链均未重写。force继续真实collect/priority；成熟运营lane仍immediate。未加decision frequency，SearchArea inactive，未进新阶段或修改Universal Bridge。

## 验证与限制

最终回归事实以 validation-summary.json 为准。产品完整回归在含静态assets/res的隔离snapshot运行一次；后帧native与自然smoke使用同JAR。

- Windows首遍 **80/81，47 Java / 31 Python suites / 500 tests**。唯一失败 `py:test_g5_runtime` 旧fixture期望“己方未受损、敌HP突然变大→撤退”，与本轮明确替换的契约冲突。测试加入显式synthetic own HP下降（1000→800，frame4；高敌HP先frame3可见不撤），同JAR仅补该suite **4/4 PASS**。有效81/81、未解决失败0；不把原始首遍称为零失败，不重复full、不改产品回到旧行为。

- Focused：Tactical75、Combat57、Commander265、ForceController69、Registry287、Operations37、CompactLog34通过。
- Native **501 checks PASS**：G51 272（含AIR18）、G3多厂168/模式30/lane竞争31。原版waypoint及后帧orderType确认move；初见AIR、1HP chip不撤，六成员各20HP显式fixture损伤后撤；原生counter菜单/slot/付款/队列链保留。simulationTicks0，人工位置和HP不冒充自然被空军攻击、自然抵达或存活收益。
- 原真人raw真实转换成gzip：**35,075,195 bytes / 原体积3.2666%**。171071有效记录全部保留；139402非投影记录data+trace解引用精确等价，含全部commands/receipts/witness/transitions/G2 events。原末尾坏行独立计数排除。gzip完整性通过；每条syncflush的体积测试比生产每轮flush更保守。这是格式/体积证明，不是新对局。
- 同JAR SmallIsland5×自然600320game-ms：**PARTIAL/ONGOING / exit2预算结束**，49新战斗单位、2次工厂施工、4次矿施工、1次ready厂升级；关键帧最多同时3ready矿/2ready陆厂，1General，ADVANCE/STANDOFF/FRONTIER。日志7,850,135 bytes / 0 malformed，真实面板回读解析0错误。不要把累计4矿叫同时4矿，或用采样关键帧时间作精确建造时间。
- 自然仍有409 GENERAL_RETREAT accepted记录、400相邻同actor同路径move重复；当前并非受控AB，不能宣称APM已降或真人战术获益。后续用实机定位剩余退却/集结刷新，不重启全仓审计。

完整回归冻结后发现工具问题：BAT newline经Windows二次转换产生CRCRLF，改为字节写入；实际gzip面板初验发现后缀筛选漏检，先读0事件，修复后23 focused测试及真实smoke全日志回读通过。只改launcher/observer/tests，JAR完全未变，没有重复full；223测试输入中最终差异仅4项（两个工具与两个测试），逐项记在validation-summary，agent/src字节全一致。此前native JDK17环境/class版本/新增GENERAL move未过滤lane的测试失败、root外部PS UTF8 BOM失败均保留，未修改产品绕过环境关。

## 下一断点 / 需要什么

需要操作者在新隔离环境跑一局反馈相近的Spain，保留完整 `runs/时间编号`、原版回放，并给异常大致游戏时间/倍速/出生位/针对方法。优先看：移动能否真正脱离；小规模AIR是否仍过退；有合法替代目标是否继续施压；反空自然到位；长局日志与面板是否稳定。不要替操作者启动、关闭或操作桌面游戏。

NEEDS_EVIDENCE：真人第一波生存/攻下目标效率、自然AIR交换与counter到位、动态rally/path安全、4800长局实际存储/面板实时积压、剩余重复move、持续新增产能。此前G5真人自然4General、多路推进撤退、长期运营、>48、无desync/resync仍是历史已验收事实，不降级也不转借成这次G5.1R真人验收。

未deploy、未push、未操作用户桌面；原版引擎/冻结交付Jar指纹保持原值。只使用Sol6.1/high局部子代理，root最终整合、完整回归、同JAR原生/natural与交付。
