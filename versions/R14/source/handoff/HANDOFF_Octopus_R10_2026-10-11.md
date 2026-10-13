# R10 handoff：临时分兵回流、到矿资金、持续侦察与空间投资

分支 `codex/octopus-g3-execution-20261002`，已验收起点 R9 `56a059831cc69dc8f2cf83ef8685e5369fe4dfa2`。产品提交 `d3be4f6`，最终文档 HEAD 见 Git。这是 **R10 工程候选，真人效果尚未验收**。用户报告 R9 首次真人原版胜利，记为 `HUMAN_REPORTED_NATIVE_VICTORY`；未重新检查该胜局。本轮较低难度隔离自然局另有 native VICTORY，不能称为真人验收。

## 事实输入与根因

本轮只读分析 R9 `runs/20261010-193513-26836`：raw SHA256 `414446c4e9d883c0d04976a5c9d4bbe7f6c5d2c399ea5a850f630a104e932c25`，852949 行、损坏 0。它不是用户报告的胜局；该 run 单倍约3600秒被 wall safety 截断。528条本地矿缺钱事件、EXPLORE/RECHECK任务生命周期、陆军和空海共同资金入口是本轮依据，不能把日志次数当任务数或损失数。

已施工的共同根因：

1. 小机动目标被 whole-General allocation 消费；临时借兵缺少可恢复 origin。现在 Commander 与 LocalResponse 使用同一合法角色分类、原有粗响应预算，优先 FREE/补兵、JOINING，再少量借 General。借出期间保留原 reservation，clear 恢复原任务。普通未知角色不凭猜测归为小威胁，基地/战略目标仍走已有整队分配。
2. Builder 到矿前后只有软 prospect，普通造兵和 Capability 可以吃掉即将施工的钱。现在当前真实 native 可做的已到达工作区候选产生报价 bank，Strategy 先落实，普通生产/未支付空军意向让出该款。bank 期间不能重新 prospect 把自己的承诺取消。
3. Scout 将少量视野进展当完成，再退化为旧情报 recheck。现在同一任务/lease 滚动 route，后帧位置推进 cursor；RECHECK 只能临时借用，恢复原 EXPLORE。当前危险先绕路，找不到路保留任务，不把停顿当完成。
4. 所有陆厂一轮产品 runway 与追加陆军投资没有区分空间用途。现在合法当前地面粗需求已覆盖、己方 readiness 与未完成空海职责成立时，保护一份真实工程师/两栖报价，新增 capacity 让位空海。不是坦克数量封顶，也不宣布雾中大陆已经清空。
5. AirGeneral 原来只有一个 owner。现在 facade 下各 formation 有真实独立 owner/generation/roster/route/target，合法分离 frontier 才创建子队；JOINING 和后帧位置证明保持。实现通过 focused 合约，但自然创建情况必须读下方结果，不能用 fixture 当自然分队。
6. 人工观察没有常驻 native 时钟；7200入口传入的14400 wall safety 又被客户端截为3600。新 kit 默认直接只读 `/state` 显示 game time/frame，显式 wall safety 得到保留。

## 关键接口与保护

- `ForceController` / `GeneralRegistry`：临时 origin 正交于 allocation/task/membership；owner transfer 每次 generation++，旧 Intent 无效。FREE→FREE；PENDING+LR clear→JOINING；JOINING 恢复原补兵任务，重新取得执行/到达 witness；ATTACHED 借兵 clear→原 General ATTACHED/原 mission。Recovery、无效 General、外部接管不得复活旧 origin。这是用户 R10 临时借兵要求，原 G4 正式 detach→FREE 路径仍保留。
- `CommanderDirector.currentThreatTasks`：own 与 combat 端点独立 freshness/authority。own observation 绑定当前分配权限，task source 绑定合法 combat packet。不能要求两个端点同 frame/time；否则 focused 同帧样本会掩盖自然零响应。
- `StrategyDirector.Worker.mineQuote/mineReserve`：只有当前 ready/IDLE/owner、后帧到达当前工作区、真实 planned native 报价/候选/单位/session、当前风险与占用检查允许才 bank。旅行未到位仍软；沿用既有600 cluster，不再追加必须到矿心/最终 rally 的 gate。cost provenance 保存真实 endpoint observation/path，报价可以不是700。
- `BattleClient.strategyReserve()`：普通生产保护真实已到矿承诺；矿执行加回自身 reserve，避免双扣。accepted 转现有 spending/slot witness，receipt≠完成。UNKNOWN/paid ghost 不计时退款。
- `ScoutBridge` / `BattleClient`：EXPLORE 持续，RECHECK 临时中断；Builder resource-approach 使用合法已知 terrain/remembered danger 的 waypoint，静态可通行不等于现在安全。
- `MilitaryInvestmentPolicy`：HP coarse allocation 与 General FightEstimate 分离，UNKNOWN 不当零，`mainlandCleared=UNKNOWN`。没有固定飞机比例、坦克停机数、核弹或T2 scout aircraft。
- `AirGeneralController`：真实 `air-general:N`，分离合法 native AIR frontier，独立 contact duty；逐机 requested/desired/physical/weapon/terrain 仍分开。不是把两栖塞进 Ground General。
- runtime 仍是 force controllers 真 collect/arbitrate，其它成熟经济/Strategy链 caller traversal→immediate dispatch。没有恢复跨独立 actor 的人为 APM/token上限；actor冲突/owner generation、资金/slot/burst仍是执行正确性保护。

详细契约：[OCTOPUS_R10_FRONTIER_AND_SEARCH.md](../docs/OCTOPUS_R10_FRONTIER_AND_SEARCH.md)。

## 提交与验证入口

| 提交 | 内容 |
| --- | --- |
| `be26150` | 最小临时响应与 origin 恢复 |
| `8678317` | 持续探索、到矿资金、边际空海投资、独立 Air formations |
| `f824560` | native Human时钟、入口与回归注册 |
| `09e4fc7` | 首轮到矿/rally报价接线修正 |
| `b8bcc31` | 旧 frontier 测试对齐持久任务语义 |
| `6d0fc7f` | 当前 combat 与 own 独立端点 authority 修正 |
| `769735e` | 当前 native 到达工作区矿可直接 bank |
| `d0d4f9e` | banked worker 不再被普通 re-prospect 取消 |
| `d3be4f6` | 原生 own快照没有movementType，分队改用既有实际 mode witness；真实缺字段fixture与负例 |

最终构建 SHA256：`456a1fbc3870b26f57b503b7c719fc5cb04b634896c4bfe3aba715aef594a1dd`。

证据根：`G:\deepseek 工作台\_validation\octopus-r10-20261010`。新增 Java focused 总计610checks（含25 native ScoutBridge）；新增 Human时钟 Python3tests。原有16局部 suites与10项组合 focused均通过，避免把重叠 checks 重复汇总。native矩阵覆盖G3 credits/slots/burst、单机Mode、西班牙第三地形、G4/G5执行、G6制造与R9超时。受控 fixture 未自然模拟移动，不代替自然运行或真人表现。

自然复核另发现 `/state ownUnits` 不导出 movementType，但新增分队条件要求它为AIR；旧fixture多造了该字段。原候选低难度自然局已129ready飞机仍零split；6个8MiB冻结slice/226124行证明4293次飞机观测均缺该字段，355个runtime快照满足其它前置条件。`d3be4f6`删除这一错误读取，使用当前own ready +既有ATTACHED/非恢复/AIR_READY/实际nativeAIR witness，保留UNKNOWN/SUBMERGED/请求Fly负例，不修改Bridge或编造AIR。第二独立frontier的自然可得性仍由最终运行验证。

最终 full 与两局自然结果由 [validation-summary.json](../evidence/octopus-r10-2026-10-11/validation-summary.json) 保存；原失败证据不删：旧 Python frontier 仍期待 reveal 后任务结束，修正其过时契约；独立端点与 bank continuity 在旧候选上复现失败后修正并重新验证。未因这些问题提高 decision frequency。

最终 `d3be4f6` 同源 Windows：**114/114 PASS，79 Java harnesses，32 Python suites/506tests，3build，failed steps: 0**。13阶段最终native exit0。272个测试输入零漂移，1895个kit文件核对零漂移，独立full构建与交付JAR的210个内容entry一致（归档timestamp导致整体SHA不同）；受保护三文件原哈希不变。根证据 `final-verification-air-mode`、`native-validation/native-air-mode-final`、`delivery-verification.json`。

## 普通难度自然运行与小响应

最终同SHA `spain-air-mode-normal-4800`，原地图/player0/5×/difficulty **0**，无状态注入。预算正常结束 **PARTIAL/ONGOING**，无系统崩溃，未取得胜利。raw SHA `c5e833abbe64e0874a95dbbe280dad34b3823b44904d5a2020195d422c1e27e7`，461357行、损坏0。

最后完整own 4793.376s：32ready重坦、4ready两栖、2Builder、2工程师、3矿、2T2陆厂、2修复湾、现金375。该局持续承受压力，未建立稳定经济优势，**不能称R10对战质量已提高**。新款预算的4笔到矿承诺均取得原生ready证据，筹款→accepted中位9.344秒/最大50.880秒，筹款→ready中位38.368秒/最大60.032秒；不代表所有矿点等待都解决。

721条单位临时响应接管记录，369条恢复原General、260条恢复JOINING、82条恢复FREE；这是单位状态转换次数，不是721个ThreatTask或击杀，Recovery/失效等中断不会强制恢复旧origin。原General任务保持，继续出现6533条General目标命令accepted；这仍不是每条命令有效推进的证明。当前只一支AirGeneral，31搜索段/18后帧到位/120攻击receipt，未满足多队规模，末期未结账0。现有单LocalResponse并未承诺多地点同时派多支小队。

## 已收齐的低难度自然终局证据

`spain-air-mode-diagnostic-4800` 使用最终同SHA、原Spain地图、player0、5×、原版difficulty **-2**；没有注入资金/HP/位置/fog，不是fixture。原版结果屏、最终Bridge match与raw summary均为 **VICTORY**，约47游戏分钟提前结束（final-state 2838.176s）；它不是普通难度或真人胜率验证，也没有固定随机种子，不能用前后不同局声称胜利由单一修正造成。

raw SHA256 `31f0433bb42521fc7778aa229cc8cbb390ddf94371b1a65f02e606646574dc3a`，163277行、损坏0。最后完整 own 2798.288s：188ready重坦、121ready两栖、24Builder、4工程师、16矿、2T2陆厂、2修复湾、2重炮、现金162851.5。15笔 arrived矿bank取得accepted，其中14笔后帧ready；accepted延迟中位0、最大36.288游戏秒，ready延迟中位22.624、最大59.728秒。剩余accepted没有ready记录，保留UNKNOWN，不声称15座全部完成。

最多18支独立AirGeneral，634段搜索、621次后帧实际到位witness，18个owner都取得位置witness；37条attack receipt不当击杀。地面粗需求covered/unfinishedair日志103次是决策理由变化/记录，不等于103次独立投资。修复、地面补充与矿升级继续，没有坦克数量停机或固定air比例。

只读复核证明621/621搜索witness都有同owner/actor/目的地的较早native MOVE receipt，更晚own sourceFrame；17个子队都在JOINING之后、严格更晚own frame以6/6成员ATTACHED，最多18个不同frontier同时存在。owner3在1843.552～1881.744s接敌期间，5个其它owner继续取得搜索到位witness。owner4与owner8各有当前合法contact loss→clear→原frontier/route hash保留→SEARCH receipt→后帧到位，cursor分别0→53、14→60；失联不计击杀。精确行号、packet身份与复现路径见 [air-natural-continuity.json](../evidence/octopus-r10-2026-10-11/air-natural-continuity.json)，没有把UNKNOWN label或compact省略数组当消失证据。

仍有82笔/164000施工付款效果未结清；当前自身现金会被保守shadow扣至0，不能称资金转化已经完全解决，也不能timeout退款。原版容量精确接口、移动施工产品的历史effect消歧、Scout自然长期存活/替补、分散小编队在真人强空军下的交换与真正水下首次发现仍需下一证据。Builder覆盖与损失/替补效率不能仅凭末期24个证明。

Scout共同代码的自然局诊断：同Task/lease/generation连续23个MOVE（23 execution witness/22到位）、7次同lease续route、3次危险绕路，无Scout RECHECK打断；165.328s因SCOUT_DAMAGED退出，随后进入Recovery，退出时精确HP投影缺失，之后己方缺席不能当死亡。最终候选两局首Scout也只观察到早期活动；长期存活/恢复后重新侦察/替补仍未证明，不把“任务不因少量视野结束”夸大为整局不间断侦察。

## 人工候选（最终入口）

只准备、未启动：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R10E`，port **47695**，上面同SHA。旧 R10/R10B/R10C/R10D 是过程中间构建，不用于本候选验收。

用户 `1-Start-Game.bat` → Spain/对手/速度/开始 → `2-Run-Agent-7200.bat`；另1200/2400。控制台标题与新run的 `rw-agent-live.html` 顶部持续显示 `Game mm:ss.s | Frame N`；失败/换session显示UNKNOWN，绝不用wall推算。14400wall safety适合单倍7200入口，预算结束是PARTIAL。Agent结束不关闭用户游戏。

[人工说明与观察点](../docs/OCTOPUS_R10_HUMAN_VALIDATION.md)。原版/设置/存档/回放未操作，未push/deploy；用户launcher修改与backup三文件原哈希保留、未stage，整体工作区有意dirty。下一任务从本产品与本handoff出发，不reset/clean输入。
