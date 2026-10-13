# HANDOFF（对话46 / orchestrator crash recovery v0 完成）

> 正式正文：`G:\deepseek 工作台\助手交接\对话46.txt`
> 原始凭证：`G:\deepseek 工作台\助手交接\evidence\crash_recovery_v0.txt`
> **只改了 Python 编排器 + 测试 + 文档**；Java 源码 / 候选 JAR / 策略语义 / 冻结件未动，复核哈希未变。

## 1. 五项验收（全部通过）

| 要求 | 实测 |
| --- | --- |
| ① Ctrl+C 路径不回归 | 既有 `CancellationCleanupTests` 通过（套件 12/12 绿） |
| ② 强杀编排器后引擎残留 | A pid=20992 / B pid=4724 在 +1s/+10s 都存活，端口 14900/14901 都仍 LISTEN |
| ③ `--reap` 只清本批 | `killed=2`（正好两个引擎）、`skipped=5`、exit 0、`verified=["every claimed pid is gone and every claimed engine port is released"]` |
| ④ 幂等 | 第二次 `killed=0`、exit 0、PASS |
| ⑤ 新批次可跑 | `smoke --episodes 2 --parallel-pair` → `completed=2 passed=2 proof=PASS`、exit 0 |
| 回归 | **Java harness 17 项 + Python 10 套，failed steps = 0**；`test_headless_parallel` 7 → **12** 用例 |

## 2. 实现要点

- `live-claims.json`：编排器启动即写、进程创建/退出时更新（原子写）；登记 `role/pid/port/workDirectory/
  episode/sessionId/commandMarker/state`。**查不到进程表时不判断**（保持 RUNNING），避免把"查不到"当"已退出"而掩盖泄漏。
- `--reap <run 目录>`：只读登记簿，逐条复核身份（引擎需 `io.rwagent.headless.HeadlessRunner` + `--port <登记端口>`
  + 该端口 LISTEN 属主为本 PID；客户端需 `io.rwagent.client.` + `-Drwagent.port=<登记端口>`；
  **orchestrator 角色永不回收**），才 `taskkill /PID /F`；之后复核 PID 消失与端口释放，
  只有"确实杀过"的条目才计入失败。幂等，产物 `reap-report.json`。
- `--game-dir` 改为可选；不带 `--reap` 时行为与之前完全一致。冒烟环境副本已同步（两处 SHA256 相同）。

## 3. 剩余风险

1. 引擎刚 Popen 就被杀的瞬间可能少写一条记录 → 最坏漏清理一个进程，**不会误杀**（reap 报告会列出未释放端口）。
2. `--reap` 依赖 Windows PowerShell 查询；查不到身份时**一律跳过而不杀**（"绝不误杀"的取舍）。
3. 端口 TOCTOU 窗口仍在（上一轮已登记）。
4. reap 不做跨批次清理（一次只认一个 run 目录，避免越权）。

## 4. 文档顺手修正（裁决要求）

- 「下一次验证」不再把 `NO_PROGRESS_TARGET_HANDLING v0` 当待验收项（已 E4 收官）；改为指出
  **当前候选 `6fec7a0c…`（v1b.1）尚未实机 E4**。
- Builder Utilization v0 的 E4 按原始报告统一为 **5x + 1x**（`battle-1790444931065` 倍速 5.0 +
  `battle-1790445226048` 倍速 1.0，候选均 `f3d63e75`）；巨岛/冰岛（都是 5.0、候选 `73a09229`）归 Economy v1b。
- Economy v0 的验收局改回它自己的 `1663738e` 巨岛局，不再与后来几局混淆。

## 5. 下一步（只提建议，未施工）

**「固定条件 A/B 对比能力 v0」**：① 每实例规则 profile 注入；② seed 控制与复现验证
（若不可设则改为"重复 N 局 + 配对同条件 + 报方差"，**需裁决**）；③ 跨批聚合器。
4 实例 / 同局双 Agent / self-play / 策略语义改动仍未开工。

## 6. 本轮改动文件

| 文件 | 改动 |
| --- | --- |
| `游戏环境\Rusted-Warfare-1.15-Agent-0.07\tools\run_headless.py` | `LiveClaims` / `process_info` / `port_owners` / `claims_identity` / `reap_run`；episode/client 登记；`--reap`；`--game-dir` 可选 |
| `游戏环境\P1F-冒烟环境\tools\run_headless.py` | 同步（两处 SHA256 = `1121df0833aa12a2…`） |
| `游戏环境\...\developer\tests\test_headless_parallel.py` | `LiveClaimsAndReapTests` 5 用例 + 既有用例补登记簿断言 |
| `助手交接\evidence\crash_recovery_v0.txt` | 本轮正式凭证（新增） |
| `助手交接\evidence\CURRENT_STATE.md`（+ 根目录副本） | 强杀残留改为"已修"+剩余风险；两处按原始报告修正 |
| `_analysis\crash_recovery_probe.ps1` | 端到端故障注入探针（PS 5.1 需 UTF-8 BOM） |
