# HANDOFF（对话38 / 工程轮次 42）

> 交接说明：交接文件序号是 **对话38**；工程轮次计数（代码注释/证据里用的那个）是 **42**。两者不同源，别混。
> ChatGPT 现在可直接只读访问 `G:\deepseek 工作台`，所以本文件只给**精确路径**，不再打包分发。
> `CHATGPT_HANDOFF.zip` 仅作离线兜底（本轮已重建）。

## 1. 这一轮做了什么

用户交来两局新实机（第一局 5x、第二局 1x），跑的都是我上一轮交付的候选 `f3d63e75…`（`Builder Utilization v0`）。
我按 ChatGPT 上一份《矿收入口径修正与双倍速实机计划》给的口径，把只读分析扩到**四局合并**重算收入，
并补齐了两项之前没有的实机测量（结构建造耗时、每座矿完工那一刻的 army）。**没有改任何产品代码。**

## 2. 请 ChatGPT 读的文件（按重要性）

| 路径 | 为什么 |
| --- | --- |
| `G:\deepseek 工作台\助手交接\对话38.txt` | **本轮正式交接正文**：结论、方法学对比、v1b 设计提案与三点待裁决问题 |
| `G:\deepseek 工作台\助手交接\evidence\two_speed_economy_builder.txt` | 主证据（E61，NEW）：四局合并收入口径 v2/v3、建造耗时、builder 闲置归因、每座结构完工时的 army/矿数/余额 |
| `G:\deepseek 工作台\_analysis\two_speed_economy_builder.py` | 生成上表的脚本（只读，环境变量驱动，可复跑） |
| `G:\deepseek 工作台\助手交接\evidence\CURRENT_STATE.md` | 当前状态速查（本轮已更新：候选、样本表、v0 验收、v1b 提案、backlog） |
| `G:\deepseek 工作台\助手交接\evidence\EVIDENCE_INDEX.md` | 证据索引（E61 = 本轮新增；E60 `alite_ledger_builder.txt` 的 10.1 已被 E61 取代） |
| `G:\deepseek 工作台\助手交接\evidence\alite_ledger_builder.txt` | 上一轮 A-lite（对照用；其 10.1 结论已作废） |

原始报告（如需自己复算）：

```
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790444931065-1b7fa4c7.jsonl   ← 新局C（5x, f3d63e75）
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790445226048-8149ed41.jsonl   ← 新局D（1x, f3d63e75）
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790441059316-57d35d69.jsonl   ← 旧局2（5x, c7bf22e5, v0 之前）
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790440004071-9f97c745.jsonl   ← 旧局1（5x, 3fa9d7ab）
```

复跑命令（PowerShell，只读）：

```powershell
$env:PYTHONIOENCODING='utf-8'
$env:ALITE_REPORTS='["G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790440004071-9f97c745.jsonl","G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790441059316-57d35d69.jsonl","G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790444931065-1b7fa4c7.jsonl","G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790445226048-8149ed41.jsonl"]'
$env:ALITE_LABELS='{"battle-1790440004071-9f97c745.jsonl":"旧局1","battle-1790441059316-57d35d69.jsonl":"旧局2","battle-1790444931065-1b7fa4c7.jsonl":"新局C","battle-1790445226048-8149ed41.jsonl":"新局D"}'
python "G:\deepseek 工作台\_analysis\two_speed_economy_builder.py"
```

## 3. 四条结论（一句话版）

1. **收入尺子定了**：`收入 = 26.9 + 12.07 × 已完工矿数`（资金/普通游戏秒），四局 20 窗口 R²=0.999；
   **单矿回本 58.0 游戏秒**，机制库 12.1 / 58 秒获实机确认。**上一轮的 10.1 是口径偏差，作废**（旧口径同批给出 9.7~11.5）。
2. **建造耗时实测 17.0~17.9 秒**（23 座结构，1x/5x 一致），机制库 16.7 秒命中；
   同时提醒一个坑：`*_completed` 事件**没有 gameTimeMs**，其 `firstSeenGameMs` 是**开工**不是完工。
3. **`Builder Utilization v0` 验收通过**：没有任何闸门挡着却闲置的时间 26.3/60.3 秒 → **0.0/2.1 秒**；
   60 秒退避缺陷在旧候选上留下精确指纹（COMPLETED@448.7 → 下一个 intent@509.0 = 60.3 秒），新候选同位置 1.0~2.6 秒。
4. **新发现（影响 v1b）**：`measuredIncomePerGameSecond` 只在 ≥150 游戏秒的长窗口可信——
   客户端按"下单批次"整笔记账、游戏按"逐件完工"逐笔扣款（"无支出观测对"在低矿档实测为**负收入**）。

## 4. 需要 ChatGPT 裁决的三点（详见 `对话38.txt` §6.2）

1. 是否允许把实测常数 `26.9 / 12.07` 写进代码（带 `-Drwagent.*` 覆盖 + provenance 标注）？
2. 动态第 3 座厂是否现在做？（钱在军队触顶后没有出口；矿升级/工事按前裁决不做）
3. 1x 局丢 2 座矿后未重建（矿数 4 仍高于下限 3，越基线通道又正被军事压力关着）——接受为策略后果，还是记成独立 backlog？

## 5. 本轮未动的东西（避免误解）

- 未改任何产品代码；未重跑回归；候选仍是 `f3d63e75…`（`dist` == 已安装件，`contentDigest fff2e612…`）。
- 冻结交付物完好：环境根 `rw-agent-bootstrap.jar` = `0681b4f7…`、`game-lib.jar` = `8a550a37…`。
- 军事紧迫度阈值、`army ≥ 24` 意图门槛、`mineTarget` 下限、矿/厂升级本版都不动。
