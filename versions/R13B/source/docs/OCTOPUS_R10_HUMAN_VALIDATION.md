# R10 人工入口

已准备隔离环境，未启动：

`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R10E`

端口 **47695**。原设置、存档、回放和旧 R9 环境保留。

1. 用户双击 `1-Start-Game.bat`，在新窗口选择 Spain、对手和速度，开始本局。
2. 双击 `2-Run-Agent-7200.bat`；也提供 1200/2400 入口。数字为游戏秒。单倍长局的 wall safety 为 14400 秒，正常限时退出是 PARTIAL。
3. 控制台标题持续显示当前 Bridge 游戏时间/Frame。打开新 run 的 `rw-agent-live.html`，顶部也固定显示。例如 `Game 13:42.6 | Frame 245731`。
4. 本次产物保存在 kit 的 `runs/<时间-pid>/`，包括 human-launch、live HTML、human report、压缩 raw。失败/换 session 时钟会显示 UNKNOWN，不会用墙钟补算。

clock 是人类观察基础设施，不改游戏状态或 Agent 决策。Agent 结束不关闭游戏。不要并行连接多个 Agent 到同一局。

建议这轮观察五件事，不用固定成绩线：

- 小 Builder/骚扰出现时，是否只借附近少量兵；结束后 JOINING/General 是否恢复原来的补兵/推进。
- 远端 Builder 真正到合法矿边后，是否开始银行真实报价，钱够后是否施工；截图带 game/frame，便于分清缺钱、当前占用与命令未执行。
- Scout 是否持续向外走；短 RECHECK 结束后是否恢复探索；危险改变路线后是否仍继续工作。Scout 缺席本身不证明死亡。
- 已有陆军覆盖当前需求后，新增资金是否逐步进入两栖/工程师/矿；观察实际 ready 飞机和新矿，不把 receipt 数当成果。
- 是否出现多个 `air-general:N` 独立搜索；某队接敌时其它队是否继续走，clear 后是否恢复原区域。

T2 空中 Scout 与核弹未启用。本轮隔离自然验证包括原版 difficulty 0 和 -2、5×、player0：普通难度4800游戏秒预算结束为 PARTIAL/ONGOING；较低难度约47游戏分钟取得 native VICTORY，并自然形成最多18支独立 AirGeneral。两局不是单倍真人 A/B，也不保证对真人胜利。R9 首次胜利是用户真人报告，保留这一进展；R10 的真人效果仍需使用本候选验证。
