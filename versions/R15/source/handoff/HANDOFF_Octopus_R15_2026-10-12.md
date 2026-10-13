# R15 Air 清剿连续性候选交接

分支 `codex/octopus-g3-execution-20261002`，起点R14 `6637cdd`。本轮提交：

- `98acb46`：隔离原生 `--player` 选择、Python透传、0 tick出生证据及测试。
- `cff889e`：Air区域清剿连续性、受控重取视野、集结/损伤反馈和focused/native测试。
- 文档/证据最终HEAD见Git。

**同一最终JAR在普通Spain、原生player5、5×自然运行中取得VICTORY，检测样本Game42:43.664 / Frame160229；Agent exit0，无人工接管。** 这是单次隔离自然胜利，不是稳定胜率或真人验收。原生事件真实发生瞬间没有另行断言；该时间是判胜时最近的合法/state样本。

## 改动与真人证据

用户R14在Game40:05.4截图后手动集中两栖，先拆防空、清东岛，再顶着Agent覆盖命令飞往东南岛，最终43:50.157胜利。该局必须标人机混合，不能算R14自主胜利。原始溯源在 `G:\deepseek 工作台\_audit\r14-human-intervention-20261012\REPORT.md`。

确认的冲突为：记忆AA圈导致安全观察站位永远拿不到所需视野；单点clear马上恢复旧Search方向；清点后的主队被当idle donor拆散。R15仅在现有AirGeneralController修这条生命周期，未改地面、Builder、经济、Repair和生产主循环。

- 合法历史建筑责任优先于泛搜索/拆队；清一点后继续其他已知责任，直到没有eligible duty再恢复Search。
- 实际已集结、当前physical AIR的现有成军规模允许MOVE重取视野。远处名册不冒充集結，散队先汇聚，晚到成员实际到位后才加入own HP预算。
- 复用现有损伤预算，own HP大幅恶化时真实MOVE撤出；敌情UNKNOWN不阻止己方损伤反馈。完全恢复或真实补充后可重新评估，不靠timer重复送入。
- 所有攻击继续要求当前合法可见、原生兼容性和原生命令guard。记忆位置不是攻击权限，receipt不是实际抵达，clear不是killed。
- 不同既有owner优先领取不同区域，保留并行搜索。**末段仍观察到弱小owner触发过量支援，未声明最少增援已经解决。**

完整契约：[OCTOPUS_R15_AIR_CLEARANCE.md](../docs/OCTOPUS_R15_AIR_CLEARANCE.md)。没有新增全局APM额度，没有启动核弹方向。

## 自然运行事实

唯一完成的自然局：`G:\deepseek 工作台\_validation\octopus-r15-air-clearance-20261012\spain-normal-player5-v2`。普通difficulty0、5×、LOS fog、原生player5。第0tick基地 `(7370,1950)`；原图与隔离图SHA一致，所有10队由原版提供4000初始资金/1初始单位，1人类+9原版AI，未修改位置、资金或fog。

原始日志210494行，0 malformed。压缩raw、独立own monitor和输入SHA均保存。以下不使用旧summary中的completedMines=12冒充全局矿数。

| 时点 | 实际证据 |
|---|---|
| 20:07.040 | 首个完整own关键帧中出现ready两栖；并非精确生产完成事件时间 |
| 31:40.448 | 主owner以10架已集结飞机、4944当前HP开始调查旧AA点14181 `(3500,7280)`；10条原生MOVE accepted |
| 31:45.536 | 当前可见T2防空8448收到9成员attack-target；此后也有后续批次 |
| 31:48.384 | 独立own采样中28969到旧点约9.65 world；是真实自然位置，不是fixture移动 |
| 32:03.344 | 主owner继续对当前工厂13255下attack-target |
| 32:11.392 | 当前局部visibility加新combat empty确认旧点clear，不计击杀 |
| 42:43.664 | 原生VICTORY被Agent检出，退出0 |

东南任务29255也形成接近/接敌/clear记录，但主队当时仍在远程在途；其它编队/友军视野先完成局部覆盖。不能把责任坐标当主队坐标，也不能把每次局部clear归功于某个将军击杀。

最后完整own关键帧42:32.656：**44矿、7陆厂、94重坦、170 ready两栖、12工程师、5Builder、3修复湾，现金304502**。最后独立monitor含未ready时为173两栖，不能与ready数混用。49条accepted建矿均找到后帧ready同址己方矿，但不是独占builder因果归因。

全局最多24支有效Air编队；107次Air攻击receipt、191次后帧search leg witness、42次局部clear。15次probe记录中很多是多个owner同点支援，不应写成15个独立岛屿成功。39次blocked仍存在，不能以最终胜利否认过程中的局部等待；没有自然loss-retreat事件，因此撤退/恢复闭环只有focused证据。

末5分钟全部observation时间戳31个间隔，中位9.728游戏秒。未把约30秒完整own日志采样当decision gate；也未证明同规模性能改善。

证据：[spain-normal-player5.json](../evidence/octopus-r15-air-clearance-2026-10-12/spain-normal-player5.json)。

## 验证与失败保留

- Windows矩阵首轮 **134/137**，102 Java、32 Python/509 tests、3 build。两个旧原生套件缺隔离assets，报表套件缺历史docs样本。补齐输入，**同一产品JAR/测试源码不变**，仅重跑这三套全部通过，最终 **PASS_COMBINED_MATRIX 137/137、failed steps: 0**。不声称首次全绿。
- 新focused：Air116、Headless参数12。新真实Bridge/原生fixture18checks，0自然ticks；显式fog/位置/时钟不能作为自然飞行或发现证明。
- 原生player5、默认player0各64checks，仅3帧：初始基地分别 `(7370,1950)` / `(4370,5610)`；默认路径保留。地图自动创建8/9槽，未删除或改资金。
- 首个自然启动在Agent启动前因观察脚本误读`player.id`失败；修为真实`player.teamId`后重跑，仅完成一局自然比赛。保留首启结果。
- focused首遍第三个模拟区域越界、player fixture首遍错误预期8/9槽不存在；只校准测试输入/期望，失败日志均保留。

矩阵：[windows-matrix.json](../evidence/octopus-r15-air-clearance-2026-10-12/windows-matrix.json)，总汇：[validation-summary.json](../evidence/octopus-r15-air-clearance-2026-10-12/validation-summary.json)。完整输出在 `_validation\octopus-r15-air-clearance-20261012`；player bootstrap在 `_validation\octopus-r15-flow-20261012\player`。

## 剩余边界与下一断点

1. 38:52附近一个不足编制的owner仍在目标30331上承担责任，触发多支既有Air同时支援。最终没有阻塞胜利，但“支援已足够”尚未统一扣除，可能造成多余跨图飞行。下一工程断点应先修已分配支援量，保留本轮已证实的清剿连续性，不重新回到记忆AA禁区。
2. 清剿预算不是完整FightEstimate；未知防御仍可能造成一个慢循环内的突发损伤。自然本局没有触发撤退，不能据fixture宣称所有真实危机已验证。
3. Scout只在早期有完整位置观测；长期存活/替补不在本轮解决。后期现金仍30万量级，没有为了花光钱改经济。
4. 单倍真人同出生点的观感、人工接管后的命令覆盖、各种岛屿防空强度仍待人工测试。旧日志的退出超时不能混入新自然胜利。

## 交付与保护

新人工目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-12-R15`，port47705。

`1-Start-Game.bat` → 用户自行普通Spain/出生点5 → `2-Run-Agent-7200.bat`。默认真实Game/Frame，Agent sample单列，live HTML保持；详见[人工说明](../docs/OCTOPUS_R15_HUMAN_VALIDATION.md)。只准备，没有自动启动游戏。

JAR SHA256：`d0530e5d7e294d391f2252109393054ec7b3cd382e30b0446ef028dab01b7aba`。Windows、自然局、新kit一致；320个测试快照输入和1895个kit文件核对零漂移，文档收束另计。

R14备份 `backup/r14-before-r15-20261012` → `6637cdd`；原R14 JAR仍为 `c984f4f98d644a16ddcc9c79627242ec5467eac8a0229459badee4d46c991317`。

没有push/deploy/启停桌面游戏，没有改原版商业引擎、旧kit、设置、存档、回放。用户原有 `tools/RW-Agent-Octopus-Test.ps1`、`tools/rw-agent-human.py` 改动和 `.bak-20261003` 三项哈希未变，未stage。最终工作区有意保留这三项dirty，不得reset/clean。
