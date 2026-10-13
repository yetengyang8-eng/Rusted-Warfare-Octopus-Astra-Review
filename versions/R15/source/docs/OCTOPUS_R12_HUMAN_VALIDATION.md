# R12 Builder 人工测试入口

本轮从 R11 `f4d81e8` 开始；产品 `7b9e4e5`。仅准备隔离目录，没有启动、关闭或操作用户桌面游戏。

目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R12`，端口 **47699**。

1. 用户运行 `1-Start-Game.bat`。
2. 用户自行选择 Spain、难度、玩家位和速度，然后开始游戏。建议沿用本次罢工局条件；自然 headless 的 Hard/player0 不能代替人工 player5 条件。
3. 用户运行 `2-Run-Agent-7200.bat`。另有1200和2400；7200为 game-time 预算，wall safety14400秒。预算到期不关闭用户游戏。
4. `runs/<run-id>/rw-agent-live.html` 顶部及 Agent 控制台标题默认显示 Bridge 当前 `Game mm:ss.s | Frame N`。UNKNOWN 不用墙钟补算。raw 使用 compact gzip。

`game/rw-agent-bootstrap.jar` SHA256：

`61d8a341b3fff0445e67978d7deb2a6d4e349589e683a1f35afc35c9317b98cf`

重点看三件事：

- 多个 Builder 进入同一矿区时，是否分别开始不同矿点，是否仍互相空等。附近有合法矿时，应落实当地施工，而不继续奔旧 rally。
- 已经开始的建造，是否被无意义找矿 MOVE 覆盖。遇到当前危险/严重残血的真实 retreat 另算，不把所有转向都叫干扰。
- 矿点旁停留时记录 Game/Frame、Builder ID、当前现金和视野；需要区分当前 native 合法矿、报价预约、accepted receipt、实际矿建成，不能只按最终矿数量判断。

若旧 R11B 还在运行，不要为新候选自动关闭它。新环境使用独立端口；同一局只运行一个控制它的 Agent。

沿用现有成熟军队、重坦、恢复、经济和 Air 主循环；本轮不交付新的战术行为。没有复制原安装 preferences、parameter、saves/replays 或单位 mods，没有 push/deploy。

完整交接：[R12 handoff](../handoff/HANDOFF_Octopus_R12_Builder_2026-10-11.md)；契约：[Builder work](OCTOPUS_R12_BUILDER_WORK.md)。
