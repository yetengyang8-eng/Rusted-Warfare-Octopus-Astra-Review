# R14 施工与空军回查候选交接

收束于 2026-10-12。分支 `codex/octopus-g3-execution-20261002`。起点 R13B `56d5b7f`；产品 `1c0a98d`，旧日志对照校准 `de2b02b`，交付文档 HEAD 见 Git。用户授权实现/必要重构，要求保留旧版、节约额度；本轮复用两个 Sol 子任务，主代理只跑一次完整矩阵，再重跑两项受影响套件。

## 结论

Builder 的已证实卡死链、重炮漏编组、空军逐机查询、硬 frontier 预约、显示历史积压均有可运行修复。**R14 是可供人工验证的工程候选，不是空军/终局质量已验收。** 普通 Spain 自然 7200 秒预算结束仍为 PARTIAL/ONGOING，未取得 native victory。空军阻塞责任与巨型主队耦合仍明确存在，应作为下一断点。

## 真人证据与本轮施工

事实入口：[human-causes.json](../evidence/octopus-r14-flow-2026-10-11/human-causes.json)。原始 run 为 R13B `20261011-221046-27816`，player5 / Normal，458007 行，0 malformed。用户确认先退出游戏；末尾 Bridge 超时不能当中途崩溃。

1. **Builder**：原生 resource approach 截断六格；途中路点被记成长期矿区。1489 号工人在旧 anchor 外接受实际矿施工后，仍在无固定矿的旧区域产生 216 次 UNKNOWN/空候选保留。现在最终 approach 与当前导航 leg 分开，已知安全直路可直达最终 approach；接受本地造矿清掉旧 prospect。区域是软偏好，没有可用本地工作时允许合法其他旅程，UNKNOWN 不会被伪造为探清。原生 build/repair、报价、资金预约与 ownership 保留。
2. **重炮**：独立 `heavyArtillery` 在旧 catalog 中缺失，既有 LAND 编组与静态 eligibility 因此漏掉它。新增同 gameJar、独立 SHA 校验的原版 profile，保留旧 catalog；现有控制链即可处理，不增加将军规则。截图附近的 25209 是当前可见 AIR 直升机，原生重炮 INCOMPATIBLE 正确，未赋予重炮对空能力。
3. **Air**：己方 mode 改为 1..48 身份分批读取，847 成员 fixture 为 18 请求；逐成员 UNKNOWN/mode/receipt 边界不变。Search 当前 lease 与 soft avoid 分开，所有 native fallback 排除硬预约。失联点被记忆 AA 圈阻挡时，可规划安全观察站位、多段路线；当前局部 visibility + 随后合法空 contact packet 才释放责任，clear 不等于 killed。
4. **空海投资**：互补空海职责不再等整个 activeTarget 都成为 75% HP 的现役兵。成熟最低规模的可用地面力量允许角色存在；普通地面边际投资判断仍使用当前合法需求与计划编制，保留 emergency/真实报价/资金/slot 保护。未声明大陆已清空。
5. **显示**：`battle-latest.json` 原子替换最新 runtime/provenance/summary，完整 archive 保留。Human panel 不再靠追赶 gzip 历史显示当前状态；Bridge Game/Frame 与 Agent sample 分别标注。只读显示旁路不参与决策。

契约：[OCTOPUS_R14_WORK_AND_AIR_FLOW.md](../docs/OCTOPUS_R14_WORK_AND_AIR_FLOW.md)。未提高全局额度/决策频率，未实现敌方红线锁定字段，未 push/deploy/启停桌面游戏。

## 验证

- 完整 Windows 初轮 **132/134**；99 Java、32 Python，失败为旧 G1/G2 把新增原 catalog actor `sourceId` 当行为差异。实际 wire 命令与原决策一致。
- 仅对这一份冗余旧来源字段做规范化；新增/未知来源、命令与决策字段仍严格比较，并新增防止过度规范化的测试。G1 **5/5**、G2 **9/9** 重跑通过。同一产品 JAR未改，最终 **PASS_COMBINED_MATRIX 134/134、508 Python tests、failed steps: 0**。不是声称首轮全绿。
- 初次局部重跑因测试副本缺少 tools import 路径而报错，保留在 `trace-comparison-correction`；显式补 PYTHONPATH 后 `trace-comparison-correction-v2` 通过。没有产品改动救环境错误。
- 六个新增 focused/native 套件共 1872 checks，重炮缺失/损坏 profile 另 12 checks。native 重炮14、Air65、Builder/Scout Bridge26，均明确 fixture/零 simulation ticks，不充当自然移动或开火胜负证明。
- 一次普通 **difficulty0 / player0 / 5× Spain**，同一候选 SHA、无位置/资金注入。预算 7200 秒结束、Agent exit2、PARTIAL/ONGOING；598296 行、0 malformed。与真人 player5 不是受控 A/B。
- 122 次 accepted build-extractor，121 次后帧己方 ready 矿位置匹配；87 次本地机会中断软 prospect。匹配不是独占 builder lineage，不能推断没有其他建造参与。
- 两栖首次 ready 的完整快照在 **15:45.6**；末期完整快照 **38 矿、6 陆厂、113 重坦、546 ready 两栖、3 Builder、现金 1,019,707.5**。不同时间的独立 monitor 多看见未 ready 飞机，勿拿548代替546 ready。
- 最多同时6支 Ground General、10支 AirGeneral。自然记录45次后帧搜索 leg witness、83次局部 area clear；都是事件次数，不是独立击杀/目标数。

证据：[validation-summary.json](../evidence/octopus-r14-flow-2026-10-11/validation-summary.json)、[windows-matrix.json](../evidence/octopus-r14-flow-2026-10-11/windows-matrix.json)、[spain-normal.json](../evidence/octopus-r14-flow-2026-10-11/spain-normal.json)。详细输出 `G:\deepseek 工作台\_validation\octopus-r14-flow-20261011`。

## 必须保留的失败/未知边界

- 末期两支 AirGeneral 中，主队 roster539，另一队4，仍被约70分钟时取得的旧区域责任牵住。1452次 `air_general_area_blocked`，主要巨型主队仍不断接收新飞机。原生安全观察规划能执行，不代表它一定能取得足够视野；本轮未完成 blocked duty 的跨 owner 移交或有限兵力调查责任。
- 下一步优先处理“保留调查责任不等于捆住整个编队与后续新机”，并核实安全观察站位的真实视野覆盖。不要放宽 ghost 攻击，不能把 blocked 日志当解决。
- 本轮汇总时曾把完整 own payload 关键帧间隔30.6秒误称循环间隔，已纠正。**所有3328条 observation 的原生时间戳**复核：最后5分钟44间隔，中位数 **6.648游戏秒**、最大10.256秒。完整单位快照约30秒采样，不是decision gate。R13B全部2680条 observation 的末期中位数31.050秒，但其单位规模更大，不能声称受控4.7倍加速。
- 现金仍百万级、终局未完成；批量查询减少已证明，整局效能和同规模性能改善未作受控证明。Scout 此局仅早期有位置样本，不能声称长期生存或替补已解决。

## 交付与保护

人工目录 `G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R14`，port47703，JAR SHA **c984f4f98d644a16ddcc9c79627242ec5467eac8a0229459badee4d46c991317**。

`1-Start-Game.bat` → 用户自行开始 Spain → `2-Run-Agent-7200.bat`。默认 Game/Frame 与最新状态；详细 [人工入口](../docs/OCTOPUS_R14_HUMAN_VALIDATION.md)。只准备未启动。

R13B 原目录和 JAR 保留；Git `backup/r13b-before-r14-20261011` 指向原 `56d5b7f`。新 kit 1895 文件零漂移，产品源与测试快照零漂移。两个人工 launcher 改动及 `.bak-20261003` 的原 SHA 保持，未 stage；工作区最终有意保留这三项 dirty 输入，不得 reset/clean 删除。
