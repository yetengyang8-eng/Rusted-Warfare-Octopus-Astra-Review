# R9：攻城连续性、经济前沿、空中制造吞吐与局部生产容错

基于 R8 `775e5c4`，保留 Ground General / Commander / Recovery / FREE / JOINING / 重坦普通生产 / AirGeneral 主循环。R9 没有提高 decision frequency，没有恢复全局操作额度，没有读取隐藏敌情。

## 当前攻城任务

`GeneralCombatDirector` 复用既有 combat ledger：此前实际 accepted ADVANCE、相同 owner generation / roster revision / 当前目标，以及后帧当前己方成员已经进入当前可见可信基地射程，共同构成已进入攻城的证据。accepted receipt 本身不证明进入、开火或伤害。

该当前任务优先于普通远处 ThreatTask 分配变化；普通 Tactical STANDOFF 不再因轻微估值变化或把已经承受过的损伤再次算作接近损失，而终止已进入的合法攻城。当前合法敌情、实际严重受损、攻击兼容性和真正退却条件仍然有效。失联不继续攻击 ghost；generation / roster 变化不复用旧授权。

所有重新定位/退出路径检查已知基地火力圈。安全的当前位置仍可以 hold；当前位置已经处于火力圈内时，候选 MOVE 目标在已知圈外，并保留到达余量。这里证明的是几何目标，不是完整路径安全或实际到达；native 移动校验继续权威。没有新的独立 siege enum。

## Builder 施工前沿

`BattleClient.constructionMilitaryFrontsStrategy()` 仅导出 ACTIVE General 当前合法 ownUnits 中健康、ready、NORMAL、真实由该 owner 控制的 ATTACHED 地面成员位置。基地、任务目标、cached centroid、General 数字本身都不替代实际空间展开。

`StrategyDirector` 将上述位置附近静态可达且有资源施工机会的区域加入 prepared construction fronts，复用既有 cluster 范围；资源位置先验不是空矿/安全证明。Builder 并发需求来自当前空间工作，并对就近空闲、已施工、已承诺工人进行覆盖匹配。远方未展开地图不因此产生 Builder 购买。

已有 prospect / 支付施工 / 区域承诺保持空间连续性，IDLE 不等于没有区域任务。普通基地 repairbay / fabricator 优先使用家附近空闲及未承担前沿的工人；确实缺少家附近施工能力时走既有 Builder 采购。保留当前合法威胁响应。最终矿点、报价、建造合法性和现场效果仍经过原生链。

## 后期空中投资

现有 `LongTermCapital` 继续采样自身现金历史。新增制造服务证据为真实 accepted native-priced amphibiousJet 施工 → 后帧当前己方 ready 产品 witness；计时器、空队列、库存数字均不当完成。

当已有终局空间职责、长期现金仍增长、当前制造者已经实际获得近期两栖工作且有完成服务样本时，用观察到的净现金流和单制造者实际服务支出速率产生额外 provider demand。实际采购仍使用当前原生报价、资金/slot/ownership；pending provider 计入承诺，避免重复购买。第一名 provider 仍沿用原启动路径。

多个独立后方工程师可并发制造；General / 空海编队生命周期保持独立。普通陆厂生产不按库存数量停机，也未被新的固定两栖比例取代。额外空中制造缺口尚未覆盖时，fabricator 的新建/升级让位；正常地面生产 runway、恢复和当前基础设施需求仍保留。

这次解决的是后期边际投资实际吞吐，**不声称已建立完整“大陆已清空”判据**。日志 `mainlandCleared=UNKNOWN`；合法未探索空间、当前 AirGeneral / Capability 工作表达职责，不从 fog 推断敌方是否已经消失。

## 局部生产 witness 超时

`BattleClient.updatePending()` 的 180 游戏秒窗口变为局部 `production_witness_unconfirmed`，状态 UNKNOWN，保留具体 reason。生产队列必须为明确有限非负数才是当前 queue evidence；缺失、负数不冒充空队列。upgrade 完成必须当前 tier witness。

当前明确空队列且该 producer 没有未结账 queue commitment 时，可退休本地 Pending 预期；native 队列仍忙、未知或尚未观察到付款效果时保留对应承诺。`NativeCreditWitness.hasUnsettledQueue()` 只是查询，不退款、不通过计时器释放 slot。一次局部异常不再抛出终止整个 BattleClient 的异常。

无法消歧的 paid-before-visible obligation 仍可能保守占用该 producer。R9 不把这种不确定性扩大为整局停机，也不伪造 readiness / spending effect。

## 证据边界

R9 focused tests 证明状态与几何/预算合同；原生 fixture 证明实际菜单、命令、队列、模式与保留账，人工 clock/position/fog 不冒充自然行为。Spain 自然运行的实际结果、仍缺证据边界及同 SHA 人工入口见 R9 handoff / validation-summary。

最终自然局当前移动单位正好1000；用户确认原版存在单位上限，精确合法native capacity接口仍待证据，政策slot不是原版人口。另有439笔/876700未结账：当前施工点group匹配未持久化已合法看见但随后飞离的移动产品effect。不能把计时器当付款证据，也不能把所有accepted判成实际执行。下一断点是实际容量与军种空间职责、持久一次性施工effect witness；当前候选不声称终局胜利或全部资金结清。完整边界见 [R9 handoff](../handoff/HANDOFF_Octopus_R9_2026-10-05.md)。
