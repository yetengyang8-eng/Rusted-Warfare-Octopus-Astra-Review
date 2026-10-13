# HANDOFF（对话29）—— Astra 审查发现的落差已修复；仍只请求批准短 live smoke

## 发生了什么

1. **P0 根因已判定并修复**（`AM_CJ_VOID_MUTATOR = CONFIRMED_ROOT_CAUSE_OF_DIAGNOSTIC_KILL`）：诊断把 `am` 的**字段 `cj`** 与**同名 `void cj()`** 混淆，后者字节码只有 `cu = -1`，而 `cu` 就是 HP。每次采样置死被探测对象。第二个独立缺陷 `ar.a()`（工厂访问器，registry +1）也已移除。
2. **Astra 的审查属实，我先认账**：我上一轮把白名单写进交接时称"按 owner + name + descriptor 匹配"，但实现里键只有 `owner#name`，descriptor 只当说明文字，匹配时不比较参数/返回类型，还允许沿父类链匹配。**契约没有兑现**——这与本次事故（"按名字调用"）属于同一类落差，必须立即修掉。
3. **已修复并加了运行时取证**：
   - 白名单结构改为 `owner#name -> { 真实参数化 descriptor -> 用途 }`；
   - 匹配用 `parametricDescriptor(Method)`（参数类型 + 返回类型）**精确比较**；
   - **取消父类链匹配**（安全方向：宁可少放行）；
   - 未放行的调用记录**观察到的真实 descriptor**（`stages=allowlist` 可查），供未来逐条审计后加入，而不是靠猜。
4. **运行时发现的具体后果（已如实记录）**：
   - `am.h() -> ao`、`am.r()/y.r() -> as`、`am.bI() -> boolean`、`am.cW() -> boolean` 在实机上与白名单完全一致 → 安全路径未受影响（`cu` 全程 210/210）；
   - `movementObject()` 的 fallback 扫描里 **8 个类型访问器全部未放行**，现在返回 `SKIPPED_NOT_ALLOWLISTED`；其中 `ar$53#b` 的真实返回类型是 **`void`**——本身就不该被诊断调用；
   - 因此 fallback 路径现在**结构性失效**（movement class 保持 null），首选的 `y.h()` 仍可用。这是设计意图：**扫描猜语义被彻底关掉**。
5. 回归：Java 17 harness + Python 9 套，failed = 0；`ReachabilityHarness` 103/107。
6. 新候选：`bf54e90ff74ed3930677a6875c8c86b62ca33ab69f72b06fd5e56b0dfb98809b`（替换 `302f0c95…`）。

## 为什么需要 ChatGPT

只需一个裁决：**放行短 live smoke 用哪个候选**。

- 我建议用本轮 `bf54e90f…`（严格 descriptor 白名单，fallback 扫描结构性关闭）；
- 若要变量最少也可先用 `302f0c95…`，但那版白名单比交接口径宽松，不推荐。

技术侧无未答问题；B/C/D/E 按《输出28》归 Diagnostic Lab，不再阻塞。

## Astra 的其余意见与我的处置

| Astra 意见 | 我的处置 |
| --- | --- |
| 诊断工具反成事故源；诊断不能无限占据主线 | 已在 `CURRENT_STATE.md` 落实 Mainline / Diagnostic Lab 分轨；本事故按 Closure Criteria 收尾 |
| 局部正确 ≠ 整局有效（41 单位围着海军工厂 51 秒） | 目标无进展处理机制已在 backlog；建议作为 P0 关闭后的**第一项 Mainline 功能**，我会给最小方案 + 明确结束条件 |
| 交接过于细碎 | 已采纳 `CHATGPT_HANDOFF.zip` 单包 + 证据等级 E0–E4 + GREEN/YELLOW/RED 三档权限 |
| 最新 developer 目录未提供 | 可提供：`游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\`（源码/测试/构建脚本），需要时打包 |
| 无进展机制优先于继续加兵 | 同意；cap=40 暂固定，不升 48 |
| 评价从"功能是否触发"转向实效（无效追击时长、恢复时长、闲置资金、胜/败/超时分布） | 同意，已记入 backlog 的评价口径 |

## 新增证据 ID

| evidenceId | filename | 状态 |
| --- | --- | --- |
| 见 `EVIDENCE_INDEX.md` | `allowlist_verification.txt` | UPDATED（严格匹配后的运行时取证） |
| 见 `EVIDENCE_INDEX.md` | `candidate_sha_lineage.json` | UPDATED（新候选 `bf54e90f…`） |
| 见 `EVIDENCE_INDEX.md` | `CURRENT_STATE.md` | UPDATED |

## 用户操作（若批准）

```text
1. RW-Agent-Start.bat
2. 巨岛 1v1：非常困难 AI / 1x / 正常迷雾（cap=40 为默认）
3. 标准开局后 RW-Agent-Match.bat（不需要参数）
4. 打到出现 3～5 次 reachability sample，重点确认：没有"没挨打就死"、没有异常死亡音效
5. 在游戏内正常退出该局，等 Match 控制台自行收尾
6. RW-Agent-Collect-Reports.bat，把 ZIP 给我
```

回来后我只看五件事：① 不再出现 first representative 采样后约 0.5s 异常死亡 ② target 不再因一次诊断失去 live-unit 状态 ③ 无异常死亡音效/瞬死 ④ registry 无异常增长 ⑤ 报告里不出现被诊断单位 `cu=-1`。
