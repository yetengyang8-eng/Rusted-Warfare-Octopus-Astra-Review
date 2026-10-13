# HANDOFF（对话48 / A/B campaign v0 独立验收 + WinError 5 根因 + 原生回放评估）

> 正式正文：`G:\deepseek 工作台\助手交接\对话48.txt`
> 原始凭证：`G:\deepseek 工作台\助手交接\evidence\ab_campaign_v0_ds_acceptance.txt`
> **产品代码只改了一处**：`tools/run_headless.py` 的原子写对瞬态占用做有界重试（含 2 个新用例）。
> Java 策略 / 聚合器 / campaign 编排 / profile / 冻结件未动。

## 1. 验收：ACCEPT（E2 + E3）

| 任务 | 结果 |
| --- | --- |
| ① 编排 / 持久状态 / 续跑幂等 / PARTIAL 与真失败区分 | AB/BA 交替正确；我自跑 campaign **原命令重跑**后 `launches` 保持 1,1、聚合 SHA 不变；PARTIAL 记为有效但右删失、指标 n=0，不算编排失败 ✅ |
| ② 资源缓存 / junction / 磁盘预算 | episode 的 `assets/libs/res` 是**指向 `<campaign>\resource-cache` 的 reparse point**（不直连原件）；缓存 game-lib SHA == 原件；**原游戏目录 mtime 仍是 2026-09-20（未被写入）**；旧证据未删 ✅ |
| ③ crash recovery / live-claims / --reap | 恢复现场 `reap-report.json`：`killed=2`、`skipped=5`、`verified` 明确；pair-0001 下 3 个 run（旧未完成 + 补跑 + BA）全保留 ✅ |
| ④ 最终聚合不串、不静默混算 | 两个 campaign：我自写的原始目录审计 + 独立重聚合 → 与自带 `aggregate.json`、交接副本 `ab_campaign_v0.json` **逐字段相同** ✅ |
| ⑤ 最小独立冒烟 | `--pairs 2`（120 游戏秒预算）：`COMPLETE`、`launches=1,1`、聚合 SHA `7df348d0…` ✅ |
| 回归 | **17 Java + 12 套 Python，failed steps = 0** ✅ |

## 2. WinError 5：根因 = **并发读者占用状态文件**（已复现）

- 同一失败目录现在重做同样形态的原子写 → **成功** ⇒ 路径 / ACL / 中文目录 / 文件系统都不是原因。
- 另一进程持有只读句柄（无 `FILE_SHARE_DELETE`）时 `os.replace` → **复现同一条** `[WinError 5] 拒绝访问。: '…tmp' -> '…campaign.json'`，连续两次失败，**释放后成功**。
- 严重性**中低**：零数据丢失（完整状态留在 `.tmp`），但 campaign 停在 BLOCKED 并抛原始 WinError。
- **加固**：`write_bytes` 对 `PermissionError` 有界重试（≈3.75 秒）；持续占用仍原样抛出；2 个新用例；
  端到端：我自跑 campaign 第二次运行时**先按住 `campaign.json` 2 秒**，仍 exit 0、`launches` 1,1、聚合不变。
- **给 Codex 的两个后果**：① 因 plan 绑定 `runnerSha256`，**旧 campaign 不能用新字节续跑**（会被明确拒绝）；
  恢复路径 = `evidence\ab_campaign_v0_sources.zip` 里的旧 runner（SHA 正是绑定的 `7355d5be…`）。
  ② 建议（不施工）加一个**有记录的"脚本身份重绑"流程**。

## 3. 原生回放：**暂不可行**

引擎启动时会重写 `preferences.ini`，**启动前预置 `allowGameRecording:true` 被丢弃**；探针无任何 `.replay`、无 `saves/`。
两条可行路径：① 找引擎录制开关（Lab 相邻，先只读侦察）；② **桌面入口**由用户在游戏选项里开录制（成本最低，一分钟可验证）。
参考项目的 replay 命令流分析**以"有 replay"为前提**，同样被卡住。HTML viewer 保留为 Agent 视角诊断工具。

## 4. 下一轮 Codex 合同建议（只建议）

**编排层够用了，下一轮的价值在"跑一次真正有信息量的 campaign"**（n=2/臂只能描述；20 对≈1 小时墙钟、≈0.3 GB）。

- **A（推荐）「A/B campaign 实测 + 统计口径 v0」**：① 裁决 N / 地图 / battle 预算 / 主指标；
  ② 一处小改动：聚合器为 **PARTIAL（右删失）** 局输出**单独标注**的描述统计块（不与原生完成指标混算）——
  这正是当前短预算 campaign `n=0`、信息量为零的直接原因；③ 无人值守跑满 N；④ 只报描述统计与不确定性，**不判优劣**。
- **B「campaign doctor」**：脚本身份重绑 + BLOCKED 现场自检/续跑（承接 §2 的后果）。
- **C 只读侦察（可并行）**：同局双 Agent/self-play 接口风险图；seed 初始化期注入口；引擎录制开关。

**子智能体预算**：默认 ≤2、重大工程 ≤3；**同一时刻只有一个代码 owner**，其余只读并只交文本结论。
按 A 执行建议 **2 个**；若同时要 C 的两项侦察则 **3 个**。

## 5. 本轮改动与产物

| 文件 | 内容 |
| --- | --- |
| `游戏环境\...\tools\run_headless.py`（+ 冒烟环境副本） | 原子写瞬态占用重试（唯一代码改动） |
| `游戏环境\...\developer\tests\test_headless_parallel.py` | 新增 2 个原子写用例（套件 22 → 24） |
| `_analysis\ab_campaign_v0_audit.py` | 我的 campaign 独立审计（原始目录 + 独立重聚合对照） |
| `_analysis\winerror5_forensic.py` | WinError 5 两个受控探针（复现 + 排除路径/权限） |
| `_analysis\replay_feasibility_probe.py`、`replay_probe_engine.log` | 原生回放有界探针与日志 |
| `_analysis\ds_campaign_verify.ps1` | 我自跑的 campaign 冒烟 + 读者占用压力验证 |
| `助手交接\对话48.txt`、`evidence\ab_campaign_v0_ds_acceptance.txt` | 验收正文与原始凭证 |
