# Octopus G5.1 handoff — 2026-10-03

## 接手身份

分支 `codex/octopus-g3-execution-20261002`；验收起点 `ca2fc92`。实现 `9cd71c1`，近期进展时间语义硬化 `8498c10`；最终文档/evidence提交在其后。测试产品源码为 `8498c101ed56b24c756af951936c1e29c99607c3`，最终 HEAD见Git。

候选 `RW-CANDIDATE-2026-10-03-OCTOPUS-G5.1-v1`；JAR为 `G:\deepseek 工作台\_validation\octopus-g51-20261003\final-source\agent\dist\rw-agent-bootstrap.jar`，SHA256 **`273a861f479f2c89438e9689a352fd399b4d35640281bd88a4aa0f6995c23418`**。

先读本handoff、[G5.1契约](../docs/OCTOPUS_G51_OPERATIONS.md)、[validation-summary](../evidence/octopus-g51-2026-10-03/validation-summary.json)、[人工入口](../docs/OCTOPUS_G51_HUMAN_VALIDATION.md)、[manifest](../deliveries/octopus-g51-2026-10-03/candidate-manifest.json)。不重新G0～G4全仓审计，不用冻结9/30 verifier的源码drift覆盖候选。

未deploy/push，未启动/关闭/操作桌面游戏；Universal Bridge源码/安装未改。只使用Sol6.1/high子代理局部实现/review，root整合、唯一最终完整回归与证据汇总。保留原有两份untracked真人feedback/prompt并作为输入归档，没有reset/clean。

## 真人raw根因与实际修复

主证据 `G:\铁锈战争 桥 工作空间\headless-runs\human-g5-spain-20261003-01\agent\rw-agent-reports\battle-1791014693923-d7f87169.jsonl`；707127357 bytes、111746行、0 malformed，SHA `6f54bcd21224e78ee5383e28537a429517760e92036f8a37d166df4b7afa5a9b`。本轮无需重新解析真人replay；此前验收的resync0为既有证据。root/子代理均stream本地raw，不将全部长日志塞入repo。

1. **第一波目标准入缺失。** 原General偏最近native-compatible目标，没有把目标旁静态火力、AIR缺口和撤退/后援成本纳入准入。新增TacticalOpportunity；确定性合法机会排序、当前暴露准入、改目标/standoff/retreat。拒绝基地目标不能回落到基地后方frontier。保留native engagement/known approach与48chunk，不随机路线、不合兵。
2. **反空不足没有接到普通生产。** AirResponsePolicy从当前可见armed AIR与可信fresh domain形成有界采购偏好；当前native菜单挑合法LAND/HOVER反空候选，accepted pending计入覆盖，源player/frame/time严格校验，UNKNOWN不补猜。真实菜单选择scout700暴露了旧unitScore白名单slot缺口，现counter显式militarySlots1。采购不等于counter已到前线/已击落飞机；T1缺合法报价不发明路线。
3. **builder到点失去局部承诺。** 5924北部己方到位检测696832ms，南向改派702544ms，南部矿命令788720ms。反馈粗估与这些snapshot/detected时间不是同一发生时钟，不把其差直接叫准确浪费。ProspectCommitment从accepted move→严格后帧arrival建立600局部簇；native当地施工/近矿优先，资金不足等、UNKNOWN保留、两次≥8s完整空簇才释放，危险/接管可释放。稳定anchor防逐步跨图漂移；不认定hidden矿安全/空闲。
4. **吞吐需求口径与恢复闸互相锁住。** 78 capacity samples为27NO_USEFUL_DEMAND/51UNKNOWN，usefulDemand与safe窗口均0；成军者离开legacy free-army评估口径。Strategy read-only纳入General-owned current ordinary，不借owner发令；远方战斗不再全局veto经济，当前home500 armed威胁/未知/恢复仍保护。≥6健康主力和真实replacement/reserve预算可考虑tech/矿投资，不等32/43目标；tech以既有收入估计模型和accepted consumption做≤30s有界等待。满编时native replacement报价仍可知，军队slot与升级报价分离。native ready施工解除额外15s等待。150s/80%利用率/单factoryCommitmentId保留，未cash-only扩厂。
5. **重复move与旧Bridge证据不足。** raw1470move中739JOINING、556GeneralRetreat，536相邻相同owner/actors/path重复。增加current native order/generation/revision/后帧进展复用；已到rally的个体无需等待General75%quorum才停刷。旧Bridge缺orderX/Y时只做短defer：后帧、朝accepted几何目标净减少≥24、近期≤2500ms、General/retreat/JOIN硬窗口6/4/12s，目标/威胁/owner变化即时失效。长gap重建基线，不把检测时刻伪装近期运动；不是native目标或execution witness。

raw peak17648不单独证明可买产能：最终1549392ms credits17191.5但mobileArmed0/landFactoryQueue0。重复路径也不证明每条都无用。没有发明K/D、击杀归因或隐藏AIR位置。

## 可运行证据

- Windows原始完整首遍 **79/80 / failed steps1**；唯一失败StrategyNativeHarness在引擎初始化前缺assets/translations/Strings.properties。将**同Jar/同compiled tests**置于已有完整隔离native-work目录，仅补该harness，**51checks PASS**；无产品改动、无第二完整回归。有效 **80/80 / unresolved failed steps0**，46Java/31Python/497tests/skip0；首遍日志、异常和环境补验分别保留，不称原始首遍零失败。219份agent/tools/knowledge输入在repo与隔离snapshot逐字节核对。G1/G2 optional历史参考直接使用此前已经接受的83c09fb冻结legacyJar e911b170…，G3使用既有G2 fixed-candidate；不把旧9/30/preG1参考当当前行为基准。下次隔离full准备需将原版静态assets/res合法复制到隔离工作目录或为StrategyNativeHarness指定完整隔离cwd；不能在缺asset目录重复boot，不能修改用户安装。
- 最终focused：G51Operations37 / Tactical45 / Logistics53 / ProspectIntegration92；旧G5Combat56 / Commander265 / ForceController69 / ForceFormation219 / ProductionCapacity36 / Strategy174仍通过。
- 最终同JAR native **495 checks**：G51 266（含AIR13）；G3多厂168、两栖Mode30、lane竞争31。真实HTTP、原版菜单、原生命令k付款、后帧队列/order witness；`simulationTicks=0`，位置/单位/时钟由fixture控制，不是自然战斗获益证明。
- 最终同JAR Small Island5×自然600400game-ms，PARTIAL/ONGOING：29新战斗单位、一次ready厂升级、三次矿施工完成（同时矿最多2）、一General、自然ADVANCE/STANDOFF；953accepted commands，其中544GeneralRetreat。首ready厂source20.080s、6ready ordinary77.200s、首T2厂317.616s。没有自然扩厂/新矿升级。本局不是受控AB，也不能和真人Spain不同地图/打法比较胜率或APM。
- 外部完整证据 `G:\deepseek 工作台\_validation\octopus-g51-20261003`。小摘要提交到evidence，不提交商用游戏assets或大raw。

保留失败/过程：原生首试PowerShell JVM参数解释错误（未boot）；第二试错误期待heavyTank，实际native可选scout700并发现slot遗漏；observer初验与真人port lease冲突后只隔离mock测试锁目录；9cd源码full刚开始时子代理最终focused发现long-gap语义错误，root仅停止自己隔离回归进程树、保留aborted源码/日志，然后8498完整重跑。最终 **一轮完整回归**，另有 **一轮中断的中间回归**，不得声称全过程只有一次启动。

早期smoke使用不同候选且随机运行条件未受控；`after-g51/result.json`的hash曾采样正在重build的原路径，实际immutable运行JAR由raw provenance19120d2e…确认。该错误metadata保留并标注，最终8498 smoke的raw/copy/JAR SHA一致；只以最终同Jar evidence交付。

## 已证明与待证

此前真人G5自然4General、多路推进/撤退、长期生产/扩矿、>48分块与无desync/resync仍PROVEN。相关机制未重写，focused/native/full保护不变量；不要把旧G5能力重新降为NEEDS_EVIDENCE，也不声称新G5.1已经再次真人证明这些能力。

G5.1已有fixture/native证据：合法防御准入、owner/ABA、缺坐标短defer/long-gap门禁、个体rally hold、局部矿承诺真实Strategy链、read-only owned assessment、真实counter采购/付款/队列来源。自然只证明最终Jar运行、生产/升级、General战术转换。

仍NEEDS_EVIDENCE：真人Spain第一波存活与抗重复针对；counter自然到位及AIR交换收益；builder簇自然毛收入/释放收益；持续产能扩张及矿升级时机；旧Bridge实际APM/有效推进；rally/standoff动态安全与混合域可达性。HP exposure仍proxy，catalog反空兼容不是native攻击许可。direct attack/group move缺Bridge接口，本轮未做。SearchArea继续contract-only。

`rwagent.g51=false`只关本轮运营/assessment接线，不是完整G5战术回退开关；真实旧产品对照用旧Jar。runtime仍force collect/arbitrate，经济/production/Strategy/Recon成熟链immediate；未全局batch，也未提高cadence。

## 下一断点

操作者自行启动合法Spain对局后运行 `tools/RW-Agent-Octopus-Test.bat`；默认已指向G5.1 manifest，可由PowerShell `-Seconds 4800`超长运行。保留实际Bridge/client双身份与port lease，不抢已运行controller。root未执行桌面入口。

优先观察 [人工入口中的5个现象](../docs/OCTOPUS_G51_HUMAN_VALIDATION.md)：第一波standoff/改目标是否有效或长期站桩、AIR需求→counter到位、builder簇到点后当地矿转化、6坦/3矿/厂矿升级/容量窗口、move/APM与有效推进。取新raw后再修真实共同瓶颈；不要继续加频率、随机路线、固定build order，或先扩理论与测试矩阵。
