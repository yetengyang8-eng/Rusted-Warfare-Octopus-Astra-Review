# Astra 云端任务书｜Octopus R12 与 R16 陆军对照、根因定位与隔离修复

> 版本：2026-10-13 · 面向**没有用户本机访问权**的云端 Astra。请先读完本文件，再执行。  
> 审查仓库：`https://github.com/yetengyang8-eng/Rusted-Warfare-Octopus-Astra-Review`  
> **不得假定能够访问 Windows G: 盘、Steam 安装、Desktop Commander 或用户当前桌面。**

## 1. 目标与用户最新的直接反馈

本轮的首要目标：**在证据支持下解释并修复 R16 陆军相较 R12 的明显退步，同时保护 Air、Builder、Scout、经济、Repair 与 G0～G6 安全约束。**

用户直接观察到：

- **R16 陆军特别分散**；与 R12 相比，难以维持合理的战场组织和局部攻势。
- **R16 几乎不怕敌方基地**；即使局部机制判断基地危险，也出现**一半部队撤离或观望，另一半继续打**的矛盾行为。
- **用户更认可 R12 的陆军表现**，希望你实际对照 R12 与 R16，而不是预先接受 R16 工程文档宣称的修复效果。

注意：这是玩家实战观察，**不是已经确认的代码根因**。R12 的优势也不等于整个 R12 更强，不能整套回滚后直接交付。

## 2. 云端可获取的材料——不要再找本机路径

直接在云端工作环境克隆：

```bash
git clone https://github.com/yetengyang8-eng/Rusted-Warfare-Octopus-Astra-Review.git
cd Rusted-Warfare-Octopus-Astra-Review
git lfs pull
```

如果云端已挂载这个仓库，直接在其根目录工作，无需再克隆。Git LFS 用来取回 `.zip`、`.gz`、`.partial`、`.replay` 等大证据文件；普通网页显示 LFS 指针时，**不能把指针误认为原始日志**。

优先阅读以下仓库相对路径：

1. `review/ASTRA_START_HERE.md`、`review/REGRESSION_QUESTIONS.md`；
2. `versions/R12/source/handoff/HANDOFF_Octopus_R12_Builder_2026-10-11.md`；
3. `versions/R16/source/handoff/HANDOFF_Octopus_R16_2026-10-12.md`；
4. `versions/R16/source/docs/OCTOPUS_R16_GROUND_APPROACH.md`；
5. `review/diffs/R15_to_R16_code.patch`；
6. `versions/R12/source/agent/` 与 `versions/R16/source/agent/` 的实际实现；
7. `evidence/validation-archives/R12_native_validation_text_and_trace.zip`、`R16_native_validation_text_and_trace.zip`；
8. `evidence/native-raw/R12/`、`evidence/native-raw/R16/`；
9. `cloud_review/reference/spain_natural.py`（**历史真实运行脚本，仅作核对，不直接在云端执行其 Windows 绝对路径**）；
10. `cloud_review/reference/spain-r16-normal-p5-summary.json` 及 `review/FULL_EVIDENCE_INDEX.md`。

已冻结的身份：

| 基线 | 源提交 | 冻结 Agent JAR（仓库相对路径） | SHA256 | 旧本机端口，仅作身份说明 |
|---|---|---|---|---|
| R12 | `eef352fafdf1392a588a1b7be41d8003c2512531` | `versions/R12/kit-owned-files/game/rw-agent-bootstrap.jar` | `61d8a341b3fff0445e67978d7deb2a6d4e349589e683a1f35afc35c9317b98cf` | 47699 |
| R16 | `0ada07a88426b4fe03781027c58e16a0950f5f96` | `versions/R16/kit-owned-files/game/rw-agent-bootstrap.jar` | `3a1eeb33725c93279818e90a9812e21bfb0bbf04f079407143835054d8ee8ad5` | 47707 |

**不要使用** `VERSION.json` 中的 `local_full_kit` 作为实际执行路径；那些是历史 Windows 信息。也不要运行 `kit-owned-files/1-Start-Game.bat` 或 `2-Run-Agent-7200.bat`：它们是 Windows GUI 入口，不适合云端。

## 3. 如何在云端运行 Spain：先验证依赖，再启动原生对照

### 3.1 已上传与尚缺的运行条件

仓库已包含：R12/R16 **源码与成品 Agent JAR**、西班牙 MP97 地图、旧原生测试输出、分析文件和可移植的自然对局入口：

`cloud_review/run_spain.py`

原始地图位于 `evidence/human-1v9-original/` 里的 `.tmx` 文件；地图 SHA256：

`342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746`

**关键限制**：公开仓库未重新分发 Rusted Warfare 商业引擎和资源，因此**仅凭 Git clone 无法运行新的原生 Spain 对局**。云端要运行必须另外合法取得一个兼容的、已解包的游戏资源目录，至少包含：

```text
<ENGINE_DIR>/
  game-lib.jar
  assets/                 # 包含 tilesets/
  libs/
  res/
```

它的 `game-lib.jar` SHA256 必须是：

`8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`

此外需要 Java 和 Python 3；Java 的 LWJGL/原生依赖须与运行平台匹配。**不要从网上随意下载一个同名 JAR 充当受控环境，不要绕过 SHA 校验。** 如果云端没有这些合法游戏依赖：跳到第4节，基于已上传的源代码与原生历史证据开展审查；在报告中明确标注 **NEW_NATIVE_RUN_BLOCKED_MISSING_ENGINE**，而不是声称已经复现或测试通过。向用户索要合规的可用环境交付方式即可，不要引用 Windows G 盘或要求用户操作本机 MCP。

### 3.1.1 原生运行器的真实版本差异（必须遵守）

已核对两份**冻结源码**：`versions/R12/source/agent/src/io/rwagent/headless/HeadlessRunner.java` 只接受地图、端口、速度、时限、帧数和难度；它**不接受 `--player`**，启动原生 quick-match 时固定选择玩家0。R16 才加入 `--player 0..7`，默认也是0。故未改原始产品、又需要真正同条件对照时，统一跑**普通 Spain、玩家0、5倍速**，而非此前 R15/R16 自然胜利样本的玩家5。请不要试图给 R12 的原生 runner 强行传 `--player 5`，它会直接启动失败。

这条 HeadlessRunner 链路是**原生 quick-match 的8槽位设置**，不是用户本人真人1v9的10槽位完整重演。R16既有普通Spain/player5样本是历史对照资料，但不能与新做的普通Spain/player0直接按原样比胜率。若要真正比较玩家5，需要在**共同隔离、可验证的同一版本测试驱动**中显式支持两个Agent，从而证明不会改变其产品战术；做不到就老实选玩家0。

### 3.2 有兼容引擎时的完整对照命令

在仓库根目录，将 `ENGINE_DIR` 替换为**云端自己**可访问的合法资源目录。先运行预检：

```bash
python cloud_review/run_spain.py --version R12 --resources-dir "$ENGINE_DIR" --check-only
python cloud_review/run_spain.py --version R16 --resources-dir "$ENGINE_DIR" --check-only
```

`--check-only` 只验证所需文件、地图、引擎及冻结 JAR 的哈希，不启动对局；必须两个都显示 `INPUTS_OK` 后再执行。

然后按顺序分别运行：

```bash
python cloud_review/run_spain.py --version R12 --resources-dir "$ENGINE_DIR" --label r12-normal-p0-01 --game-seconds 3600
python cloud_review/run_spain.py --version R16 --resources-dir "$ENGINE_DIR" --label r16-normal-p0-01 --game-seconds 3600
```

脚本固定**地图 Spain MP97、普通难度 `difficulty=0`、`player=0`、5倍速、原生战争迷雾、完整引擎、零资金/单位/位置注入**。每个独立运行目录在 `cloud_review/runs/<label>/`，含 `input.json`、`monitor.jsonl`、`initial-state.json`、`final-state.json`、引擎与 Agent 日志及原生战斗 raw。时间依据**游戏 gameTimeMs / frame**，非墙钟。

`run_spain.py` 是由已归档、曾实际成功执行 R15/R16 自然对照的 `spain_natural.py` 去除硬编码路径后移植的**新入口**；其 Python 语法与 CLI 可检查，但没有在缺失引擎的云端完成真实端到端验证。启动后必须首先核对 `initial-state.json` 的 player0、存活的原生 commandCenter、地图/引擎/Agent 哈希。若某环境跑不起来，保留失败记录，不为了让结果“顺利”改变测试条件。

两套冻结 JAR 的新对局不应互相混用，也不能同时控制同一个 Bridge。可以先顺序运行；若做多局，应保存每局全部 raw，明确双方敌情可能有随机差异。**3600秒预算结束且 ONGOING 不等于死亡或原生失败胜负**。

对生成的空间快照，可使用：

```bash
python cloud_review/summarize_ground.py \
  --r12 cloud_review/runs/r12-normal-p0-01/monitor.jsonl \
  --r16 cloud_review/runs/r16-normal-p0-01/monitor.jsonl
```

这只量化重坦空间离散程度，**不代表 General 的真实编组或基地威胁判断**；真正归因仍需原生 General transition、actor/order、基地威胁、视野、HP 与后续实战结果。若不同平台的 HeadlessRunner 无法加载原生依赖，报告环境失败，不能变更地图、无雾或降低AI难度“修复”测试。

## 4. 如果云端没有游戏引擎：可立即开展的真实工作

这不是停工理由。请执行：

1. 将 `evidence/validation-archives/R12_native_validation_text_and_trace.zip`、`R16_native_validation_text_and_trace.zip` 解压到**云端工作临时目录**，阅读其自然局 raw、工程回归与失败样本。原 zip 已上传，不要请求本机再次寻找。
2. 同时分析仓库普通 Git 的 `versions/R12/source/agent`、`versions/R16/source/agent`，建立“基地威胁 → General任务 → 成员运动/命令 → 后帧血量/位置”代码链。
3. 追溯 R15 中间阶段，区分 R16 的**地面改变**与其他既有问题；R16 Air class 未改并不证明 Air 运行表现没受影响。
4. 使用上传的旧自然局和 validation 事实提出可证伪的原因、对应场景及最小变更建议。把只能从日志推断的内容标成 `HYPOTHESIS` 或 `CODE_SUPPORTED`，与 `CONFIRMED_NATIVE` 区分。
5. 如具备源码编译环境，运行不需要第三方引擎的静态/单元检查；**不可以把它称为完整 Spain 自然对照。**

**如缺运行依赖，交付先做到“可靠定位 + 候选修复 + 可运行时的验证计划”；不要声称 R12/R16 新一轮真实对照已经执行。**

## 5. 首要诊断问题：R16 为什么越防基地越散？

请优先按以下链路比较 R12 与 R16：

**A. 集结 / 分兵 / 增援**：General roster 与真实位于前线的 local actors 是否混淆？远处任务是否一次拉走整个军群？当一部分前去获取视野，是否保留当前主攻任务并渐进增援？应区别**有目的的战略分兵**与**同一攻城任务战术协调失败**。

**B. 同场基地威胁判定冲突**：某些单位/分队避让基地，另一些却继续攻击，究竟是不同 General 的结论不一致、同 General 不同执行器冲突、路径线段评估遗漏、局部400半径估算缺乏任务一致性，还是较高优先级的普通MOVE/ATTACK绕过了保护？这些均为待核查假设，**不要先选择最顺眼的一个**。

**C. 实际战果与权限**：对每个典型接敌点，按 gameTime/frame 记录当时**可见**基地、实际前排成员、局部HP/DPS、攻击命令及其 `accepted`、下一段真实位置/损失、基地HP变化和最终是否完成任务。记忆中的基地只能合法警戒/搜索，不能幽灵攻击。不能把控制器日志的 `admitted` 当作全军实际进入安全阵型。

**D. 过度保守与送兵之间的平衡**：修复不能只是让全军更怕基地。有效攻城时应持续，真正无法承受火力时受影响的成员应协调规避，不能一半误送一半空等。

**E. 核对改动引入点**：对照 R12/R15/R16 的 `GeneralCombatDirector`、`LocalArmyDirector`、任务/owner、威胁估算、指令仲裁。不要凭“修改文件是陆军”就断定它不会改变 Air 的任务输入或经济链。

本轮不把历史真人1v9（不同控制者与不同难度）当作 R12/R16 控制实验。

## 6. 开发边界与回归保护

- **冻结 R12/R15/R16 原始源码与 JAR**，在隔离分支或新候选目录实现最小修复，不覆盖 `versions/`、不改写原生历史日志。
- 不修改 AirGeneral / AirResponsePolicy、Builder、Scout、Repair、生产与经济策略来“顺手优化”；确有跨系统依赖时先说明证据，再做最窄改动。
- 保留 fog/可见性、`owner`、generation/ABA、native command guard、receipt与实际后帧区分，不降低测试守卫。
- 不通过增加全局命令预算、写死编队数量/最低攻城人数、固定等待时间或扩大保守阈值掩盖问题。
- 在 focused 测试之外，保护空军索敌与持续区域清剿、Builder 本地真合法报价施工及远端区域职责、Scout 运动连续性、Repair 和经济生产吞吐。
- 不能假定 R12 全部系统优于 R16；只是把 R12 陆军作为用户指定的高价值参照。

## 7. 建议交付

向仓库提交一份 `review/results/R12_R16_GROUND_FINDINGS.md`（可配合 PR），说明：

1. R12 和 R16 源码差异以及证据支持的行为差异；
2. 各个异常链的 General/actor/unitId、GameTime/Frame、基地位置/真实视野、命令与后帧证据；
3. 哪些结论是 `CONFIRMED_NATIVE`、`CODE_SUPPORTED`、`HYPOTHESIS`、`NEEDS_USER_EVIDENCE`；
4. 能否运行新 Spain 自然局，若不能列明缺的外部引擎/资源及预检错误，**不要伪称已经跑完**；
5. 最小补丁及涉及的影响范围；
6. focused 以及非陆军系统回归测试结果，新的原生对照结果（只有实际运行成功才能填写）；
7. 未解决的问题和下一步验证。

**最终验收原则：陆军像 R12 一样能有组织地判断基地、分兵与攻城，同时不牺牲后续版本已经真正实现的空军、经济和安全能力。**
