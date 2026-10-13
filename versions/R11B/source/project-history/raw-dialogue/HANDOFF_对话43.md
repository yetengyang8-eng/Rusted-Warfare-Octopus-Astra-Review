# HANDOFF（对话43 / Economy v1b 实机验收）

> 正式交接正文是 `G:\deepseek 工作台\助手交接\对话39.txt`（含两个待裁决问题与全部数据）。
> 本文件是索引 + 完成回执。**本轮零代码改动**，候选仍是 `73a09229bfccaf59…`。

## 1. 一句话结果

用户实机两局（巨岛 5x、冰岛 5x），都跑了 v1b 候选：

- **巨岛 5x**：`factory_target_increased` @316.2 游戏秒（满载 82.2%、收入 63.1 − 消费 50.3 = 剩余 12.8/秒、
  余额 860.5、两个硬储备为 0、**army 19/40**）→ 第 3 座厂 @387.3 建成 → `production_facility_finished(factories=3)`；
  产能 46.0 → 74.7/游戏秒；`investment_reserve_stuck` 0 条。**裁决 §6 的验收链完整走通。**
- **冰岛 5x**：未触发，唯一卡点 `FACTORY_NOT_SATURATED`（76.8% < 80%），收入/余额/储备都合格；该局 PASS/VICTORY。
- **附加收获**：写进代码的 `26.9 / 12.07` 在**未参与拟合**的这两局上得到确认
  （6 局合并稳健拟合 12.06 / 26.91，逐档中位数不变）。

## 2. 请 ChatGPT 裁决的三件事（详见 `对话39.txt` §5）

1. **Q1**：80% 满载阈值维持 / 下调 / 换成"需求 ≥ 产能"的直接读数（队列非空**或**因余额被推迟生产）？
   我倾向维持现状，把新判据先做成只读复算再决定。
2. **Q2**：建造路径仍用**单帧** `lastFactoryQueueNonEmpty`（冻结的 P2-B 闸门），实测让"目标 +1 → 下单"
   多等 **47.3 秒**（其间 6 次 `FACTORY_QUEUE_EMPTY`）。是否把该闸门也换成同一长窗口读数？
3. **Q3**（纯显示）：`factory_target_increase_blocked.saturationPct` 只留 1 位小数，
   79.98 被拒会显示成"80.0 却拒绝"。改成 2 位小数（无行为影响），与 Q1/Q2 同轮做。

## 3. 本轮读了什么 / 改了什么

- **新增只读证据**：`G:\deepseek 工作台\助手交接\evidence\economy_v1b_live.txt`
  （脚本 `G:\deepseek 工作台\_analysis\v1b_live_acceptance.py`）。
- **更新**：`CURRENT_STATE.md`（v1b 标为 DONE_AND_LIVE_VALIDATED；记入两条后续与一条显示缺陷）。
- **未改动**：产品源码、回归、候选 JAR、`docs\BATTLE_CN.md`。
- 额外复算（同一只读脚本）：`G:\deepseek 工作台\_analysis\six_match_income.txt`——
  6 局合并的收入口径 v2/v3 对照，用于样本外检验写进代码的常数。

## 4. 原始报告（如需自己复算）

```
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790483259213-91b9f83c.jsonl   ← 巨岛 5x（第 3 座厂）
G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-reports\battle-1790483501409-8f7b1014.jsonl   ← 冰岛 5x（VICTORY）
```

复跑命令：

```powershell
$env:PYTHONIOENCODING='utf-8'
$env:ACC_REPORTS='["G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790483259213-91b9f83c.jsonl","G:\\deepseek 工作台\\游戏环境\\P1F-冒烟环境\\rw-agent-reports\\battle-1790483501409-8f7b1014.jsonl"]'
$env:ACC_LABELS='{"battle-1790483259213-91b9f83c.jsonl":"巨岛(5x)","battle-1790483501409-8f7b1014.jsonl":"冰岛(5x)"}'
python "G:\deepseek 工作台\_analysis\v1b_live_acceptance.py"
```
