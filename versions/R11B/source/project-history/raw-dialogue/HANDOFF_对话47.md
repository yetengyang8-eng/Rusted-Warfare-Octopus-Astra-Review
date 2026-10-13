# HANDOFF（对话47 / 固定条件 A/B 对比 v0 独立验收）

> 正式正文：`G:\deepseek 工作台\助手交接\对话47.txt`
> 原始凭证：`G:\deepseek 工作台\助手交接\evidence\ab_v0_ds_acceptance.txt`
> **本轮未改任何代码**（Java 策略 / 运行器 / 聚合器 / profile / 冻结件全未动）。

## 1. 结论：ACCEPT（E2 + E3）

| 验收项 | 我的做法 | 结果 |
| --- | --- | --- |
| A/B 位置是否真的交换 | 只读两批原始目录的 `abProfiles.profileOrder` + 每局 `abProfile`，用 `battle_config` 反查 | AB 批 ep1=cap32/ep2=cap40，BA 批反向 ✅（我自跑两批同样交换） |
| cap 实际值 / profile 只进客户端 | 逐局核 `声明值 → 阶段记录的客户端 -D → **原始 battle_config**`；核引擎命令行 | 三者一致；引擎命令行**无** profile 参数 ✅ |
| 身份与隔离 | 报告 SHA vs 登记、报告自证 JAR、session 集合、报告种类、parallel proof 五项隔离 + 帧前进 | 全部通过（含我自跑四局）✅ |
| 谱系 | 独立重算 contentDigest / whole-file SHA / 冻结件 | 安装件 == dist == 源码（`efd51831…` / `6fec7a0c…`）；冻结件未变 ✅ |
| 聚合器拒绝能力 | **我自己构造 7 个变异样本** | 硬拒绝（exit 2）3 例 + 结果内 `INVALID` 并排除 2 例 + 两条交叉判据 ✅，**无静默混算** |
| 自跑最小 campaign | AB+BA、`--battle-seconds 240` | 四局 PARTIAL/ONGOING → 聚合器记 `partial=2`/臂、指标 `n=0`、分母保留 ✅ |
| 回归 | 我在当前树重跑统一入口 | **Java 17 + Python 11 套，failed steps = 0** ✅ |

**n=2/臂只能描述，不裁决 cap32/cap40 优劣**（四局 seed 各不同、`seedReproducibilityVerified=false`）。
E 级划分见正文 §2：位置/cap/隔离/拒绝能力/谱系 = E2+E3；回归 = E2；
桌面图形等价性、跨进程确定性、seed 复现 = **未经验证**（仅对方自述）。

## 2. 我的两条独立发现（只记录，未施工）

1. **迁址陷阱**：复制/搬走 run 目录后不重算回写报告 SHA → 聚合器把每一局判成 `battle raw SHA mismatch`，
   表现为"没有数据"（**exit 0、n=0**）。归档要保持字节与路径原样；**验收看 `validBattleReports` /
   `invalidOrMissingReports` / `n`，不能只看退出码**。
2. **磁盘成本**：每个 run 目录 ≈ **76 MB**（`stage()` 的 `assets/libs/res` 因无符号链接权限回退成 copytree）。
   建议下一轮改 junction，并给 campaign 加磁盘预算检查。

## 3. 下一阶段评估（只读建议）

- **主任务：`A/B campaign v0`** —— 三块拼图已齐（profile 注入 / 崩溃恢复 / 跨批聚合），campaign 只差编排层。
  最小内容：① AB/BA 自动交叉 + `campaign.json`；② 断点续跑与幂等；③ **失败策略必须区分"编排失败"与
  "没有原生结果"**（本轮实测：240 秒预算四局全 PARTIAL、运行器 exit 1，那是正常数据，不能中止 campaign）；
  ④ 一次收口聚合（给 `attempted/valid/partial/invalid/n`，**不产出优劣结论**）；⑤ 磁盘与清理。
  样本量 N **本轮不定**，留待裁决。
- **只读侦察 1（同局双 Agent / self-play）**：**不适合作为主任务**。`HeadlessRunner` 明确
  `Only local matches are supported`；桥是"每引擎进程一个端口、无队参数"。需要一局两个非 AI 玩家 +
  两个桥绑不同队 + 按队路由 → 触及引擎/桥接层（P0/Lab 边界）。只做只读风险图，不实现。
- **只读侦察 2（seed 初始化期注入口）**：目前**未找到**——`--seed` 不存在、创建路径不暴露 seed、
  `seedReproducibilityVerified=false`；运行中写引擎内部状态属禁区 ⇒ 按裁决判**不适合主线**，
  只做一次有界只读探查，否则承认"同条件不同随机"用 N 承担方差。
- **子智能体预算**：默认 ≤2，重大工程 ≤3；**主工作区同一时刻只有一个代码 owner**，其余只读、只交文本结论。
  本 sprint 建议 3 个（代码 owner = campaign 编排层；两个只读侦察见上）。

## 4. 顺手修的文档事实

`CURRENT_STATE.md` 的 crash-recovery 段原写"回归 17 + 10 套、`test_headless_parallel` 12 用例"——
那是**该轮**事实，与当前"17 + 11 套 / 22 用例"并列会误读；已标注"该轮/当前"。
A/B 段的状态已从"待独立验收"改为"**独立验收通过（ACCEPT，E2+E3）**"并补入本轮实测与两条发现。

## 5. 本轮产物

| 文件 | 内容 |
| --- | --- |
| `助手交接\对话47.txt` | 验收正文（五项任务 + E 级 + 下一阶段评估 + 子智能体预算） |
| `助手交接\evidence\ab_v0_ds_acceptance.txt` | 原始凭证（含 7 个变异样本矩阵、我自跑 campaign 的聚合结果） |
| `_analysis\ab_v0_audit.py` | 我自写的原始目录审计（可对任意 A/B 批次复跑） |
| `_analysis\ab_aggregate_rejection_test.py` | 变异样本测试（含"迁址 + 回写 SHA"的等效副本构造） |
| `_analysis\ds_ab_campaign.ps1` | 我自己的最小 AB+BA campaign 驱动 |
| `助手交接\evidence\CURRENT_STATE.md`（+ 根目录副本） | 状态改为"独立验收通过"，补两条发现与计数消歧 |
