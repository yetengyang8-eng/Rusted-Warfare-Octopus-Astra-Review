# HANDOFF（对话42 / Economy v1b 实现完成）

> ChatGPT 已在《Economy v1b 正式裁决》里批准本方向（用户同意）。本文件只是**完成回执 + 路径索引**，
> 没有待裁决问题。ChatGPT 可直接只读访问 `G:\deepseek 工作台`。

## 1. 已完成（按裁决 §6 的清单）

| 裁决要求 | 状态 |
| --- | --- |
| 源码 | ✅ `BattleClient.java`：`considerFactoryTargetIncrease` / `observeFactoryLoad` / `factoryLoadWindow` / `productionConsumptionPerGameSecond` / `modelledIncomePerGameSecond` / `sustainableSurplusPerGameSecond` / `reportFactoryTargetIncreaseBlocked` |
| 回归 | ✅ `developer\test-win.ps1`：Java harness 17 + Python 9 套，**failed steps = 0**；`test_battle_client.py` 68 → **75** |
| 安装候选 | ✅ `游戏环境\P1F-冒烟环境\rw-agent-bootstrap.jar` = dist = 源码内容 |
| 谱系 | ✅ `candidate_sha_lineage.json` 已重新生成（`_analysis/assemble_lineage.py`，不再是手工拼的） |
| CURRENT_STATE | ✅ 已更新 |
| 陈旧元数据清理（§1） | ✅ `candidate_sha_lineage.json` 里 `10.1 / 34.5 / "use 10.1 in v1b"` 已替换为 `12.07 / 26.9` + 口径 + 取代说明 |

候选：whole-file `73a09229bfccaf59…`，contentDigest
`6a51a1dcc9a3e6aa3e6715f011863f7c9e4fbbf53c47011e36d1620f797e54a5`。

## 2. 实现摘要（与裁决逐条对应）

- **§1 常数**：`26.9` / `12.07` 进代码，`-Drwagent.baseIncome` / `-Drwagent.incomePerMine` 可覆盖，
  `report_provenance.economyModelSource` 与 `battle_config` / 汇总都带来源与样本口径。
- **§2 触发**：`现有陆厂长期满载 + 可持续收入 > 生产消费 + 现金 ≥ 厂价 + 硬储备 → landFactoryTarget +1`
  （上限 `-Drwagent.landFactoryTargetMax`=5）。四条闸门全部长窗口：窗口预热 = 对局至少一个
  `-Drwagent.economyWindowMs`（默认 150 游戏秒）；满载 = 队列非空的**时间加权**占比 ≥ 80%
  （`-Drwagent.factorySaturationPct`）；**没有**任何 `credits > X` 或"剩余 ≥ Y"的拍脑袋裕度。
- **§3 不做**：矿升级、厂升级、防御塔、Scout/Role/World Model/Enemy Memory/跨海、`MILITARY_URGENCY`
  阈值、`army ≥ 24` 门槛、Investment Candidate 排序 —— 全部未动。
- **§4 above-floor 矿损**：登记为独立 backlog `ABOVE_FLOOR_ECONOMIC_LOSS_REEVALUATION`
  （语义按裁决：压力期间可不补，压力解除后重新评估当前最佳投资），未实现。
- **§5 诊断字段**：`own_loss` 补 `type`/`x`/`y`；`extractor_completed`/`factory_completed` 补
  `completedAtGameMs`。**不改行为**。
- **§6 验收**：需要一个可见的第 3 座厂 → **正在请求用户实机**。

## 3. 一条实现期发现（不是阻塞，供下一轮参考）

按最终阈值重跑四局历史报告（`v1b_trigger_feasibility.txt`）：**3 局会触发**，但条件只以
**2~8 游戏秒的簇**成立。原因是长期满载（≥80%）与收入>消费在真机数据里都贴着阈值上下波动。
所以"某局没加第 3 座厂"**不足以判定实现错误**，判据应该是
`factory_target_increase_blocked` 的最后一条理由 + 当时实测值；这一点已写进 `economy_v1b.txt` §7 看板。

## 4. 请 ChatGPT 读的文件

| 路径 | 内容 |
| --- | --- |
| `G:\deepseek 工作台\助手交接\evidence\economy_v1b.txt` | 实现与回归证据（本轮回执正文） |
| `G:\deepseek 工作台\助手交接\evidence\v1b_trigger_feasibility.txt` | 触发可达性预检（含军队触顶反例） |
| `G:\deepseek 工作台\助手交接\evidence\CURRENT_STATE.md` | 当前状态（候选、闸门、backlog、验收看板） |
| `G:\deepseek 工作台\助手交接\evidence\candidate_sha_lineage.json` | 谱系 + 已修正的经济常数元数据 |
| `G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07\docs\BATTLE_CN.md` | 新增「经济 v1b」与 Builder Utilization 实机验收两节 |
| `G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\tests\test_battle_client.py` | 新增 `FactoryTargetIncreaseTests`（7 个用例） |

## 5. 下一步

等用户实机一局（1x 或 5x 均可）。回来后按 `economy_v1b.txt` §7 看板核对；
若触发条件在真机上从未成立，会先给出卡在哪道闸门与实测值，再决定调阈值还是改机制。
