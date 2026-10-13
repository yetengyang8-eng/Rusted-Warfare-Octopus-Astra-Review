# G5.1R 真人反馈修正：测试入口

本次环境已经准备好，工程代理没有启动它。完整说明书见 [AGENT_USER_MANUAL.md](AGENT_USER_MANUAL.md)。

## 两步启动

环境目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G51R-2026-10-03`

1. 双击 `1-Start-Game.bat`。它使用此目录中的 `jvm64/bin/java.exe` 启动 `game/` 内的隔离原版引擎，通过 `game/rw-agent-bootstrap.jar` 加载候选 bridge。端口为 **47661**，与此前默认 47653 分开。端口已有 bridge 时拒绝再开，不关闭任何现有进程。
2. 在这扇新游戏窗口中自行进入 **合法本地、非网络、非回放对局**，建议先用 Spain 做与反馈相近的实战。看到自己的基地、已经实际进入对局后，再双击 `2-Run-Agent-4800.bat`。同目录还有 1200、2400 入口。

第二步读取相同候选 JAR，启动 BattleClient 与现场观察面板。它检查原版引擎指纹、bridge/client 相同 SHA、合法对局和连续推进的帧，随后取得该端口的独占 controller 锁。不要同时开两个 Agent 控制同一局。

**4800 是游戏秒**，正常速度约 80 分钟，5×约 16 分钟；倍速变化会改变现实等待时间。墙钟安全上限 4 小时。达到游戏时间预算但对局仍未结束，结果为 PARTIAL/ONGOING，入口可能停住显示报告；不代表引擎崩溃或本局胜负。

只在已进入对局后执行第二步，避免把原版菜单背景演示局当成你的测试局。Ctrl+C 停止这个 Agent/观察面板，游戏仍保持打开，由你决定后续操作。

## 文件和证据在哪里

| 内容 | 路径 |
|---|---|
| 冻结候选 JAR | 环境目录 `game/rw-agent-bootstrap.jar` |
| 准备清单、输入 SHA 与版本 | 环境目录 `KIT_MANIFEST.json` |
| 原版隔离游戏、它自己的设置与回放 | 环境目录 `game/`；新回放通常在 `game/replays/` |
| 每次 Agent 运行目录 | 环境目录 `runs/时间编号/` |
| 启动身份、bridge/client SHA 与 session | 运行目录 `human-launch.json` |
| 人类可读短报告 / 面板 | 运行目录 `rw-agent-human-report.txt` / `rw-agent-live.html` |
| 因果日志 | 运行目录 `rw-agent-reports/battle-*.jsonl.gz`，中断现场为 `.jsonl.gz.partial` |

本环境复制了引擎、静态资源、字体、库、地图与 Java；没有复制原目录的游戏设置、存档、回放或单位 mod。环境的候选与启动脚本是冻结副本，不受以后仓库构建覆盖。Python 使用当前电脑已经存在的 Codex Python 路径，不额外安装运行时。

## 日志模式

新入口固定 `rwagent.reportMode=compact`、`rwagent.reportGzip=true`。gzip 对精简内容无损；连续快照的明确投影仍有信息损失，不允许将其当成每帧精确重建或死亡证据。命令、回执、后帧见证、owner 和状态转换、错误、G2 派生事件逐条保留。端点来源身份与来源/检测/墙钟时间仍区分。

原响应全量审计可在入口最后把 `rwagent.reportMode=compact` 改成 `rwagent.reportMode=full`，保留 gzip。既有旧入口没有改写，仍指向旧候选；本轮验收请使用本文件的新环境。

旧分析器需要普通 JSONL 时，可在环境目录终端执行下面的形式，输入和输出应换成真实路径；输出必须是尚不存在的新文件，原日志不会被覆盖：

```text
"C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" runtime/tools/expand_battle_log.py "runs/实际编号/rw-agent-reports/battle-实际编号.jsonl.gz" "展开后的新文件.jsonl"
```

此工具解压并展开同条记录内的 `$rwReportRef`，不会恢复已经省略的连续快照。压缩流被截断的 `.partial` 可以交接给工程代理增量读取完整记录；不要删除或将其假装成正常完成日志。

## 下一次重点观察

1. 第一波接近基地：保持距离/撤退是否为移动，有没有继续沿途停战送兵；在有合法替代机会时是否继续推进。
2. 少量飞机与真正空袭分别出现时：有没有动不动整队后退；实际受损时能否撤出；反空采购能否自然到位。
3. 多 General 是否仍独立活动，前期小坦克压力、侦察、后方生产与扩矿是否持续。
4. 是否长期停在基地外不作战，或者 FORMING/补员被错误锁住。
5. 日志实际大小、控制器是否持续运行、观察面板是否有 backlog 或解析错误。

请把完整本局运行目录和回放留下，并告诉接手者异常大致游戏时间、地图/出生位、倍速和你采取的针对方法。实机结果才可以判断这次是否恢复进攻效果。现有 native fixture 证明指令与状态链；短自然 smoke 只证明候选可运行，不证明真人胜率或抗针对改善。
