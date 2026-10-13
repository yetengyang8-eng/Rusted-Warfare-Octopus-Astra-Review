# HANDOFF 对话30（DeepSeek → ChatGPT）

本轮主题：把 Astra 对"白名单口径 vs 实现"的审查意见施工到位，并处理 输出29 指出的状态漂移。
本轮没有实机运行；所有结论都来自可复跑的回归与探针。

## 1. 状态（可直接核对）

- 候选（安装件 == 回归产物 == 当前源码内容）：

```text
P1F-冒烟环境\rw-agent-bootstrap.jar
  whole-file 6e001729cb60fef13c7d30a82f54545f8fa444f2a665cd9743154e2c7b9c4de1
  contentDigest c7a6d423551f5a07a92e5beb72133a54ef8cc3407ca24136058117ff07118ac6
```

- 冻结交付物本轮重新核过，未改动：环境根 `rw-agent-bootstrap.jar` = `0681b4f7…d4a9eb1`；`game-lib.jar` = `8a550a37…620deec9`。
- 回归：`developer\test-win.ps1` → Java harness **17** 项 + Python 9 套，**failed steps = 0**。
  - `test_battle_client`：38 → **42** tests（本轮新增 4 个报告契约用例）。
  - `ReachabilityHarness`：**121** checks（live-safe）/ **127** checks（sandbox）。
- 状态漂移已修：根 `CURRENT_STATE.md` 不再写 `302f0c95…`，改为当前候选，并写明"whole-file SHA 不是候选身份、contentDigest 才是"。

## 2. Astra 审查意见的处理结果（对话29 §5）

Astra 说的属实：我写的口径是 "owner + name + descriptor 匹配"，实现里却只按 `owner#name` 匹配，descriptor 仅作说明文字，而且沿父类链无限制匹配。

最终契约（已施工、已有回归用例）：

1. 白名单 = `owner#name -> { 真实参数化 descriptor -> 用途 }`；
2. `isAllowlisted()` 用 `parametricDescriptor(Method)`（参数类型 + 返回类型）**精确比较**；
3. 沿具体类父类链匹配，但**只有本身已在白名单里的 owner 才能放行**；
4. 被拒绝的调用计入 `reflectionGuards.notAllowedSkipped`，签名进 `notAllowedSignatures`。

### 2.1 我自己在第 1 版修复里制造的回归（已修复，请重点复核这一条）

第 1 版按"宁可少放行"把父类链匹配整条删除，结果 **movement-class 路径被静默关闭**：

```text
attacker.getClass().getMethod("h")  declaringClass = com.corrodinggames.rts.game.units.e.j
descriptor = ([])->com.corrodinggames.rts.game.units.ao
第 1 版：attackerMovementClass = null , attackerMovementClassSource = FIELD_SCAN_FALLBACK
第 2 版：attackerMovementClass = "LAND" , attackerMovementClassSource = y.h() via y#h
```

教训：这里的"保守"不是安全方向，而是功能回归——放行判断必须继续认已审计的祖先 owner。

### 2.2 新增的回归用例（`ReachabilityHarness`）

- 遍历真实白名单 × 真实 `getDeclaredMethods()`：4 条已审计签名全部放行；`every non-audited same-name overload is refused (8/8)`。
- 具名反向对照：`am#cj`（void，方法体 `cu=-1`）、`ar#a`（工厂访问器）、`k.l#b`（未审计 owner）全部被拒。

## 3. 本轮另外查出并修掉的两个缺陷

1. **`reflectionGuards` 的计数是"半成品"**（Astra 契约缺口的延伸）：`notAllowedSkipped` 原本只在 `stages=allowlist` 出现；补进主 payload 后我又发现它**序列化在同一响应内那些拒绝调用之前**，于是自报 0 却在自己的 body 里写着 `SKIPPED_NOT_ALLOWLISTED`。已把 guard 汇总移到 payload 末尾，并加用例 `the refusal count covers every refusal in this response (62)`。
2. **`_analysis/evidence_index.py` 解析自己写出的索引时没有去掉反引号**，导致 sha 永不相等、所有历史证据都被标成 UPDATED，delta manifest 一次列出 **48 个文件要求上传**——正是"手工挑文件出错"这个协议要防的事。修复后本轮 SEND = **4 个文件**。这条属于工具链缺陷，请一并确认。

## 4. 报告侧契约（Astra 第 2 项：report contract vs implementation）

`test_battle_client.py` 此前**完全没有** reachability 覆盖。本轮新增 `ReachabilityReportTests`（4 个用例，驱动真实 BattleClient 打假桥接，再读 JSONL 报告）：

- 端点发布的 `reflectionGuards.{notAllowedSkipped, counterScope, notAllowedSignatures}` 原样进入 `report_reachability.raw`；
- `collisionRadiusRaw.readVia == "Field.get"`（报告里不存在方法读路径）；
- **客户端从不发送 `groups=` / `stages=` / `incident=`**；只有 `dump=full` 这一个已记录的 opt-in，且每个代表兵种只取一次；
- 每批 representative 数 ≤ 3（`reachabilityTypeLimit`），`representativeIndex` 从 0 连续编号。

## 5. 需要你（ChatGPT / Astra）裁决或复核的

1. **上一版 `live_incident_report.md` 里那句 "live-safe Group A 的唯一口径是 Field.get only / METHOD CALLS = none" 是错的，我已作废并改写**（正确口径 `{ r(), i(), bI(), cW() }`，均为桥接代码里的直接 Java 调用，不经反射）。请确认这个更正与你的理解一致。
2. **第 1 版白名单修复制造的回归**：请复核"父类链只认已审计 owner"是否满足你要求的安全边界（我理解它把可达范围仍然限制在已审计 owner 的签名集合内，不引入新的类）。
3. **短 live smoke 现在能否执行**：候选已就绪（contentDigest `c7a6d423…`），验收清单 ①～⑥ 已写进 `CURRENT_STATE.md`。是否执行仍按用户安排。
4. **Astra 工作包剩余项**（只读，随时可做）：(3) 冻结语义 vs 测试覆盖对照；(4) 全 JAR 零参 void / 工厂方法普查；(5) 独立复算事故证据链。第 (1)(2) 项本轮已由我施工+用例覆盖，可由 Astra 复核而不是重做。

## 6. 精确路径（你有只读访问，不必打包）

```text
G:\deepseek 工作台\CURRENT_STATE.md
G:\deepseek 工作台\助手交接\HANDOFF_对话30.md
G:\deepseek 工作台\助手交接\evidence\allowlist_verification.txt   ← 本轮核心证据（可复跑生成）
G:\deepseek 工作台\助手交接\evidence\candidate_sha_lineage.json
G:\deepseek 工作台\助手交接\evidence\CURRENT_STATE.md
G:\deepseek 工作台\助手交接\evidence\live_incident_report.md      ← 措辞更正版
G:\deepseek 工作台\助手交接\evidence\EVIDENCE_INDEX.md
G:\deepseek 工作台\_analysis\allowlist_verification.py            ← 生成上面第一份证据
G:\deepseek 工作台\_analysis\sha_lineage.py
G:\deepseek 工作台\_analysis\evidence_index.py
G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\src\io\rwagent\bootstrap\CombatBridge.java
G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\tests\ReachabilityHarness.java
G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\tests\test_battle_client.py
```
