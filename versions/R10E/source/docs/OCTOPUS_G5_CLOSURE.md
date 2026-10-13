# G5 收尾 / Pre-G6：调度、交换、基地记忆、运营与恢复

2026-10-04，候选简称 G5.2 R3；这是 G5 闭环修正，不代表完整 G6。产品提交 e8d0e01、legacy gate 修正32064c6、远处建筑同样遵守预算8a4cad3；原版旧场景对齐fff1883，未知 profile 危机fixture对齐7cd9111。验收入口见 [人工测试](OCTOPUS_G52_HUMAN_VALIDATION.md)，交接见 [handoff](../handoff/HANDOFF_Octopus_G5_Closure_2026-10-04.md)。

## 本轮代码现实与证据

G51R 两份真人压缩 raw 经逐行解压、同记录引用展开：461918 / 400870 条，malformed 均 0。首份最大现金 565045.5、armedMobile 128、armyTarget 128、factoryTarget 4、General 6；包含 MOBILE_UNIT_HARD_CAP_REACHED 51 条、STRATEGY_ARMY_TARGET_REACHED 30 条。第二份最大现金 116060.5、armedMobile/armyTarget 112、factoryTarget 2、General 5，目标门控 53 条。首份同一任务吸引六个 General 的快照 450 条，五个 364 条；第二份五个 535 条。它们是日志记录数与合法关键帧最大值，不是持续时间或独立事件数量。

共同根因：Commander 没有统一响应预算，General 仍各自选远处当前目标；TacticalOpportunity 把厚建筑 HP 当火力代理；已看见基地没有持续的旅行否决；总库存目标、成军目标和软件上限耦合，普通菜单缺乏可见敌人时又可能被解释成无需求。修复沿用现有 owner、Intent、native guard 与承诺，不替换成新全局经济 AI。

旧迁移梯度中的 G5 协调与合法证据边界仍有效。用户本轮将旧 G6 残血 Scout/Reclaim 方向改成恢复池，本候选落实最小恢复与运营接口；SearchArea、完整 Capability Search/A7/A8、弹性编制、将军数量策略仍未开启。

## 调度契约

`observeG4Forces` 先处理合法己方 roster/伤兵，执行 LocalResponse 的实际选择与控制权预备，然后刷新被分走成员的 General 中心和战力，再交 Commander 统一分配。预备不发令、不消耗 token；force intents 仍 collect → priority → dispatch。

ThreatTask 新字段：`demand / localCovered / assigned / deficit / assignedGeneralIds / localResponderIds / allocationDecisions`。需求仍是当前 HP ×既有 1.5 系数的粗估，单位 HP/1000 为响应容量，明确标 `CURRENT_HP_ALLOCATION_PROXY_NOT_FIGHT_ESTIMATE`。先扣当前真实、兼容、具有有效 owner 的 LocalResponse；未承诺 FREE 不计虚假覆盖。随后优先选能一次补足缺口的一个 General；不足才扩大。选择成本包括距离、原目标中断和保留原任务偏好。FORMING、撤退/重组、陈旧己方证据不能被普通任务征召。

当前可信目录下，混合任务只计能兼容任务中所有目标的容量。这是保守交集，尚不等同于跨兵种组合求解。`allocationDecisions` 为每个 General 记录容量、距离、中断成本、是否被选与原因。所有远处目标，包括建筑，都必须有本队当前分配许可；不允许用“战略建筑”例外绕过小威胁预算。贴身220政策距离内的当前目标可以自行处理，包括自卫和无武装经济机会，仍经过交换估值和原版兼容检查。未知/失联没有攻击授权。

敌人从当前 visibility 消失：任务立刻 CLEAR，`killClaim=false`。已响应 General 可短暂留在自己的当前位置，最多 3000 game-ms，用 move，无隐藏位置追击。LocalResponse 同样立即清除；无 pending 的释放单位只有短暂 FREE staging，正式分配立即覆盖，pending 仍走 JOINING。UNKNOWN 不当作 CLEAR，也不授权新攻击。

## FightEstimate 与 Base Threat Zone

Commander 估值和 General 具体交换估值分开。`FightEstimate` 仅在原版 fingerprint 与目录身份匹配时使用已核验的 tank/c_tank、scout、heavyTank、commandCenter 四种静态 profile。原版基地 HP4000、range280、名义 DPS60；机制库的 HP3000 不作为 runtime 权威。对应出处在 [profile 来源](../agent/resources/knowledge/COMBAT_PROFILES_1_15.md)。

模型按兼容有效 DPS、当前 HP、射程/速度接近损失、目标 TTK、当前可见防守火力与预计己方损失比例判断。厚血无武装建筑只延长 TTK，不凭空增加 DPS。损失预算 .55 是候选政策参数；未知单位、未知兼容性或不可信游戏身份保持 UNKNOWN 并沿用保守代理。不是精确模拟器，不模拟完整火力衰减、阵形、弹道、治疗或击杀归因。真正撤退仍需要可信窗口内实质己方伤害/显式己方损失，看到基地或反空不足本身不触发整队逃离。保持距离、取消旧进攻、撤退均用 move。

合法当前发现基地后记录位置、range、来源 frame/time/session/player。失联仍保留防区，仅用于旅行否决，不宣称基地仍存活或授权攻击。当前合法明确 dead/HP0 才清除；桥是否能自然提供这种销毁行仍 NEEDS_EVIDENCE，空列表和 CLEARED 不能代替。防区内/穿过防区的 frontier 不再直接采用，被拒点进入最多 48 个点的已有侦察 avoid 输入；下轮选择其他点。已有单位沿直线持续向外并离开防区可以退出。

当前合法可见该基地，且当前兵力 FightEstimate 已准入，才放开相应防区。记忆只控制候选直线几何，并非原版实际路径、动态障碍或整条路线安全证明。未知基地 profile 使用有标签的保守政策范围。日志 `fightEstimate / localFightEstimate / baseThreatZones / blockedBaseZone / tacticalReason / pressureEvidence` 能解释为什么不踩、何时允许突破。

## 高收入转化与完整升矿链

G5 且 globalStrategy 启用、没有显式覆写时，普通单位软件安全上限由 128 变为 512；旧策略关闭或显式配置保留原值。`nativeUnitCapStatus=UNKNOWN`，512 不是原版硬容量。

总库存目标采用测量/现有收入、当前原版普通单位报价、己方现存 General 补员需求及有新鲜推进证据的任务需求。600 秒库存规划期、30 秒评估、每次最多增长 8 是政策，不是原版生产时长/寿命或单纯余额阈值。收入大于消耗、成熟开局、非本土紧急状态才放大；总库存目标与 active 成军规模分离，不因为库存增多直接产生超大 General。已有真实原版普通路线可以支持己方编制维护需求；UNKNOWN/ROUTE_UNAVAILABLE 不改成 KNOWN。

资金/paid-before-visible/slot/ABA/burst 保护保留。军事 floor 不得越过本次经济目标；下降目标也必须容纳现存兵力与未结算承诺。现有 150 秒需求、80% 工厂利用率、安全与真实报价的扩厂窗口保留，默认工厂政策上限仍 5。真人高收入长期能否持续扩厂、消化现金仍需下一局，不能由本次短局推断。

矿链接入原版菜单 T1→T2→T3→overclock/reinforce。强化矿3000、超频8000以当次原版报价为权威。超频还需持续180秒的保守低威胁窗口、健康、资金/补兵保护与剩余收益；超过6秒采样缺口重建窗口。该窗口是“当前合法未见威胁”，不证明隐藏区域安全。风险/信息不足的 T3 可走预算允许的强化分支；强化收入为0，ROI不伪造为增益。

同级 T3 转换若采样跨过整个队列，仍必须由唯一未结算 actor、同玩家、后帧原版产物类型改变及 buildProgress=1 见证花费。时间、钱包变化或空队列不能解锁。最终报价/action/current guard 来自原版；只改 EconomyBridge 自有链路，Universal Bridge 未改。

## 恢复池契约

普通 own ready 单位严重残血 `<25%` → `force:recovery`，真实 owner transfer/generation++，脱离 General、取消该单位补员 reservation 与普通 LR。HealthRole=RECOVERING，allocation/membership 等仍为正交维度。旧 attackMove 即使已在修复点附近也必须被 plain move 取消。

后帧当前合法 HP 达到 `≥75%` 才释放回 FREE，generation++，再由原流程分配；不自动回原 General。缺席不是死亡，move receipt 不是到达或修复。优先前往当前合法可见己方 ready repairbay，否则己方 home。伤兵少于4且总缺血少于1200时不专门扩建；否则目标 `max(ceil(count/8),ceil(missingHP/1440))`。这是规模/缺血量规划预算，不是强制每8人建湾，1440不是已实测原版服务率，没有新固定修复湾数量上限。

运营扣除当前已有/在建湾与 paid-but-not-visible 建筑承诺，复用 extra builder/engineer、lease、原版建设规划、施工 guard、资金与 slot，不抢 primary builder。`g5_runtime_state.recoveryPool`、`g4_force_transition` 与施工因果说明何时入池、缺多少容量、何时真实回到可分配状态。自然修复吞吐、施工完成、伤兵往返与恢复重新成军仍 NEEDS_EVIDENCE。

## 仍保留的边界

两栖仍 SPECIALIST_POOL；requested/desired/physical/weapon/terrain 证据不合并，缺任务闲置不通过并入普通军队来掩盖。完整 Capability Search/A7/A8、终局 SearchArea、动态编制、复杂战斗模拟或 Combat Performance 属后续断点。本轮没有提高 cadence，没有 collect-all 全局改造：force/recovery/General 真正批收集裁决，成熟 Economy/Production/Strategy/Recon 仍含 immediate dispatch。多个 lane 共享 token 的局限应从真实 runtime 日志判断。

所有阈值都是候选政策；focused tests 证明确定性契约，native fixture 证明原版接受/队列/付款/后帧 order，单局自然 headless 证明该局运行，真人效果只由新的真人 raw/replay 验证。
