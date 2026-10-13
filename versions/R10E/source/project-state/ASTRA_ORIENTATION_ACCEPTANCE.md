# Astra 第一轮资格验收合同（2026-10-01）

目的：第一轮不是开发，而是证明 Astra 已经准确掌握项目、能运行仓库测试/原生 headless 环境、能通过 GitHub 独立分支写回。只有完成本合同，第二轮才进入实质施工。

## 0. 范围限制
- 本轮不要修改 `agent/src`、生产资源、策略参数或候选 JAR。
- 不做架构重写，不重新发明路线图，不重复整理旧历史。
- 可以创建一个仅用于资格验收的 Git 分支和一份 orientation report。
- 发现矛盾、缺证据或环境失败时如实记录，不为“通过”而修改事实。

## 1. 项目阅读证明
按 `ASTRA_START_HERE.md` 的顺序阅读，并在报告中用仓库路径/类名/方法或符号证明你实际读过。
必须准确说明：
1. 当前 Git main/HEAD、冻结基线、当前固定候选及 JAR SHA256。
2. FEEDBACK-v1 已落地的主要能力，以及仍未解决的边界。
3. 最新用户桌面 Spain 4800s 与 Big Island 2400s 的关键事实和证据边界。
4. 为什么主线已转为“经济增长→产能扩张→可替换生产通路→军事交付→并行操作吞吐→地图控制”。
5. 哪些结论是日志已证明，哪些只是工程解释/下一步假设。

## 2. 源码理解证明
至少定位并说明以下子系统的真实源码位置、主要职责和互相调用关系：
- `BattleClient`
- `StrategyDirector`
- `LocalArmyDirector`
- `LocalCrisisPolicy`
- `SurplusSpendingPolicy`
- `CommandArbiter`
- `BootstrapClient`
并进一步给出一张“现有生产通路”表，至少覆盖：普通陆厂补员、重炮余钱出口、builder recovery、工程师→amphibiousJet、T1→T2→T3 矿升级、新工厂建设。每条写清生产者、触发入口、资金/预留约束、ready/交付证据，以及当前缺失环节。

还要核对并报告当前命令吞吐限制，不允许只写“有 APM 限制”：至少确认全局 command gate、LocalArmy cohort cadence、公平调度 lane 与 LocalCrisis 的关系，并指出哪些诊断字段目前存在已知统计问题。

## 3. 测试与原生环境证明
先记录：`git rev-parse HEAD`、`git status`、`java -version`、`python --version`、操作系统。

必须实际验证 GitHub 自带的 headless 资源：
```text
python tools/prepare_headless_engine.py --out .engine/rw115
```
确认 manifest 校验成功，并记录 `game-lib.jar` SHA256；期望身份为：
`8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`。

然后至少完成两类执行证明：
1. 使用当前平台适用的标准回归入口（Linux 优先 `bash agent/test.sh .engine/rw115/game-lib.jar .engine/rw115/libs`；若环境不同，说明等价入口），保留实际 pass/fail/skip 数，不用历史数字替代本次运行。
2. 用固定 FEEDBACK-v1 JAR 运行一次最小 headless smoke：
```text
python tools/run_headless.py --game-dir .engine/rw115 --agent-jar deliveries/feedback-2026-10-01/rw-agent-bootstrap.jar --mode smoke --speed 4 --timeout 120 --out headless-runs/orientation-smoke
```
如果 smoke 入口因平台/依赖失败，必须给出原始失败点和最小修复需求，不能写成通过。

## 4. GitHub 写回能力证明
从最新 main 创建：`astra/orientation-20261001`。
只允许新增：`astra-checkins/ORIENTATION_REPORT_2026-10-01.md`。
报告必须包含上述阅读、源码、测试、headless、环境与矛盾检查结果。
提交并 push 该分支；给出 branch、commit SHA、remote 状态。不要直接改 main，不 force-push。
## 5. 第一轮最终回答格式
最后只给出一个清晰资格结论：`PASS` / `PARTIAL` / `FAIL`。

必须附：
- 你读到的 main HEAD、candidate、baseline；
- 回归与 headless smoke 的实际结果；
- GitHub 写回 branch + commit SHA；
- 发现的文档/代码/证据矛盾；
- 第二轮施工前仍需解决的 blocker（若无，明确写 none）；
- 你认为 Production Capacity Controller v1 的最小源码修改面，但本轮不要实现。

## 通过标准
只有以下全部成立才算 PASS：
- 项目身份与最新桌面事实无明显误读；
- 能把主要子系统和生产通路映射到真实源码；
- headless engine 成功准备并通过 manifest / SHA 身份检查；
- 至少实际运行标准回归和一次固定候选 headless smoke，并诚实报告结果；
- 能在独立分支创建、commit、push orientation report；
- 没有修改生产源码或伪造未取得的自然对局/胜率结论。

本轮的价值是消除第二轮的环境、权限、知识和测试不确定性。第二轮应该能够直接从 `project-state/NEXT_STAGE_PLAN.md` 的第一主施工项开始。
> 注意：若 Astra 自身平台不支持某条命令语法，可使用语义等价命令，但必须保留同样的身份校验、实际执行和失败披露要求。不得用历史 CI/旧日志替代本轮真实执行。