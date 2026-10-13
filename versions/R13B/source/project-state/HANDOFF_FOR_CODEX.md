# 当前 G5 接手入口 — 2026-10-03

从83c09fb推进到G5，功能22c88a2、测试源码f058d3a，后续交付docs提交为最终HEAD。独立General combat/retreat/regroup、多General birth/Commander/ThreatTask、fast runtime和Human observer已接线。Windows full一次原始74/76，旧参考诊断漂移由83c复现；两suite13项当前参考补验后有效76/76，42Java/31Python/496tests；最终Jar native482；自然Small Island5×平均decision366ms且三次退却/恢复。未deploy/push/桌面启停。

先读[G5 handoff](../handoff/HANDOFF_Octopus_G5_2026-10-03.md)、[CURRENT_STATE](CURRENT_STATE.md)、[契约](../docs/OCTOPUS_G5_COMBAT_RUNTIME.md)。下一建议G5.1人类Spain/长图证自然额外General与rally。不得隐藏原始reference失败，也不回滚83c首厂修复。子代理Sol6.1/high，禁Astra；历史文件名不授权模型或桌面操作。以下历史导航保留，仅对各旧阶段有效。

---

# 当前 G4.1 接手入口 — 2026-10-03

当前分支codex/octopus-g3-execution-20261002；起点5562ec7，测试源码39a2670；Windows71/71、39Java、29Python/473tests、同Jar native657checks绿。零军队FORMING→ACTIVE、静态地图缓存、原生合法扩张与轻量screen已接线。自然局/ready施工NEEDS_EVIDENCE，未deploy/push/进入G5。

先读[CURRENT_STATE](CURRENT_STATE.md)、[G4.1 handoff](../handoff/HANDOFF_Octopus_G41_2026-10-03.md)、[Early契约](../docs/OCTOPUS_G41_EARLY_OPERATIONS.md)、[Static契约](../docs/OCTOPUS_G41_STATIC_MAP_KNOWLEDGE.md)。下一建议G4.2自然隔离核验。子智能体Sol6.1/high，禁Astra。下文完整保留历史导航，旧current/latest仅对旧阶段有效。

---

# 当前 G4 接手入口 — 2026-10-02

从当前最新G4交付继续：分支codex/octopus-g3-execution-20261002，测试源码54b9de1；Windows68/68、37Java/28Python466 tests、同JAR原生298checks绿灯。首遍日志兼容失败及修正完整重跑均保留；未部署/push/自然验收，未进入G5。

先读[CURRENT_STATE](CURRENT_STATE.md)、[G4 handoff](../handoff/HANDOFF_Octopus_G4_2026-10-02.md)、[force契约](../docs/OCTOPUS_G4_FORCE_LIFECYCLE.md)。force batch有真实priority，其他lane仍immediate；入口零兵无General自动birth。子智能体Sol6.1/high或用户指定同级，禁Astra；旧文件名不代表模型授权。

以下内容完整保留为历史导航，其中current/latest/next只对各历史阶段有效。

---

# 当前 G3/G3.5 接手入口 — 2026-10-02

从当前最新G3/G3.5交付继续：最终测试源码95a917e，分支codex/octopus-g3-execution-20261002。有效63/63验证（历史fixture环境补验另列），未部署/自然验收，未进入G4。

请先读[CURRENT_STATE](CURRENT_STATE.md)、[G3 handoff](../handoff/HANDOFF_Octopus_G3_G35_2026-10-02.md)及[契约](../docs/OCTOPUS_G3_EXECUTION_INTENT.md)。子智能体优先gpt-6.1-sol/high，禁Astra；旧文件名不代表模型授权。下面仅保留历史导航，current/latest/next表述以本段与当前project-state为准。

---

# 接手入口

最新增量及GPT/DeepSeek交接：[HANDOFF_Codex_Feedback_2026-10-01.md](../handoff/HANDOFF_Codex_Feedback_2026-10-01.md)。无builder起步、多队控制、局部危机、公平调度、后方生产与T2/T3局部投资已落在当前仓库；先读最新CURRENT_STATE，保留增量。冻结基线未回写或晋级。

从 **RW-BASELINE-2026-09-30-GS-v1** 开始后续任务。

1. 阅读 [BASELINE.md](BASELINE.md)：源码、交付件、环境角色、验证边界和保护规则。
2. 阅读 [baseline-manifest.json](baseline-manifest.json)，核对当前 Git 状态和运行件身份。
3. 阅读 [CURRENT_STATE.md](CURRENT_STATE.md) 与 [NEXT_STAGE_PLAN.md](NEXT_STAGE_PLAN.md)。
4. 在仓库根用 Python 3 运行 project-state/verify_baseline.py；本机可附加 --workspace "G:\deepseek 工作台"。发现漂移先报告，不覆盖用户改动。

默认开发目录为本仓库 agent/ 和 tools/。旧游戏环境的 developer、旧解压交付与旧临时分支仅供历史核对。

基线后的增量实现保存在本仓库最新提交中；继续任务时先读取 CURRENT_STATE 的当前候选，保留已完成工作。基线核对器报告源码与 v1 不同，可是已有候选的预期变化，不能据此回退最新源码。未晋级的候选验证不得混入 v1 的冻结结果。

新任务应说明输入基线、目标行为、验证方法和预期交付。源码/脚本变化在隔离目录做构建与完整回归；自然行为结论需要对应候选的原始证据。文档整理不需要启动游戏。不要自动运行旧交接中的安装命令。

危险反射、合法视野、冻结 game-lib、用户桌面与存档保护详见 BASELINE。新用户授权优先，旧轮次的策略范围不自动变成永久禁令。

本次整理前的完整交接已归档于 [repository-HANDOFF_FOR_CODEX.md](archive/2026-09-30-pre-baseline/repository-HANDOFF_FOR_CODEX.md)，仅用于事故和历史检索。
