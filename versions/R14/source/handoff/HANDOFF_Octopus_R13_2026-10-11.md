# R13：施工现场、渐进分兵与持续搜索

起点 `eef352fafdf1392a588a1b7be41d8003c2512531`；分支 `codex/octopus-g3-execution-20261002`。产品 **36c9fa6**，最终文档 HEAD 见 Git。这是工程候选，未标记真人验收通过。

候选 JAR SHA256：`8f40e5ec9616f16acbe9bf12a30d47a2f72a48ca392fb96eb8869a8babaa6206`。最终完整回归、最终自然局与 R13B 人工目录使用同一份归档字节。

## 已确认根因与实际修改

**Builder 的无效忙碌**。R12 原始选局分析位于 `G:\deepseek 工作台\_audit\r12-human-agent-20261011\REPORT.md`：Builder 9014 在 11:27.190 取得700报价，11:30.725开始当前 native plan 已明确无合法工地，但旧 optionalGet 折叠成 null，资金链继续保留旧单并提交，累计38次拒绝。有效资金已超过700且 token充足，不能归因于全局命令上限。另有真实缺钱的 Builder，不能把不同原因合并。

现在 PROSPECT 只是软行程，当前位置的真实 native 本地矿可以抢先施工。当前 `build/repair` 不被覆盖；真实现场报价形成精确资金承诺，旧远途 MOVE 将离开工作范围时取消该远行筹资，不对 idle 刷命令。`no-current-work` 与 UNKNOWN 分开，当前拒绝只释放未付款意向，UNKNOWN 不能用旧报价重试。价源仍指向实际报价 observation/path，最终 native guard 未放宽。

本轮自然局确实暴露一个新接入遗漏：已到区的旧 backlog 分支把 `no-current-work` 当带坐标的计划，导致 NPE。失败 raw 保留；新增用例先复现同一行栈，再将该调用点改为验证当前合法 quote 后读取坐标，随后重新冻结最终候选。致命异常保留 stderr stack，避免下次只有异常名称。

**远程 ThreatTask 搬整军**。过去 Commander 粗需求足够便可让 General 改方向，小响应和失联责任之间存在空档。现在 taskResponse 使用现有 temporary owner/return 路径，优先 FREE，再借少量 JOINING/General 成员；主队保留方向。当前新敌情才能增援；明确无武装高HP建筑不按总血量搬军。整军改变方向须达到当前战线级别需求。失联由分队 MOVE 回最后合法区域重新取得视野，不能攻击 ghost；完整当前区域证据支持清除后恢复原组织，CLEAR不是击杀。

最后复核还发现“借来的成员暂缺 own row→算零容量→新packet再借一份”的风险。已用旧产品稳定复现，修为保留 UNKNOWN 的既有 owner/generation 承诺，等待 own 恢复再判断差额；其他当前成员继续执行。没有新增全局操作门槛。

**Scout 与 Air 连续性**。Scout 进入已知转弯格后接下一合法段；新 route anchor 与当前 own tile 一致时同轮续行，仍保留 mission/owner。同轮 actor 已发过命令只是局部延期，不再误判 native 拒绝并释放 Scout。另一 actor 执行 RECHECK 期间 Explore 可以继续；不能为测试强制清空仍有效的路线。

AirGeneral 保留区域清剿责任，绕路/普通 Search/短暂失联不抢任务。失联撤销 native entity attack，改合法区域 MOVE重新看。威胁按成员当前 physical AIR/SUBMERGED 的真实原版武器兼容性计入；普通矿、无法对空工厂/炮塔不再靠HP数量推高AIR风险。对当前兼容目标按对自身有效的武器威胁优先，新增 native `/command/attack-target` 让选择成为实际实体攻击。原版再核验 visibility/actor/domain/session，receipt不等于伤害、到位或清空。

**新增资源方向**。复用当前可见陆地需求、己方实际ready力量和未完成空海职责的粗分配结果：地面已覆盖时闲陆厂允许暂停新增普通队列，去掉对应地面补充保留和未付款冗余技术意向，交给真实矿业/空军/资本链。当前压力或不足恢复补充；已付款队列不取消，不固定坦克数量，不推断隐藏大陆清空。

详细状态与证据契约：[OCTOPUS_R13_TASK_CONTINUITY](../docs/OCTOPUS_R13_TASK_CONTINUITY.md)。没有重做 General/Repair/经济主循环，没有增加全局额度，没有核弹或 T2空Scout工程。

## 验证与真实边界

最终完整 Windows **128/128、failed steps: 0**，93次Java、32个Python套件/506 tests、3步构建，exit0。286个agent/tools/knowledge测试输入哈希无漂移；人工包1896文件逐项核对。

最终同SHA自然局按用户最新要求停止高难度观察并转交人工，状态 **PARTIAL_USER_REDIRECTED_TO_MANUAL**。最后独立 `/state` 监测3332032ms（55:32.032）、frame208252；该样本为3矿、2陆厂、2修复湾、10重坦，未见Builder/两栖。不能仅由缺席断言死亡。保留的gzip spool前缀到完整own样本3204240ms，271346行、malformed0，未保存尾部可能缺失。前缀中24条建矿accepted、23处后帧新ready位置匹配，仍长期受压，没有进入空军规模/屯蛋/终局阶段。这不是完成7200秒，不是原版胜利，也不是对R12的受控改善。

首Scout603在监测中44.720–94.144s可见，最大离基地约1598world；仅证明早期移动，长时续航与转弯自然连续性仍 **NEEDS_EVIDENCE**。最终自然局未触发足够Air场景，因此空中防空优先、岛屿持续清剿和后期投资转换目前为focused/native链路证据，整体效果 **NEEDS_EVIDENCE**。按用户最新方向，下一次自然完整流程使用 **普通 difficulty=0**，再做较高难度压力；本轮先交R13B给用户，不续开新局。

新增七个 focused harness 最终合计 **491 checks**：Builder14、矿报价native4、Scout9、地面边际投资4、Threat分队405、Air生命周期41、Air native14。native 两项共18 checks为**零 simulation ticks、显式fixture准备**，证明原版字段/guard/queue/own order，不替代自然运动或战斗。

先前失败保留：第一次完整回归6失败（Scout同轮actor冲突影响两项，旧Commander/Recon fixture各一项，隔离stage漏历史docs/assets各一项）；中间 Scout“恢复时全清空”的假设被更完整事件和独立 Explore 测试否定，已撤回；中间完整运行在已知新问题后停止。最终结果不能混用旧日志。

首个前候选自然局 `spain-veryhard` 原版 **DEFEAT / 约28:47**，closed raw119024行、malformed0，不能称PASS胜局。17条accepted矿/16个后帧新ready对应地点，仅是位置与新实体证据。Builder10206于591408ms从PROSPECT让位给700本地报价，597200ms发build，647616ms出现对应ready矿。分队有278次当前攻击移动、246次失联MOVE，1–7人/次；20次从General借出成员后源队最少保留3人。返回事件及raw行号在证据包。主队也观察到约7.8秒真实无订单停驻后恢复，未证明所有短暂停顿消失。

中间 A35E 构建自然局 **FAIL / ONGOING**：最新 `/state`716768ms，Builder typed-negative触发NPE，Agent exit1。这是产品失败，非自然DEFEAT或测试环境问题；回归复现和修复在36c9fa6。无后缀R13人工目录是该失败构建，**不要作为当前候选使用**。

自然环境为原版1.15、MP97 Spain、非常困难 `difficulty=2`、5×、player0，无人工移动/注资。原地图SHA `342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746`。此前真人player5不同出生位/节奏，不能当同条件A/B。本轮没有复查或否定用户已报告的R9胜局。

证据根：`G:\deepseek 工作台\_validation\octopus-r13-continuity-20261011`。最终目录 `final-candidate`；`final-source`/`corrected`/`verified`是前候选与失败尝试，均保留。仓库小证据：[validation-summary.json](../evidence/octopus-r13-continuity-2026-10-11/validation-summary.json)。raw始终优先于summary全局指标，`completedMines`仍只覆盖旧baseline，不代表全局。

## 提交与人工入口

- `3b518cc`：施工本地机会、报价时效、Scout接续与边际投资。
- `907bb85`：渐进分队、失联责任与原组织回流。
- `f1e4ad0`：实际武器域、Air区域任务和原生实体攻击。
- `29ffc0f`：旧整军预算fixture与持续路线fixture校准。
- `36c9fa6`：自然崩溃修复、UNKNOWN已借容量去重及失败栈诊断。

人工目录 `G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R13B`，port47701。用户 `1-Start-Game.bat → 自行开Spain局 → 2-Run-Agent-7200.bat`；1200/2400也保留。默认从Bridge读取 Game Time + Frame显示在控制台/live HTML。[人工观察说明](../docs/OCTOPUS_R13_HUMAN_VALIDATION.md)。只准备，未启动该游戏。

未push/deploy，未启停或操控桌面游戏；原版引擎、旧候选、设置、存档、回放保持。用户原有两项launcher修改与 `.bak-20261003` 未stage、未删除、哈希保留，**整个工作区有意不是clean**；本轮源码/测试/文档/evidence提交完整。

下一断点：本轮交 R13B 给用户自行测试；后续工程自然局先用普通难度完整观察“就地报价后有效施工延迟、分队失联回查但主军不停摆、Scout转弯续航、Air优先拆真实防空并持续清岛、地面覆盖后新增资金转换”。若做版本对照，再保持相同难度、出生位等条件。若自然证据未进入空海规模阶段，维持 NEEDS_EVIDENCE，不通过提高全局额度或固定坦克上限制造通过。
