# R11B 人工测试入口

本轮产品 `197310a`，从 R10 `d776f44` 延续。准备完成，未启动用户桌面游戏。

目录：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R11B`。端口 `47697`。不要使用中间构建 R11；该目录已留下更替说明。

1. 用户运行 `1-Start-Game.bat`。
2. 用户选择 Spain、难度、玩家位置、倍速，开始本局。建议沿用上一次真人条件，并把实际条件记在反馈里。
3. 用户运行 `2-Run-Agent-7200.bat`；另有 `2-Run-Agent-1200.bat`、`2-Run-Agent-2400.bat`。7200 是 game-time 预算，wall safety 为14400秒。预算到期不关闭用户游戏。
4. 每次结果在 `runs/<run-id>/`，Agent raw 在其报告目录。控制台标题及 `rw-agent-live.html` 顶部默认显示直接读取 Bridge `/state` 的 `Game mm:ss.s | Frame N`；无法取得合法当前时钟时显示 UNKNOWN，不由墙钟补算。

候选 `game/rw-agent-bootstrap.jar` SHA256：

`798b88778921cda6fc0e39fc3546a564286dfab2d41f7bb6b952a2ae9dedb832`

使用隔离原版副本，未复制原安装的 preferences、parameter、存档、回放、单位 mods。不是部署或 push。

观察点：

- Builder：是否沿同一矿点连续前进，当前合法矿报价出现后多久真正施工。路段到位、accepted、建成分开看；遇险取消旧路不代表新区安全。
- 小威胁：孤立、明确无攻击能力的建筑是否由少量附近兵处理，Ground 主力是否继续自己的推进。武装或未知建筑仍可能走战略分配，这轮没有将它们全部降级。
- Scout：初期是否在到达前延续直线路段，当前敌人出现后能及时改向；临时 recheck 后是否恢复 EXPLORE。长期存活、受伤后复出仍需真人观察。
- Ground：没有当前敌人时是否继续 deep/stale fog patrol；编队形状改变后 anchor 是否仍在主力中心附近。内部 ACTIVE、命令次数不能代替实际位移。
- Air：是否在旧 leg 到达前继续安全下一段；普通陆地目标已有 Ground 真正执行时是否让出并恢复原搜索。空中、水下、跨域目标仍保持自己的职责。

本轮没有提高 decision frequency 或加入新的操作额度。当前 G5 没有跨互不冲突 actor 的全局人为 APM 上限；owner/generation、同 actor 冲突、重复有效命令、资金和 producer slot 的保护继续保留。若晚期响应慢，反馈 Game/Frame 与实际位置/order，下一轮应查真实循环耗时，不能只看 owner 的 ACTIVE 标记。

完整工程交接见 `handoff/HANDOFF_Octopus_R11_2026-10-11.md`，契约见 `docs/OCTOPUS_R11_MOTION_AND_WORK.md`。
