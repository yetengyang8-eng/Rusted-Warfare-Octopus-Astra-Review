# R14 人工测试入口

新隔离目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R14`

候选 JAR SHA256：`c984f4f98d644a16ddcc9c79627242ec5467eac8a0229459badee4d46c991317`。Bridge 端口 `47703`。已准备，未启动桌面游戏。

1. 运行 `1-Start-Game.bat`。
2. 自行选 Spain 地图、普通难度，开始对局。若与上一局对照，请记录出生位、队伍、初始资金、迷雾和倍速。
3. 进入对局后运行 `2-Run-Agent-7200.bat`；目录也保留 1200 / 2400 秒入口。
4. 控制台默认显示真实 Bridge Game Time / Frame，并额外显示 Agent sample 的游戏时间和帧。浏览器可打开本次 `runs\<run-id>\rw-agent-live.html`。

最新状态来自 `runs\<run-id>\rw-agent-reports\battle-latest.json`，完整原始记录仍保存在同目录的 `battle-*.jsonl.gz`。最新状态文件只是显示旁路，不参与策略，不以最新文件代替整局证据。

重点观察：

- Builder 在已知安全直路上是否直接到矿点；当前本地机会是否接管远途 prospect；造完矿后是否继续工作。
- 重炮是否跟上 General。重炮不具备对空能力；截图异常请同时记录敌军类型，避免把武器不兼容当作漏发攻击。
- 空军短暂失联后是否继续对原区域取得合法视野；被已知防空拦住时，是有用绕行、重新接敌，还是仍长期滞留。
- 地面编制尚未完全补齐时，空海生产是否已经有实际投入；后期 primary AirGeneral 过大和闲置资本仍是观察项。
- 顶部 Bridge 时钟与 Agent sample 的差距。差距扩大才提示真实循环变慢；不能再用积压的历史显示估算当前活跃度。

R13B 原目录与 JAR 保留，SHA `8f40e5ec9616f16acbe9bf12a30d47a2f72a48ca392fb96eb8869a8babaa6206`；源码备份标签 `backup/r13b-before-r14-20261011`。没有覆盖旧环境、旧日志、设置、存档或回放。
