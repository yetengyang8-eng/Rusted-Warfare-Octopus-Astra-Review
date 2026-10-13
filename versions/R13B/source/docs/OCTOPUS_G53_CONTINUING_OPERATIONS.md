# G5.3 R4：持续运营契约

本轮以 R3 Spain 真人 raw 为问题证据。生产、兵力组织和消费吞吐分开；不改变 decision frequency，不开启 SearchArea 或完整 G6。验证结果及候选身份见本轮 handoff。

## 事实与根因

R3 压缩 raw 有 1,234,365 条有效记录，约 120 游戏分钟。晚期可观察快照里，350801 credits、58 armed/96 organization target、4 个原生可负担空闲生产设施同时存在；150 秒窗口利用率约15.66%。因此“达到 armyTarget”不能解释整个停产。当前遍历中兵力命令先消耗 token，加上反复 move，挤占了后方工作。131669 条 GENERAL budget denial、23643 次 move 尝试、364 次 queue 尝试支持该诊断；这些是记录数，不是持续时间或战斗次数。

R3 的 Builder 目标长期至多2，工厂至多4、矿至多5。固定 Builder 上限、施工只看当前合法矿点、全局生产 quota 与调用次序共同限制运营。基地防区承担完整战力准入及短命 frontier 任务，又造成较强部队推进停顿。这些结论没有把 compact 中省略的字段恢复成事实。

## General 保留向外任务

每支 General 的 OutwardMission 保留已接受的合法 native frontier、方向、anchor type 和真实 receipt frame。战斗、退却及 Commander 分配暂时覆盖控制，结束后继续任务。只有真实后帧当前位置达到 waypoint（同 anchor type 的健康成员75%在140范围内）才消费该 waypoint；丢失 anchor type 则重新选点。没有每15秒丢弃目标。

下一个 frontier 仍来自已有原生 scout plan；最多尝试3个，避免近处/倒退候选，第三个合法绕行可以改变方向。方向不是硬编码路线，也不推断隐藏敌人。已访问/被 veto 的点使用现有有限 avoid 集合。任务变化记录 `outwardMission`。

基地圈现在仅保护小编制：当前 General 自己 ATTACHED、NORMAL、ready、HP至少50%的 LAND/HOVER 普通作战成员不超过11时 veto 普通旅行；超过11放行。AIR、WATER、specialist、伤兵不冒充可参战地面规模。FightEstimate 仍决定当前具体战斗；基地记忆不会授权攻击 ghost，也不会证明基地存活或击杀。

明确的当前原生 move/attackMove 仍指向已接受目标、owner/generation正确且晚于receipt时，复用命令。撤退到当前 rally 的 move 不受全局敌方HP/位置变化和15秒进展期限打断。JOINING 的明确 move 落在当前 rendezvous 90范围内时同样复用。真实 rally变化、错误native order、被覆盖的目的地、ABA仍要求替换。缺少 destination 的 UNKNOWN 输入沿用原有短 defer，不能无限复用。

## FREE、骚扰与组织

当前 ThreatTask 先准备，再由既有 ForceController LocalResponse 领取低成本 FREE，刷新 roster 后 Commander 分配缺口。小任务沿用局部上界：至多3个当前目标、至多6名响应者、1500范围，实际原生攻击兼容性决定谁能覆盖；不足由 General 接手，不把 partial coverage 冒充完成。

新 cheap LocalResponse 在分配前取得真实 owner/generation，Commander 不会同轮又拿走该单位。旧 PENDING_JOIN + LOCAL_RESPONSE 可共存。相关敌人离开当前合法 visibility 即 clear，无 last-known 搜索或额外 clear timer；pending保留后JOINING，无pending回FREE。结束时取消旧攻击的 move 优先级70。借出的 General 成员不自动返回原队。

满编健康 ACTIVE 在当前缺口/压力和供应FREE存在时可以逐步扩大 desiredStrength，沿现有组织目标软边界；额外新兵仍能按既有 birth 形成新 General。扩编不改变已有owner/member身份。该规模是组织规划，不是生产停止条件。

## 持续生产与实际服务顺序

G5 普通军事生产不再由 armyTarget 总停机；有原生合法产品、idle producer、有效资金且保护已选择承诺后继续。默认不设 Agent 的512军队/5工厂软件上限；显式用户 override 保留，原生最终上限 UNKNOWN。旧 G5-off 行为保留。

有效报价、paid-before-visible、unsettled credit、producer slot、military slot、burst、native guard仍权威。Builder/investment/Capability 已选择储备合计保护；尚未选择的昂贵主力、升厂或次级目标不再让可负担普通产品等待。便宜合法产品可以继续生产，accepted receipt仍不证明花钱/出兵。

Force intents 仍真实 collect→priority→dispatch。critical/LocalResponse（priority至少70）先服务，剩余 token 在施工投资、普通生产、普通兵力三项之间轮转。施工重轮尚未到来时保留应得服务位置，避免与 heavy gate 周期重合导致饿死。没有增加 token 或调用频率。`g5_operations_service`记录实际 lane、tokensBefore/After、nextLane；这不是所有链 collect 的全局 scheduler。Recon、经济、Strategy、production仍在轮到自己时 immediate dispatch。

## 建设施工与收入转换

固定地图每局缓存，静态资源域可作为探索施工需求；每次容量评估最多新增4个原生 static approach 校验，使用已有600资源簇。新域须验证 session、knowledgeId、actor、tile、frame/gameTime 与 waypoint；它不证明安全、无人或当前可建。动态 `/expansion/plan`与最终 native build guard保留。

独立未承诺资源域、工厂缺口及修复任务构成工作需求；扣除已有闲工、已派施工、pending worker及paid ghost。Builder 增长使用总收入×30游戏秒/真实Builder报价作为规划负担能力，仍受实际工作和当前有效资金约束。普通出兵已花的钱不是对未来所有收入的永久保留。移除默认 Builder2上限，不设固定人数。

G5 baseline矿不再重复等待 NEW_MINE investment approval；真实选定建设进入原BuildJob资金承诺，探索旅行不以6兵或资金投资审批为前提。富余资金能同时承担当地空矿与升矿，空矿需求不再一律挡住矿升级。既有T3强化/超频及报价来源保护不变。

扩厂参考原150秒真实 production spend/观察到的工厂忙碌时间估计有效消费速率，结合总收入、持续剩余资金及真实建设报价决定目标逐次增长。不用低利用率虚构“产能不足”，不设默认工厂5上限。施工承诺仍逐笔；该估计不是原版生产时长或必然最佳厂数。

## 保留与 G6 边界

Owner generation/ABA、唯一command owner、FREE/PENDING/JOINING/ATTACHED、后帧到达/治疗/资金witness保持。敌失联及己缺席不是死亡，CLEARED不是击杀，没有K/D归因。Universal Bridge未修改。

两栖仍 SPECIALIST_POOL。未来独立空军/两栖 General 与已有独立owner及长期任务兼容，但需要 typed roster、移动域任务和 Capability借用/归还协议，不能把零散飞机直接塞入普通 General。requested/desired/physical/weapon/terrain 证据仍分离。完整 Capability Search/A7/A8、独立空海任务及终局SearchArea留给G6。

真人仍需核验：多路方向长期稳定、强军突破基地圈后的真实交换、小骚扰及时响应、修复FREE的再分配、Builder多方向施工、长局扩厂/升矿和高现金转化。这些不能由人工seed/短自然局宣称已经改善。
