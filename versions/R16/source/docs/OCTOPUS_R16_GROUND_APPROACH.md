# R16：陆军基地接近与 Builder 区域连续性

起点：`1311f2a`（R15）。本轮限定陆军与普通本土施工调度；不改 AirGeneral、AirResponsePolicy、FightEstimate、CombatProfiles、TargetCatalog、Bridge、command gate、生产或资金分配。

## 陆军实际接战力量

General 名册、当前参与接战的局部兵群是两个事实。临时分队恢复原 General、成员移动速度和路线不同时，ATTACHED 可以分散在很远的位置。owner 正确不等于所有成员能同时输出。

GeneralCombatDirector 在目标进军和普通 frontier 路线两处统一检查已知基地接近：

1. 检查每个实际 owned actor 到目的地的线段是否进入已合法记录的基地圆，不能只用全队平均位置。
2. 以距基地最近的当前成员为接战前部，使用现有 `LocalArmyDirector.FORM_RADIUS=400` 取附近成员。远方名册成员不为前部承担即时 HP/DPS。
3. 基地当前合法可见时，取这一局部兵群的 native eligible 子集，复用原 FightEstimate / UNKNOWN exposure 规则。也会评估便宜目标附近的基地，不能按矿或 Builder 的 HP 给整个基地定价。
4. 只有基地记忆时，保留原“11 以内小编制保护，超过放行”的规则，但人数是当地实际兵群。记忆仍只提供行进约束，不给攻击权限。
5. 不足时复用现有 STANDOFF / 原生 MOVE 到基地射程外；后帧兵力聚拢、基地血量变化或防守减少后重新按现有规则评估。没有新增成军计时器、攻城状态机或固定最低攻城兵数。
6. 原 R9 已进入有效攻城的连续性、真实损伤撤退、receipt/arrival、owner generation/ABA 均保持。

诊断位于 `g5_general_transition.after.baseApproach`：`enemyId / reason / admitted / rosterCount / localActorIds / formationRadius / fightEstimate / geometry`。局部圆与线段是自己的位置几何，不是原生路径安全、同时开火或战斗胜利证明。

## Builder 区域归属

普通矿施工完成、清空报价或短暂看不见周边资源，只释放那一项施工，不把已在外部经济区的 Builder 自动变成基地闲置工人。

- 超出既有 home construction 半径 650 的 Builder 保留区域施工职责；普通 repairbay / fabricator 不跨图召回它。
- 基地已有空闲 Builder 仍可承接任务；原有当前施工缺口与 Builder 采购继续工作。
- capital 选择还会查看当前原生 order，`repair/build/move` 不能因 Worker bookkeeping 是 IDLE 而被当作空闲。
- 实际敌情撤退、失效任务返回等独立安全路径未删除。所以“常规施工召回取消”不等于“Builder 永远不得回家”。

边界：远端区域彻底开发完成后如何显式重新部署，以及本土缺工时新 Builder 的抵达速度，留给真人观察；没有新增永久 Worker owner 或预留工人数。

## 验证方式

R16 focused 用同一组 fixture 在 R15 编译产物上复现失败，在修改后通过；这证明两个调度缺口被关闭，不能证明实际损失率。

旧 G6CapitalPolicy / G55Operations 两个“应召回远端工人”的断言随用户新要求修改；改为保护区域职责并验证本土施工仍能执行。原 full 失败记录保留。

完整回归、同参数普通 Spain / player 5 的 R15–R16 自然对照、冻结 JAR 与 Air 相关 14 个 class 的字节等价见 handoff/evidence。单次自然对照不用于宣称稳定胜率或杀伤归因。
