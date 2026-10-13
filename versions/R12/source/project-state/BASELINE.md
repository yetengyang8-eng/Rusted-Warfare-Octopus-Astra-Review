# 工程基线：RW-BASELINE-2026-09-30-GS-v1

建立日期：2026-09-30（Asia/Shanghai）。这是后续工作的默认起点。基线描述的是一份可识别、可复核、已知限制明确的工程状态，不表示策略已经稳定或竞技性能已经达标。

本文件负责基线定义；[baseline-manifest.json](baseline-manifest.json)负责机器可读身份；[CURRENT_STATE.md](CURRENT_STATE.md)负责当前进度；[NEXT_STAGE_PLAN.md](NEXT_STAGE_PLAN.md)负责下一阶段。旧交接只作历史资料，不能覆盖这些入口。

## 1. 唯一开发起点与版本身份

| 项目 | 固定值 |
| --- | --- |
| 基线 ID | `RW-BASELINE-2026-09-30-GS-v1` |
| 功能版本 | Global Strategy / Feasibility / Combat Engineer |
| 本地源码仓库 | `G:\deepseek 工作台\GitHub发布\Rusted-Warfare-Agent` |
| 已导入源码及证据的 Git 起点 | `6214f073d1b76429c0b827268db918e1e90e9f5f` |
| 生产代码导入提交 | `21d307b`；后续 `6214f07` 补 raw evidence |
| 不可变交付件 | `astra-deliveries/global-strategy-2026-09-30/rw-agent-bootstrap.jar` |
| 交付 JAR SHA256 | `76711a8ee2716af5d5b0b66d5c53b159ae28f4f744dcbb24cae492d96f0342ed` |
| JAR contentDigest | `8092b61130c3497b370e12ce249d3ec17e6331d502fefbcb50bbd95a32f03120` |
| 原版引擎 | Rusted Warfare PC 1.15 / Build 28 / Game Code 176 |
| game-lib.jar SHA256 | `8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9` |
| `agent/resources/knowledge/UNIT_CATALOG_CANDIDATES.json` SHA256 | `263cdaaa3e923e8307c37f059e50bee529b8ecd7c3e215937ce2adf615a0e236` |

交付方记录的 `2f310ec46a4225eb1ef77efe68d1823904e52717` 是来源声明，本地 Git 没有该对象，不能把它说成本地 main 的已验证祖先。未来分支从包含本基线文档的本地标签 `baseline/2026-09-30-gs-v1` 或其后续提交创建；表中的 `6214f07` 固定生产输入。导入内容与交付 overlay 已核对，本次 Windows 重建的 87 个非 manifest 条目与最终交付完全一致，见基线验收记录。

whole-file SHA 标识具体归档；contentDigest 按排序后的 ZIP entry 名和内容 SHA256 计算，含目录 entry，排除 `META-INF/MANIFEST.MF`。重建 JAR 必须单独登记，不能只因功能相同就继承原交付件的实机证据。

## 2. 环境角色：不要从旧副本继续开发

| 路径（相对工作区） | 角色 | 当前身份 |
| --- | --- | --- |
| `GitHub发布/Rusted-Warfare-Agent/agent` 与 `tools` | 唯一默认源码和工具 | Git `6214f07` 的生产输入 |
| `游戏环境/P1F-Astra-GlobalStrategy-2026-09-30` | 最新候选的独立桌面验收环境 | JAR `76711a8e…`，本轮只读核对 |
| `游戏环境/P1F-冒烟环境` | 历史 KnowledgeBacked 环境 | JAR `5741e241…`，保留原状 |
| `游戏环境/Rusted-Warfare-1.15-Agent-0.07/developer` | 历史开发树，禁止默认为最新 | dist `5741e241…`，保留原状 |
| `游戏环境/Rusted-Warfare-1.15-Agent-0.07` 根 JAR | 冻结历史交付 | `0681b4f7…`，不得覆盖 |
| `_baseline_audit/2026-09-30-gs-v1` | 本次隔离构建与回归输出 | 不作为游戏安装目录 |

本次建立基线不复制新 JAR 到旧冒烟环境，不同步覆盖旧开发树，也不更改地图、难度、配置、存档或回放。后续新候选在独立目录构建和验收；部署应明确具体目标目录、旧件备份和回退方式。

## 3. 现有能力

- 规则型 Java Agent 与独立客户端；只消费合法视野下的动态敌情，失联情报保留 UNKNOWN 和最后已知证据。
- 侦察任务归属、命令接管、移动执行确认、短航段与到达后的合法视野核查。
- 已拒绝目标的负证据跨失联保留，新的合法兼容观察才能解除；可见目标与记忆目标有明确选择边界。
- 目标判断区分武器兼容、移动接近、实际攻击位置；不能抵达的任务可产生能力需求。
- 工程师和两栖响应者任务、动态军力/建造预算、T2 矿升级及战略施工。
- 长局报告支持超过旧 64 MiB 限制。它是当前写入实现的能力，不应写成已交付完整滚动分片系统。

## 4. 验证矩阵与证据边界

| 层次 | 当前结果 | 可用结论与边界 |
| --- | --- | --- |
| 最终交付 JAR Linux 回归 | 21 Java runs、16 Python suites；279 pass / 2 Windows 用例 skipped；0 failed steps | 对应不可变交付 JAR；不能继承为 Windows 全回归 |
| 最终候选原生 E2 | 46 项；另有 48 项策略契约检查 | 原生对象 fixture，不是自然对局 |
| 最终候选无画面自然局 | Battle 644.512 秒 VICTORY；84/84 攻击确认；峰值 56 armed | 单局结果；该局未执行工程师/T2 矿投资，不能冒用中间候选链路 |
| 最终候选 Windows 桌面 | 三场约 2400 游戏秒、5x；三场均 PARTIAL/ONGOING；专项审计零违规 | 已有真实自然执行；没有三场的终局结果，也没有胜率或因果增益结论 |
| 本次 Windows 隔离重建与回归 | PASS_COMBINED_MATRIX：40 步；281 Python 全通过，0 Python skipped | 首轮 39/40，补齐遗漏的同提交测试数据后受影响套件 16/16；POSIX 专用 Java 检查按平台跳过；详见验收记录 |

三场桌面对照固定如下，作为下一阶段的诊断输入：

| Battle ID | 地图/控制域 | 峰值移动武装单位 | 关键事件 |
| --- | --- | ---: | --- |
| `1790741949748-c01f222f` | Big Island 180×180 / team:0 | 88 | 13 个能力需求；出现接近缺口→响应→进展→可见伤害→合法解除需求 |
| `1790742800972-4064cebd` | Spain 400×370 / team:5 | 21 | 237 新战斗单位、243 己方损失；8 个能力需求；没有战略投资/响应任务 |
| `1790743783098-ae01b65e` | Spain 400×370 / team:5 | 101 | 139 新战斗单位、82 己方损失；52 次投资命令、13 次矿升级、30 次战略施工 |

Spain 两局都在 Match 启动前由操作者生产一个 builder，此后自主运行；不是原生出生零干预起步。raw report 缺 `mapPath/difficulty`，地图名由操作记录佐证；难度及未记录 seed 不能补猜。两局不是已控制全部条件的 A/B，只能用来定位机制分歧。

最终候选与中间/预最终样本按 JAR 身份隔离。中间候选的“工程师造喷气机→海厂清场”完整链不得直接计为最终候选已重复通过。

核心证据入口：

- [最终交付身份与 Linux/原生验证](../evidence/global-strategy-2026-09-30/README.md)
- [三场桌面 E4、raw report 索引与人工干预边界](../evidence/global-strategy-desktop-2026-09-30/README.md)
- [本次基线完整性与 Windows 验证](../evidence/baseline-2026-09-30/VALIDATION.md)

## 5. 已知限制和下一阶段主线

首要工程问题是同图 Spain 21/101 的轨迹差异：能力需求为何没有稳定转成投资和可执行响应。共享命令时隙、投资窗口、军力/紧急防御门槛、资金储备、工厂队列和任务 ownership 都是待核验因素，当前没有单一根因结论。

其次是响应者生存、成组执行、经济积压与终局清理；原生无 builder 起步尚不支持。修理/回收、完整海军、自适应两栖切换、同局双玩家和大规模胜率评估均未完成。分阶段工程方案见 [NEXT_STAGE_PLAN.md](NEXT_STAGE_PLAN.md)。

## 6. 不变的安全与证据约束

1. 不读取迷雾隐藏动态敌情来补策略；静态资源目录不能覆盖 UNKNOWN。地形可达不等于战斗安全或任务收益。
2. 不恢复危险 live 反射：`am.cj()` 是杀死对象的 mutator；`ar.a()` 会构造并注册单位；禁止未审计反射和仅按名称解析。不要把历史“所有方法都不能调用”误解为禁止正常下令；现有经审计直接调用保留，新读取面先独立审计。
3. 不修改冻结 `game-lib.jar`、原版资源及历史根 JAR；不改用户设置、存档、回放，不自行启动或关闭用户桌面游戏。
4. 独立无画面验证使用独立 reportDir、端口、工作目录和进程身份；既有 runner 最多两个独立引擎，不能以此宣称同局双玩家已实现。
5. 游戏时间和墙钟时间分开；queued/accepted 不等于已执行；有伤害不等于任务已完成；预算到期 PARTIAL/ONGOING 不算胜利或崩溃。
6. 历史阶段合同保留事实与事故边界，其过时的源码路径、策略禁令、旧候选和旧安装命令不再定义当前工作范围。新任务以用户当次授权和本基线为准。

## 7. 后续任务怎样开始与晋级

先读本文件、manifest、当前状态和计划，核对 Git 状态与候选身份。默认从本仓库推进；如果源码或安装件已偏离，先说明差异，保留用户改动，不回滚、不复制旧树来“修复”。

只读核对命令（Python 3，仓库根目录运行）：

```text
python project-state/verify_baseline.py
python project-state/verify_baseline.py --workspace "G:\deepseek 工作台"
```

后续候选必须有独立身份、基线对照、针对失效机制的回归与必要原生证据；一次只晋级已经满足验收门槛的变化。更新 manifest、证据矩阵、CURRENT_STATE、接手入口和可回退交付件后，才创建下一个基线 ID。旧基线与原始证据保留；不得原地改写旧结果。
