# R8 真人复测入口

候选目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R8`。

端口 `47689`；候选 JAR SHA256：`1014855760fe9880650e46b8aa960a9a33115f563000fadb16c65c07db90f19e`。G6、compact report、gzip 已启用。这里只准备了隔离环境，没有启动或操作用户桌面游戏；R7E 与其 runs 原样保留。

1. 用户双击该目录的 `1-Start-Game.bat`，启动这份隔离游戏。
2. 自行选择 Spain 地图、阵营和 AI 难度；记录是否同上一局条件，启动对局。
3. 对局进入后双击 `2-Run-Agent-7200.bat`。另有 `2-Run-Agent-1200.bat` 与 `2-Run-Agent-2400.bat`。7200 是游戏时间预算，不是 wall 时间，也不保证 native victory。
4. 报告在 `runs\<本局ID>\`；其中 `human-launch.json`、`rw-agent-human-report.txt`、`battle-client.log`、`rw-agent-reports\battle-*.jsonl.gz` 是交接入口。游戏回放按这份隔离游戏的正常设置保存，不需要 Agent 自动操作。

## 下一局最值得观察

- Builder 已抵达合法矿区后，是继续施工，还是有明确真实资金等待；早期同厂同址是否又提交两次，不以日志订单数代替矿或厂完成数。
- General 当前已经进入敌基地圈且战斗判断通过时，是否仍被原地 MOVE 打断；如果撤退，是否有当前损失/威胁理由，不能因为这次修改就把所有撤退都算故障。
- 战斗、撤退或骚扰结束后，部队是否真实恢复原方向推进；不仅看 ACTIVE/goal 字段，还看位置与有效原生命令。
- 两栖编队在无当前合法目标时，是否沿较长路线主动搜索，路口、目标失联、新危险后能否继续；每架 Dive/Fly 与物理/武器 witness 仍分开。
- 首 Scout 是否存活并持续获得远方视野；伤后是否实际恢复。当前自然/fixture 的 AIR 改善不代表这一点已经真人验收，更不代表终局胜利。

系统语义见 [R8 持续运营契约](OCTOPUS_R8_CONTINUING_OPERATIONS.md)；最终测试结论与未验证边界见 [R8 handoff](../handoff/HANDOFF_Octopus_R8_2026-10-05.md)。
