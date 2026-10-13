# G5.1 真人 Spain 验收入口

工程代理不执行本文件中的桌面入口。操作者自行打开/创建合法本地Spain对局，再双击 `tools/RW-Agent-Octopus-Test.bat`；入口默认读取 `deliveries/octopus-g51-2026-10-03/candidate-manifest.json` 中的新候选JAR。已有Bridge若协议兼容可接入，客户端与Bridge SHA分别记录，不覆盖旧安装。

默认4800游戏秒；也可由操作者显式运行：

```powershell
& "G:\deepseek 工作台\GitHub发布\Rusted-Warfare-Agent\tools\RW-Agent-Octopus-Test.ps1" -Seconds 4800
```

若有上一局controller/launcher仍在运行，保留port lock保护，由操作者结束旧controller后再接入。开发代理没有移除真人lock、杀进程或操作游戏。

新开隔离游戏的 `RW-Agent-Octopus-Start-Test.bat` 仍由操作者显式选择；复制行为与桌面实际启动验收分开。默认Test入口只attach/wait。

Observer新增逐General `tactic / admission / need`。UNKNOWN保留；短报告、human-launch身份、raw、client日志与离线HTML保存在新 `_human_runs` 目录。沿用原生结果与PARTIAL/ONGOING区分。

本轮最有价值的5个观察点：

1. 首支General遇当前可见基地静态火力时，是否选择其他合法目标、standoff或retreat；不会把拒绝目标后的frontier当绕过防御许可。记录有无长期站桩。
2. ground主力遇合法可见gunShip等AIR时，是否出现ANTI_AIR、停止不利推进、从实际菜单补counter；counter之后是否自然到达主力。失联后不追隐藏位置。
3. builder远征到矿区后是否优先本地矿，资金不足/UNKNOWN是否合理等待；空簇/当前危险是否能释放，避免长期占住worker。
4. 6坦、3矿、首厂T2、首矿T2的真实ready时间；capacity需求窗口、factoryTarget和收入→产能转化。不能只比较peak credits，更不能把战败后无可用厂的余额当投资机会。
5. 总APM、move占比、单位引用量和有效推进；缺坐标Bridge的short-defer不得掩盖停滞。与上局比较时标明地图/速度/难度/seed与真人打法差异，不直接推胜率。

真实反空交换收益、Spain局部簇收益、自然持续扩厂与APM下降仍 `NEEDS_EVIDENCE`。保留此前真人G5自然4General、多路推进/撤退、长期生产/扩矿、>48chunk与无desync/resync的证据。
