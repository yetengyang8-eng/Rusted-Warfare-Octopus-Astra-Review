# HANDOFF（对话45 / Codex 并行运行器 v0 的独立验收）

> 正式正文：`G:\deepseek 工作台\助手交接\对话45.txt`（四项任务逐条回答）。
> 原始凭证：`G:\deepseek 工作台\助手交接\evidence\parallel_runner_v0_ds_acceptance.txt`。
> **本轮未改任何产品代码 / 策略语义 / runner / 安装件**；只做复核、一次自跑长负载、文档清理与建议。

## 1. 判定：ACCEPT（E2 + E3）

| 我独立做的事 | 结果 |
| --- | --- |
| 自写审计脚本 `_analysis\parallel_pair_audit.py`（重新推导不变量，不复用上游判断） | — |
| **我新跑的双实例长负载**：`--mode match --episodes 2 --parallel-pair --speed 4 --battle-seconds 900`（当前安装件） | 两局 PASS/都 VICTORY、退出码 0；审计 **ACCEPT** |
| 用同一脚本审 Codex 归档的 4 个运行目录（N/K/R/I） | **4/4 ACCEPT**（故障注入批复现 A=FAIL exit 15 / B=PASS exit 0） |
| 宿主完整回归（Codex 沙箱里 16 项 loopback 失败的对照） | **Java 17 + Python 10 套，failed steps = 0** |
| 身份复算 | 安装件与 dist contentDigest = `6fec7a0c…`；环境根冻结件 = `0681b4f7…`（未动） |

比我这次更强的一点：Codex 的 smoke/suite 批只证明了 `lockPath` **路径不同**（smoke 没真建锁），
我这次是 match 模式，**两把 `economy.lock` 都真实存在、跑完后都能立刻取得 OS 锁** —— 锁隔离现在是实测。

## 2. 残留风险（3 条真实 + 1 条已排除）

1. **端口预留有 TOCTOU 窗口**（低）：`bind(('127.0.0.1',0))` 探测后关闭、引擎再绑定；批内靠集合去重，
   外部进程理论上可抢。事后 `verify_owner` 会核对 health 报告的端口，所以不是静默错误。
2. **强杀 Python 编排器会漏掉两个引擎**（**已实测**，非推测）：探针结果 —— +1s/+10s/+20s 两个引擎
   都仍存活、仍 LISTEN、CPU 时间持续增长；`Ctrl+C` 路径本身正常。建议最小修法：
   `live-claims.json` + `--reap <run 目录>`。
3. **条件可复现性未闭环**：引擎有 seed（`runtime.json` 自报 `seedReproducibilityVerified: false`），
   但 CLI 没有 `--seed`。做参数 A/B 前必须解决或明确接受「同条件不同随机」。
4. 已排除：Codex 提到的 `engine-process.json` 中文路径替换字符 —— 我这次 **U+FFFD = 0**，
   是它沙箱的写读编码问题，不是产物缺陷。

## 3. 状态文档清理（任务 ③）

`CURRENT_STATE.md` 两处「待做（E4）」改为已完成（Builder Utilization v0 / Economy v0）；
路线段两处「NO_PROGRESS v0 已实现（E2），待 E4」改为 DONE_AND_LIVE_VALIDATED（E4：276.7 → 21.7 游戏秒）；
v1b 收尾的重复段落合并为一条（Q1/Q2/Q3）；新增「DeepSeek 独立验收」与「强杀残留风险实测」两段。
两个 `CURRENT_STATE.md` 仍同内容；历史证据文件一字未改。

## 4. 下一步建议（任务 ④，暂不施工）

**建议：`固定条件 A/B 对比能力 v0`**，而不是直接进 self-play 设计阶段。进 self-play 还差的三件最小接口：

1. **每实例可注入不同规则 profile**（今天所有旋钮只能是整批一致的 `-Drwagent.*`）；
2. **条件固定**：地图/难度已有，缺 **seed 控制与复现验证**；若 seed 不可设（需写引擎内部状态，
   触及 Lab 禁区），替代方案是「重复 N 局 + 配对同条件 + 报方差」，并在结论里写明随机性来源 —— 这一步要裁决；
3. **跨批聚合**：`batch.json` 目前是单批的，参数比较需要一个只读聚合器（按 profile 汇总胜率/经济/损失）。

**同局双 Agent 的 self-play 不在近期范围**：`HeadlessRunner` 明确
`Only local matches are supported`，且 CLI 只有 `--map/--port/--speed/--max-wall-seconds/--frames/--difficulty`；
那是**桥接/大厅层**的独立工程，不是 runner 的能力缺口。

## 5. 边界（与 Codex 一致，我复核后同意）

- 本轮是无头编排/隔离能力验收，**不是策略 E4**；两局 VICTORY 不作为胜率结论。
- 当前候选 `6fec7a0c…`（v1b.1）**尚无实机 E4**；v1b 的实机证据属于 `73a09229…`，不能自动继承。
- 未开 4 实例、未做同局双 Agent、未做 self-play、未动策略/经济语义、未重开 P0/Lab。

## 6. 本轮产物

| 文件 | 内容 |
| --- | --- |
| `助手交接\对话45.txt` | 交接正文（四项任务逐条回答） |
| `助手交接\evidence\parallel_runner_v0_ds_acceptance.txt` | 原始凭证：5 个运行目录的审计输出 + 回归尾部 + 强杀探针 JSON + 身份复算 |
| `_analysis\parallel_pair_audit.py` | 我自写的只读审计脚本（可对任意 run 目录复跑） |
| `_analysis\parallel_pair_acceptance.py` | 汇总驱动（对多个 run 目录调用审计并附回归/探针结果） |
| `_analysis\kill_orchestrator_probe.ps1` | 强杀探针（PowerShell 5.1 需 UTF-8 BOM） |
| `助手交接\evidence\CURRENT_STATE.md` | 状态收束（含实测残留风险） |
| `HANDOFF_FOR_CODEX.md` | §6 环境陷阱新增三条（PS 5.1 无 pwsh / .ps1 需 BOM / 参数引号 / 编码方向） |
| 测试运行目录（保留作证据） | `游戏环境\...\headless-runs\ds-accept\run-20260927T071633-bd2165`、`…\ds-kill-orch\run-20260927T072919-308a98` |
