# R14：施工旅程、混编陆军与空海回查

起点 R13B `56d5b7f`（产品 `36c9fa6`）。本轮依据 20261011-221046-27816 真人 raw，保留现有重坦、General、Repair、经济、Capability 与 owner/generation 控制链。

## 施工：目的地不等于途中路点

- `Worker.buildX/buildY + prospectResourceTile` 是资源工作目的地；`Worker.x/y` 是当前导航路点。
- `ProspectCommitment` 接收最终合法 approach，后帧己方位置到达该处才形成区域偏好。途经短路点不形成施工区域。
- `/scout/resource-approach` 从完整已知安全路径中选择最远的合法直线段，取消六格截断。直路可直达实际 approach；障碍、未证实通行区域和已知威胁仍会缩短路段。静态地图路线与动态安全不混同。
- 正在执行的远途 prospect 是软意向；当前本地原生报价可以接管。造矿 receipt 被接受后，旧 prospect 区域被清除，原生建造及后续完成见证继续沿原链处理。
- 区域内有可执行候选时仍优先就地工作；候选为空时不再用 UNKNOWN survey 永久锁住 Builder。选择另一个有合法路线的机会不等于宣称旧区域安全、空闲或已探清。
- 保留真实报价来源、施工资金承诺、唯一 actor owner、原生 build/repair 保护。红线锁定尚无已验证的合法 Observation 字段，本轮未读取敌方隐藏命令。

## 陆军：补齐重炮的真实类型

新增独立 `HEAVY_ARTILLERY_1_15.json`，以原版资产 SHA 与 gameJar 身份校验，保留原冻结 catalog。`heavyArtillery / heavy_artillery` 属 LAND，可攻击 SURFACE，不可攻击 AIR/SUBMERGED；并非普通 `c_artillery` 的别名。

现有 General 同 movement 分组因此能带上重炮；已有掉队续行、临时分兵、generation/ABA 继续有效。静态类型只允许进入当前 native eligibility 查询，不代表能立刻开炮、射程内或造成伤害。资料缺失/损坏仍为 UNKNOWN。

## 空海：批量己方 mode 与合法区域回查

- `/combat/unit-modes?unitIds=...` 一次最多 48 个己方身份。每个成员有独立结果；外队/缺席/不可观察身份仅 UNKNOWN，不泄露原生隐藏属性。原 `unitId` API 保留。
- Air registry 每轮分批读取，多个 formation 共享这批己方证据。requested、desired、physical、weapon 和 terrain 证据仍分离；receipt 不能替代 mode witness。
- Search 已占 frontier 是硬 reservation，历史搜索偏好是 soft avoid。原生全局 fallback 也必须排除其他队当前 reservation；不会因为降低 soft avoid 而反复选回已占区域。
- 失联区域在已知防空圈内时，可以向原生规划器请求安全观察站位与完整路线。后帧己方位置更新 route cursor；不要求先飞进危险中心才查询合法视野。
- 只有当前 `/scout/visible` 覆盖待查局部区域，并且随后的合法 `/combat/observe` 没有该目标/区域敌人，才释放局部清剿责任。`area clear != killed`，也不是全图清空。
- 安全观察站位无法建立充分视野时，保留责任并报告 `air_general_area_blocked / handoffNeeded`。本轮没有完整实现跨 owner 的阻塞任务移交；不把该日志当成功解决。

## 地面与空海投资

首次长期空海角色的准入不再要求“整个 activeTarget 都变成至少 75% HP 的现役兵”。已有达到成熟成军最低规模的可用地面力量即可允许互补空间角色；本土紧急状态、真实原生报价、资金/slot 保护保留。

这不等于停止陆军或宣布大陆胜利。新增地面投资是否下降，仍沿现有 `MilitaryInvestmentPolicy`，使用当前合法陆地需求、现有可用地面力量与 activeTarget。取消的是“必须先填满计划编制，空海职责才存在”的循环依赖。

## 人工观察

BattleClient 附加写 `rw-agent-reports/battle-latest.json`，原子替换最近 runtime / provenance / summary；完整 gzip journal 原样保留。显示优先读取最新文件，避免解压历史积压造成分钟级延迟。

顶部仍直接显示 Bridge Game Time / Frame，另标 Agent sample 的原生时间和帧；Agent 循环变慢时两者的差距可见，不用墙钟补算。显示文件写失败不终止 Agent。该通道仅用于显示，不参与任何策略输入。

未提升全局命令额度/decision frequency；没有核弹、敌方隐藏位置或隐藏红线锁定读取。focused/native fixture、自然 Spain 和真人验收须分别报告。
