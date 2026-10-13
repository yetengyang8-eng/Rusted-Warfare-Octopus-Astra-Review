# R7：前期运营与 G6 空海终局起步

本轮从 R6 `8714448` 延续。Builder、Scout 和产能统计修正沿原运行链生效；G6 空海／资本链由 `-Drwagent.g6=true` 开启。关闭该参数保留地面与 SpecialistPool 行为，不等于回滚本轮前三项修正。

## 运营输入与责任

- 固定地图的全部矿区是未来探索需求，不能直接变成即时 Builder 采购。当前可合法施工的独立方向、原生设施任务、现有忙闲施工者与承诺，才形成即时并行工作。静态矿点不证明无人、安全或可建。
- 开局 Scout 有持续的信息任务。少量新视野是进展；后帧到达路线段末端才接续。当前危险使路线改变；没有安全路线保留任务、使用现有规划重试等待。严重伤害仍回撤，修复后应恢复独立探索。
- 产能窗口按同 session/player、递增原生 frame/game time 的端点做保守积分。150 秒窗口的最大可桥接间隔是原 80% 覆盖要求的剩余 20%，即 30 秒；常见 13 秒循环不再直接丢弃。替换工厂不能继承旧身份的忙碌证据，长空白／无来源／旧菜单仍为未知。积分不是连续原生执行证明。
- G6 开启时记录主要运行阶段的墙钟耗时；包含嵌套 IO，可能重叠，不冒充 CPU profile，也不改变 decision cadence。

## 独立空海编队

`AirGeneralController` 是有长期职责的空海编队，不属于普通 Ground `GeneralRegistry` roster。当前仅一支 `air-general:1`，不实现动态空军将军数量／编制系统。

启动依据是当前健康普通地面力量已经支持现有 active target、原生重坦报价已知且没有当前本土紧急状态。它只说明己方可以支持额外空间职责，不声称已击败敌大陆。制造仍保护实际预算；最近一张飞机账单不能通过提高历史消费率而撤销自己的后续角色。若阶段尚未成立，原 Capability 链继续工作。

```text
SPECIALIST_POOL 中可用、无当前任务的飞机
  → 真实 owner transfer / generation++
  → air-general:1 JOINING
  → 后帧自己的位置在编队锚点附近
  → ATTACHED
  → 持续共同 AIR 搜索 / 当前合法目标作战
```

后方无任务工程师使用当前原生 construction-plan 制造两栖飞机。缺少后方制造角色时沿现有真实工厂菜单采购工程师；已经承担 Capability 的工程师不算闲置制造者。接受施工之后仍等真实 paid-before-visible、工地／ready product witness，不因空队列宣称完成。

终局采购明确保存 `ENDGAME_AIR_PROVIDER` 用途，不能继承当前另一个 Capability Need。ready 工程师匹配后在原 strategy owner 下保留后方角色；暂时缺少飞机资金时，普通 Capability 自动分配也不能把它重新领走。采购队列空了而 ready 工程师尚未合法匹配，后方用途仍保留，不重复购买。这是长期制造职责，不是另建 command owner，也不抢已有 Capability 承诺。

搜索消费玩家自身 fog/history 与既有原生 Scout plan。AIR 未知格通行来自已验证原版 AIR 移动规则，不读取隐藏单位、隐藏地形成本或全局动态 path grid。执行沿 plan 的安全路线消费拐点，各成员通常前往共同路点；确实不安全时取得自己的 detour。没有替代路线时取消危险旧 MOVE，而不是只清策略标志让旧命令继续。

搜索本身用 MOVE，不从未见区域推断敌人。`SearchAreaNeed.Domain.AIR` 在“当前陆军无合法目标／路线、存在合法未覆盖区域”时形成需求；全图 AIR 任务可继续探索。现有 WATER/ISLAND 分区与独立 SearchTask 分配尚未激活。

当前合法可见的 AIR/SUBMERGED、原 Capability 需求、无当前陆军路线的目标或编队附近目标，可进入重新读取的原生 engagement。不同合法 approach 坐标分别分组，每组最多 48。失联立即失去新攻击授权，清除不等于击杀。

每成员的 requested/desired/physical/weapon/terrain 证据保持独立：

- 当前水下目标需要 Dive 时，水上成员先请求；陆地成员自行移动到合法水域。
- 等待 Dive 的成员保留模式承诺，不被搜索分支反向 Fly。
- 152/151 回执不是物理模式；独立 `/combat/unit-modes` 与当前 engagement witness 决定实际兼容性。
- 一个成员起飞不改变别人；已兼容成员可以先作战。
- 物理潜水、WATER movement、可用水下武器、当前地形攻击合法性不能互相替代。

严重伤害暂时走向当前己方修复湾／home，后帧 HP 恢复才继续。空海恢复目前在自己的 controller 内，尚不计入普通 Ground RecoveryPool 的修复容量模型；不改普通 FREE/LR/JOINING/ATTACHED。

## 长期资本

`LongTermCapital` 保存己方真实现金历史，使用 120 秒观察跨度的净增长，不用“现金超过固定 X”启动。日志说明净增长来自端点，既不是连续消费测量，也不是敌方优势判断。

空军追加供给由实际剩余收入和当前原生飞机报价估算，规划库存是已有／承诺飞机加上这段时间可追加的供给量。这是组织诊断，不是生产开关；“120 秒不足买一架”或已有飞机数量不能阻止真实闲工、有钱、合法原生飞机项目。观测消费包含普通与专家生产开支，不冒称纯陆厂 DPS 或原生吞吐。地面生产保护当前一轮真实重坦报价；资本还保护下一架飞机报价。已经有空海编队和后方制造投入、无真实扩厂／恢复缺口且现金长期仍增长，才沿原生 Builder construction-plan 建造 home 附近资源制造仪，并沿投资菜单升级。

不要求填满全部 air target 才屯蛋：收入增长不能让不断扩大的飞机目标变成永久等待。当前可负担的本地矿优先于资源制造仪；远方闲置 Builder 可以沿旧 RETURN 回家，后帧到位再取得 home 的当前施工报价。当前活跃扩矿施工者不被强夺。

原版 native focused 已验证 T1 建造 2200、T2 升级 4400、T3 升级 7500；这些是该冻结版本的验证结果。运行决策使用真实菜单报价，不把参考数字硬编码成资金权威。

## 近邻施工的资金结算

自然 Spain 发现两个工程师相距40 world制造飞机时，旧120 world施工关联范围互相覆盖。纯几何歧义把已出现的施工效果持续判成不能结算，重复制造最终累计33400未结资金，挤压后续飞机／资本预算。

`NativeCreditWitness` 现在按每笔承诺的真实接受前基线、合法后帧和产品／位置建立候选关联。相连的一组必须有 N 笔合格承诺、N 个不同的后续原生施工效果，且存在完整匹配，才整体结算。少一个、多一个、未知进度、未超过回执帧或不能完整匹配仍保留；不以计时、钱包、空队列或缺席释放。

Trace 使用 `NATIVE_CONSTRUCTION_GROUP_EFFECT_NOT_INDIVIDUAL_LINEAGE`，包含稳定的 `constructionGroupIntentIds` / `constructionEffectUnitIds`，明确 `individualProducerLineageClaimed=false`、`productReady=false`。它不把某架飞机归因到某位工程师，也不以施工初态声称自然完成。原生 focused 使用两名真实工程师、原生命令处理及建造初始化，验证只有第二个独立效果出现才释放整组资金。

## 执行与证据边界

Ground force 保持真实 collect → priority → dispatch。Recon、Strategy、空海 controller 和成熟经济链仍在现有调用阶段即时提交；没有宣称全局 all-lane batch。G5 独立 actor 没有人为总操作额度；同 actor、owner/generation/ABA、重复 order、credits 与 slots 保护继续有效。

原生 fixture 可证明菜单、命令、付款、队列、owner/mode witness。人工改 clock/fog/position/height、seed ready unit 的场景不能证明自然完成、搜索效率或胜利。Spain 自然局结果和最终候选 SHA 见本轮 handoff；预算结束的 PARTIAL/ONGOING 不是 victory。

尚待证据：全图水下残敌的主动发现、危险下群体存活效率、最终原版 victory，以及真人攻城和运营收益。Spain 第三类地形的模式与选目标已有 [G4 有界核验](../handoff/HANDOFF_Octopus_G4_2026-10-02.md)：不能就地 Dive，潜水后的 target selection 按目标而异；不能概括为不能攻击一切。自然潜水移动进入及实际发射／伤害仍 NEEDS_EVIDENCE。本轮没有读取隐藏敌人来补齐这些能力。
