# R7：前期运营修正与 G6 空海终局起步

分支 `codex/octopus-g3-execution-20261002`；验收起点 R6 `8714448`。本轮产品与测试提交：`2ddbf58`（运营与 G6 起步）、`f3cacb2`（隔离入口／回归接线）、`fbecf71`（近邻施工结算／旧模式兼容）、`cd629e7`（后方工程师用途）、`7fdeb72`（新增 HTTP 测试正确模式与付费队列）。最终文档 HEAD 见 Git。候选 JAR SHA256：`7588805d1bb74f80c2ac57bed0bcaeccd7eb2cb319fba0faa45c2c58e1e0fd2a`。

这是可运行的 G6 起步候选，尚未完成终局胜利验收。未 push/deploy/操作用户桌面游戏；原版、设置、存档、回放、冻结 kit 保留。产品已提交，工作区仍保留用户原有两份 launcher 修改及 backup，三文件 hash 未改变。

## 实际修正与共同根因

- **Builder**：全图未来 construction domains 曾直接推高即时采购。现在依据当前准备开发的可执行方向、真实设施任务、已有忙闲／待显现施工者和实际价格资金；全图静态矿仍是未来需求，没有固定前期人数上限。
- **Scout**：少量视野被当成任务完成；受伤回撤后又受 expendable 与旧 recheck 影响。现在持久 Frontier、后帧路段到达才续路，新危险改变路线，恢复后仍保留独立侦察职责。未增加命令频率。
- **产能**：旧 6 秒丢弃上限使大型局面的 150 秒窗口失明。按原 80% 覆盖余量允许至多 30 秒合法端点区间，保留截止前驱；换厂、session/player、rollback、长空白、旧菜单仍 UNKNOWN。G5=false 保留旧窗口／日志，实现真实 G1/G2 等价；阶段 wall timing 包含 IO，不冒充 CPU profile。
- **空海编队**：空闲、无任务／未结承诺的 SpecialistPool 飞机经真实 transfer/generation 与后帧 JOINING→ATTACHED 形成独立 `air-general:1`，共同 AIR 搜索、合法当前目标接敌、逐成员 Mode 与恢复，不进入地面 General。新危险无替代路径时也取消实际危险旧 MOVE。超过48引用仍分块。
- **后方制造与资本**：真实原生菜单制造两栖飞机；`ENDGAME_AIR_PROVIDER` 采购不会继承无关 Need，ready 后用途保持，暂缺资金不被普通 Capability 重新领走。air target 仅诊断，不是停产开关。长期现金净增长、已有空海投入、无真实设施缺口时，闲置本土 Builder 走资源制造仪及原生升级链，保护下一架飞机与普通生产真实账单，本地矿优先。
- **资金互锁**：两 CE 的120world关联圈覆盖同组施工，曾让两笔效果永久歧义。改为真实 baseline／后帧效果候选图：N笔承诺、N个不同效果、完整匹配才能整体结算；不猜 individual producer，不以 timer、钱包、空队列或缺席释放。仍有无法消歧的账，见下。

契约：[运营与 G6 起步](../docs/OCTOPUS_G6_EARLY_ENDGAME.md)；[系统说明书](../docs/AGENT_USER_MANUAL.md)。Ground force 仍真实 collect→priority→dispatch；经济、Recon、Strategy 与空海仍按现有阶段 immediate，未宣称全局 all-lane batch。

## 同一最终候选的验证

- 完整 Windows **98/98，64 Java harness，31 Python suites／503 tests，3 build，failed steps: 0**。最终输入255文件无漂移；独立 full 构建与交付 JAR 214 ZIP entry 内容相同。
- root focused16套通过，另资本30、credit178；完整矩阵也包含新增 focused。实际 HTTP capacity17通过；G1四路径／G2九项与历史冻结基线比对真实 wire／旧事件，没有修改过滤。
- 原生九个既有阶段通过，新增 G6 native42、Spain第三地形9，共11阶段；AIR fog／地形86由完整矩阵覆盖。显式 seed/clock/fog/position/height、0simulation ticks；证明原生命令处理、付款／queue／施工初态与合法 mode/owner witness，不能证明自然穿越、射击伤害或胜利。
- 新 kit1895文件 hash 一致，G6入口开启，只准备未启动；用户三文件 hash 未变。

最终原始根目录：`G:\deepseek 工作台\_validation\octopus-r7-20261005`。完整回归 `final-verification-v5`；自然 `spain-provider-4800`；focused `focused-final-provider.log`、`capital-final-provider.log`、`credit-final-provider.log`、`native-provider-endgame.log`。仓库[验证摘要](../evidence/octopus-g6-r7-2026-10-05/validation-summary.json)和[自然摘要](../evidence/octopus-g6-r7-2026-10-05/natural-handoff.json)保存原始路径与 SHA。

v1/v2必要修正后中断，v3为95/98（class启动和旧模式兼容），v4为97/98（新增HTTP误用legacy／无付费周转输入），均保留。最终原生初次固定port冲突、JVM参数／缺evidence参数启动失败保留，未进入产品断言；修正调用后顺序通过。[失败保留索引](../evidence/octopus-g6-r7-2026-10-05/intermediate-failures.json)。没有把旧失败删掉或算最终成功。

## 最终 Spain 自然长局

标准零普通军队，difficulty0（正常／中等）、team0、请求5×，4800游戏秒预算；未改现金／HP／位置／fog。native结果仍 **ONGOING**，Agent为 **PARTIAL**，预算结束不是胜利；不是同条件真人 AB。raw 423061 rows，malformed 0，SHA `0e17356a4a38747355183ab2f2f667bb004f1478da9ce374488d804aa9d26837`。

前5分钟 Builder4笔／2000，对重坦8笔／6400；前10分钟 Builder8笔／4000，对重坦35笔／28000。最近前5/10分钟合法 ready 快照均1Builder，分别6/30重坦；采购不等于存活、缺席不等于死亡。即时需求未再因全图未来矿区暴涨，但施工者损失／安全仍待真人。

Scout603 的己方／Recon位置证明距初始home最大1562world，24次路段命令与9次续路，含2次当前危险改道。约170.592s严重受伤回撤；长时间存活、实际修复后复出仍 **NEEDS_EVIDENCE**，不能把其它借用Recon单位行为归到这只Scout。

最后带完整己方合法快照为 **4803.536s**：ready重坦723／陆厂12／矿40／Builder13／两栖58；资源制造仪 T1=6、T2=6、T3=69，现金692607.0。after-client-exit是另一帧，数值另存不混合。

终局两栖投资79次，其中78次有原生ready关联；资本施工112次，其中80次有ready关联。实际 provider1771 于2810.032s施工飞机50155，2820.672sready；之前2815.120s自身合法出现后JOINING，2878.144s后帧位置ATTACHED并参与当前合法接敌。制造初态、worker匹配与成员状态的时间顺序不同，不称唯一 native producer lineage。首个资本投资3254.816s→3275.120s实际T1ready；升级后帧见raw。

上述终局ready飞机中，75架累计有后帧ATTACHED、77架参与搜索、42架参与攻击，搜索路段最多覆盖58架；不是终止时库存。本局使用既有工程师1771，专属ENDGAME_AIR_PROVIDER新采购的用途路径仅focused证明，尚无本局自然采购证据。

空海共266条持久路段、192到达witness、208攻击回执；它们不证明伤害／击杀／清图。较慢采样72次，最大14928ms，窗口有效覆盖150000～150000ms，sustained 72；利用率不足时仍UNKNOWN，没有伪造瓶颈。

末期未结施工／生产账 **62笔、141500**，最老1360.784s；有自然整体结算组件，但不能宣称全清。现金仍在大幅增长：制造仪同时增加收入，开始吸收资本不等于最终消费能力或终局完成。

## 人工入口与下一断点

新冻结目录 `G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-05-R7E`，port47687，显式 `rwagent.g6=true`。`1-Start-Game.bat`→用户自己进入Spain→`2-Run-Agent-7200.bat`（另1200/2400）；[入口与五项观察](../docs/OCTOPUS_G6_R7_HUMAN_VALIDATION.md)。本轮旧R7／R7-Final／R7C／R7D kit保留，均被R7E替代。

下一优先：空海在大陆优势后的实际全图覆盖、目标续接、潜水目标首次发现与原版胜利；再看危险下Scout存活／修复后复出、长期施工账消歧。当前一支AIR长期编队，不实现动态空军General数量、完整 WATER/ISLAND SearchTask／G6 A7/A8；空海RECOVERING使用现有修复湾，尚不单独增加地面RecoveryPool服务需求。Spain混合地形已有有界Dive／目标选择证据，自然进入与实际火力仍NEEDS_EVIDENCE。保住地面雪球，不再次推倒主体或靠提高频率解决。
