# HANDOFF_FOR_CODEX：Rusted Warfare 1.15 规则型 Agent —— 接手须知（当前状态）

> 写给**第一次进入本工作区的模型**（Codex / Astra）。只写**当前真相**，不写历史。
> 历史与决策过程在 `助手交接\对话N.txt` / `输出N.txt` / `*_ChatGPT.md`；当前状态速查以
> 根目录 `CURRENT_STATE.md` 为准。
> 当前工程轮次：**KnowledgeBacked World Model / Capability v0 已完成完整回归、P1F 安装、E2 原生地形对照、一局隔离原生 `PASS / VICTORY`，并已有用户桌面多局自然运行证据（含一局 180x180 VICTORY 与 400x370/1800s 压力局）；仍未证明九图 1v1 因果性能增益，独立原生验收仍待补齐**。
> 最新交接：`助手交接\HANDOFF_Codex_KnowledgeBacked_v0_2026-09-29.md`；候选身份、合法视野、原生结果与未验证边界以该文件和 `CURRENT_STATE.md` 为准。下文保留策略与实机保护约束，旧 Recon 身份段落标为历史。

---

## 1. 这是什么项目

- **Rusted Warfare PC 1.15** 上的**规则型（无机器学习）**对战 Agent：Java 客户端
  `io.rwagent.client.BattleClient`，以 `-javaagent:rw-agent-bootstrap.jar` 注入游戏，
  通过本地 HTTP 桥 `127.0.0.1:47653` 读状态/下命令；游戏与客户端是**两个 JVM**，
  客户端单独启动（`java -cp rw-agent-bootstrap.jar io.rwagent.client.BattleClient <游戏秒>`）。
- **用户是唯一的桌面实机来源**：他手动开 `游戏环境\P1F-冒烟环境\RW-Agent-Start.bat` 启动游戏，
  再开 `RW-Agent-Match.bat` 跑一局（默认 900 游戏秒预算，cap=40），然后把手打的报告交给我们。
  **不要自己启动他的桌面游戏、不要改游戏设置、不要替他做策略决定。**
- 这条桌面约束仍适用；独立工作目录中的原生无画面验收已获授权。GPT 在 `助手交接/Codex_Recon_v0.2_加速推进合同_2026-09-28.md` 进一步授权本轮 Map Memory、残血单位转侦察与 FRONTIER_SWEEP；其他大型策略分支仍需交接。
- 目标不是"打得好看"，而是**可解释、可复算、可回归**的工程闭环：每条策略都要有证据与验收。

## 2. 当前候选（唯一真相）

| 项 | 值 |
| --- | --- |
| 内容身份 | `82438a7b373111dca88b38e7a1c2779527a9cef136cdb9a1297bdf4c511273a7` |
| P1F 安装件 = `developer/dist` = 原生 staged JAR | SHA256 `5741e241ce8ec1b921f36ed3274087609d0430f9281c44f6f62c096de84c5f54` |
| 谱系 | A/B 重建内容相同；`aVsInstalled=[]`、`aVsDist=[]`；`evidence/candidate_sha_lineage.json` |
| 完整回归 | 改前 34/34 步、改后 37/37 步，失败 0；`evidence/knowledge_backed_v0_regression.log` |
| 原生地形 E2 | 438,450 格值匹配，含非方图及隔离 PathingOverride 双向改写；`evidence/knowledge_backed_v0_terrain_e2.json` |
| 隔离原生局 | Big Island (2p)，难度 1，请求 4x，`PASS / VICTORY`，审计 `PASS`、`issues=[]`；`evidence/knowledge_backed_v0_native.json` |
| 冻结文件 | 环境根 Agent `0681b4f7…d4a9eb1`；游戏 `8a550a37…620deec9`，均未改 |

这轮 Target Guard 已进入目标候选及混编分配；地形记忆只取合法可见格，Recon 路线记录 movement 与 terrain 来源。旧 P1F JAR 备份为 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\p1f-before-knowledge-backed-v0-20260929.jar`，SHA256 `0c53bdfa1b545088e15fa6ab4269216b5c3fa586b00738bf00f5770ac5adc71a`。原生局没有自然触发 LAND/HOVER 同格差异和 PathingOverride，仍为 E2-only；胜利不证明性能因果改进。详见新交接。

新增的敌方动态域读取是合法视野双重检查后的**直接类型调用** `am.i()/am.Q()/am.cH()`；冻结 `game-lib.jar` 上核对 owner 分别为 `am`，descriptor 都是 `()Z`，其中 `i/Q` 为抽象读取、`cH` 为 final 读取。它们不走反射，也不用于迷雾旧实例刷新。下方“CURRENT_SAFE_PATH”记录的是此前 P0 事故后的旧调用集，仍作为事故边界保留。

### Recon v0.2 交付时记录（历史）

> 下表中“当前”“最终”只指 2026-09-27 Recon v0.2 交付当时；勿用其 SHA 识别现装件。

| 项 | 值 |
| --- | --- |
| contentDigest（**候选身份以此为准**） | `b292e168c3cabe71ba5bada3159d14d027d324cc7794d99fe80fba6ac396aa20`（完整值见 `助手交接\evidence\candidate_sha_lineage.json`） |
| 安装件（游戏实际加载） | `游戏环境\P1F-冒烟环境\rw-agent-bootstrap.jar` = whole-file `0c53bdfa1b545088e15fa6ab4269216b5c3fa586b00738bf00f5770ac5adc71a` |
| JDK 17 交付产物 | `游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\dist\rw-agent-bootstrap.jar`（与安装件同字节） |
| 一致性 | `aVsInstalled = []`、`aVsDist = []`（当前源码经 JDK 17 重建的内容 == 当前 dist == 安装件；不宣称 JDK 13 完整回归产物与交付 JAR 逐字节一致） |
| 回归 | Recon v0.2 改前完整基线 Java **17** + Python **13**，failed 0；最终完整 `developer\test-win.ps1` 使用项目 JDK 13：Java **17** + Python **14**，**failed 0**；`ScoutHarness` 160 checks、前沿客户端 **16/16**。原始日志 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v021-final-regression.log`。交付 JAR 随后用 JDK 17 构建并完成当前源码/`dist`/安装件内容谱系核对。自然原生局单独按原始报告判定。 |
| 最终候选隔离原生 | `work/recon-v021-native/run-20260927T184202-5237df/episode-001`，Big Island (2p)、难度 1、4x、1200 游戏秒预算；原始 battle SHA256 `d68a3083734e45a36a9942939d882e7fd4a102274d1cd25a8459a129a68cd38b`，staged JAR 与当前候选 whole SHA 一致。`助手交接/evidence/recon_v02_native_final.json` 审计 `PASS`、`issues=[]`：前沿任务 36、残血转岗 1、queued 30、observed 24、己方位置进展 24、团队合法视野区域重见 23、refreshed 23、完整转岗至刷新时间链 23（`NEVER_SEEN_EDGE` 14、`DEEP_FOG` 9）；193 次主力 `attack-move` 中 75 次有已转岗单位排除检查，无违规，前沿阻塞 12 次。episode `FAIL` 是预算到期分类，battle 为 `PARTIAL/ONGOING`，没有原生胜负；团队合法重见只有时间关联，不能归因于该侦察者。 |
| 冻结交付物（**永不修改**） | `游戏环境\Rusted-Warfare-1.15-Agent-0.07\rw-agent-bootstrap.jar` = `0681b4f7…`；`game-lib.jar` = `8a550a37…` |

首条完整原生时间链可从任务 #9 核对：`c_tank` #345 转岗时 HP 2.485/210；696192 毫秒任务创建，697616 毫秒转岗/queued，698992 毫秒己方 `move` 与位置进展，700432 毫秒后续全队合法视野刷新（原始战报行 4093、4110–4118、4124–4125）。这不能证明 #345 单独造成区域重见。36 个前沿任务中 23 刷新、12 阻塞、1 个在预算终点未结；阻塞原因为路线锚点漂移 9、已知威胁走廊 1、航点到达却无合法清空 2。任务 #24 的短航点 `move` 出现在原始己方观察里，但决策采样错过，是后续 Recon 活性风险。

最终安装前的旧 v0.2 候选备份为 `C:\Users\Administrator\Documents\Codex\2026-09-27\g-deepseek\work\recon-v021-before-final.jar`，whole SHA256 `e594770f7cf31494d5245c87a6dc5353a3b6cc4a83ae269ceed5ba4459ecac8f`；更早 v0.1 备份为 `work/recon-v02-before-2026-09-28/p1f-agent.jar`，SHA256 `9394bdc52b449400d3ec332bf23e959c1a0ce9a8c28d8bbddaf255f276432a52`。旧 v0.2 候选的三局 JDK 13 原生样本依次为 Small Island 难度 1 `PARTIAL/ONGOING`、Small Island 难度 0 `PASS/VICTORY`、Big Island 难度 1 `PARTIAL/ONGOING`；三局均没有自然 FRONTIER_SWEEP 任务或残血转岗，见 `助手交接/evidence/recon_v02_trigger_forensics.txt`。旧候选样本不并入最终候选验收。下一步优先根据以上阻塞与采样证据做更强 Recon 的风险与活性收尾；经济 vNext 与多人工作后置。没有原生胜负、桌面游戏或因果性能验证。

**候选身份口径**：whole-file SHA 每次重建都会变（zip 时间戳），所以引用候选一律写
**contentDigest**（对 jar 内每个 entry 名+内容 sha 排序后再 sha256，忽略 `META-INF/MANIFEST.MF`）。
`_analysis/sha_lineage.py` 产出探针 JSON，`_analysis/assemble_lineage.py` 组装成证据文件。

## 3. 绝对禁止（P0 / Diagnostic Lab 禁区）

**这些是已经付出过代价的结论，不要重新开启：**

1. **禁止在真实比赛对象上做任何未审计的反射调用**。历史事故 `0beaf504 = QUARANTINED`：
   端点曾用 `rawMethod(am.class,"cj")` 去"读碰撞半径字段"，但 `am` 上同时有 **字段 `cj`** 与
   **`public void cj()`**，按名解析命中的是后者，而它唯一的字节码就是 `cu = -1`（`cu` 就是 hp），
   **每次采样都把被探测对象当场杀死**（诊断杀死了被观测单位）。正式状态：
   `AM_CJ_VOID_MUTATOR = CONFIRMED_ROOT_CAUSE_OF_DIAGNOSTIC_KILL`。
2. 永久禁令：**不得调用 `aq.a(y,am)` / `aq.b(y,am)`、任何 `void` 方法、任何工厂访问器
   （如 `ar.a()`，每次调用都会新建并注册单位）、任何不在白名单内的反射方法**；
   **不得把 raw boolean 当作 targetable / reachable / engageable**。
3. CURRENT_SAFE_PATH 正式口径（冻结，勿改写）：`ENGINE_METHOD_CALLS = { r(), i(), bI(), cW() }`
   （桥接代码的直接 Java 调用，不经反射）；`FIELD_READS = cj/eo/ep/eh/bX/cu/cv + declared floats`；
   `REMOVED = am.cj(), ar.a()`。**永久废弃 "METHOD CALLS = none" 这种说法**。
4. **不要重新开启已归档的事故调查**，也不要为了"更精确"去扩大 live 反射面；
   需要新反射面时必须先走白名单 + 独立审计（见 `助手交接\evidence\allowlist_verification.txt`）。
5. **不要碰**：`game-lib.jar`、环境根那份 `rw-agent-bootstrap.jar`；不要改游戏文件、不要改用户的地图/难度。
6. **不要擅自开启其他策略大分支**（跨海/跨域、克制链、经济 vNext 等）——
   GPT 的 `Codex_Recon_v0.2_加速推进合同_2026-09-28.md` 已授权本轮 Map Memory、EXPENDABLE_SCOUT、FRONTIER_SWEEP 并要求保留 `RECHECK_INTEL`；授权范围外的战术、经济或兵种角色语义仍须交接裁决。

## 4. 关键路径

```
游戏环境\Rusted-Warfare-1.15-Agent-0.07\
  developer\src\io\rwagent\client\BattleClient.java      ← 全部对战逻辑（~2300 行，唯一主文件）
  developer\tests\test_battle_client.py                  ← 78 个客户端用例（HTTP 夹具 + 真实 JVM）
  developer\tests\test_headless_parallel.py              ← 22 个双实例编排 / A/B profile / 回收用例
  developer\tests\test_ab_aggregate.py                   ← 17 个只读聚合与身份拒合用例
  developer\tests\test_ab_campaign.py                    ← 14 个续跑/磁盘/缓存/故障分类用例
  developer\tests\test_recon_client.py                   ← 7 个 Recon 客户端确定性用例
  tools\run_headless.py / tools\profiles\cap32_vs40.json    ← A/B 双实例运行器与白名单示例
  tools\aggregate_ab.py                                  ← 跨批只读聚合器
  tools\run_ab_campaign.py                                ← 可续跑的 N 批 AB/BA 编排器
  developer\test-win.ps1                                 ← 唯一回归入口
  developer\MANIFEST.MF                                  ← jar 清单
  docs\BATTLE_CN.md                                      ← 行为/边界说明（每个特性一节）
游戏环境\P1F-冒烟环境\
  rw-agent-bootstrap.jar                                 ← 安装件（游戏实际加载）
  RW-Agent-Start.bat / RW-Agent-Match.bat / RW-Agent-Health.bat   ← 用户入口（别自己跑）
  rw-agent-reports\battle-*.jsonl                        ← 每局一份原始报告（分析都从这里读）
  report-bundles\                                        ← 用户交上来的 zip（分卷 p1of5…）
助手交接\                                                ← 与 ChatGPT/用户的交接区
  evidence\CURRENT_STATE.md                              ← 当前状态速查（唯一真相）
  evidence\EVIDENCE_INDEX.md                             ← 证据索引（E01…，含 sha256 与状态）
  evidence\candidate_sha_lineage.json                    ← 谱系 + 已校准经济常数
  ../../CURRENT_STATE.md                                 ← 上面那份的根目录副本（跟着更新）
_analysis\                                               ← 只读分析脚本（全部环境变量驱动）
  sha_lineage.py / assemble_lineage.py / evidence_index.py / build_handoff_zip.py
  two_speed_economy_builder.py / q1_demand_active_proxy.py / v1b_live_acceptance.py
```

## 5. 构建 / 回归 / 安装（照抄即可）

当前 KnowledgeBacked v0 的 JAR 必须包含 `developer/resources/knowledge/UNIT_CATALOG_CANDIDATES.json`。`developer/test-win.ps1` 已处理此资源；手工打包也须带上 resources，随后按当前交接核对 catalog SHA、contentDigest 与 P1F whole-file SHA。

```powershell
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:PYTHONIOENCODING='utf-8'
$root='G:\deepseek 工作台\游戏环境\Rusted-Warfare-1.15-Agent-0.07'

# 1) 编译（源码是 UTF-8，中文注释很多；不要用 -encoding 以外的编码）
javac --release 8 -encoding UTF-8 -nowarn -cp "$root\game-lib.jar" -d "$root\developer\build\classes" `
  (Get-ChildItem "$root\developer\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
# 2) 打包
java -m jdk.jartool/sun.tools.jar.Main --create --file "$root\developer\dist\rw-agent-bootstrap.jar" `
  --manifest "$root\developer\MANIFEST.MF" -C "$root\developer\build\classes" . `
  -C "$root\developer\resources" .
# 3) 回归（唯一入口；**不要同时跑两份**，它们会抢 dist 与端口）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$root\developer\test-win.ps1" "$root\game-lib.jar" "$root\libs"
# 4) 安装 + 谱系
Copy-Item "$root\developer\dist\rw-agent-bootstrap.jar" "G:\deepseek 工作台\游戏环境\P1F-冒烟环境\rw-agent-bootstrap.jar" -Force
python "G:\deepseek 工作台\_analysis\sha_lineage.py"; python "G:\deepseek 工作台\_analysis\assemble_lineage.py"
```

单跑一个测试类（调试时更快）：

```powershell
Set-Location "$root\developer"
python tests\test_battle_client.py "$root\developer\dist\rw-agent-bootstrap.jar" FactoryTargetIncreaseTests
```

## 6. 环境陷阱（都是踩过的坑，照做能省几小时）

0. **本机没有 PowerShell 7**：`pwsh` 不存在，命令行就是 **Windows PowerShell 5.1**（`$PSVersionTable` 自证）。
   两个直接后果，2026-09-27 各踩过一次：
   - `.ps1` 文件里只要出现中文（例如工作台路径），**必须存成 UTF-8 with BOM**；否则 5.1 按 GBK 读，
     中文字符串与路径全部乱掉（表现为"脚本跑了但路径找不到"）。
   - 跨进程传含空格的中文路径要**自己加引号**：`Start-Process -ArgumentList @('a',$path)` 不会给 `$path` 加引号，
     `G:\deepseek 工作台\...` 会被拆成两个参数。用 `-ArgumentList '... "{0}" ...' -f $path`。
   - 反向的坑：**Python 源码必须 UTF-8 无 BOM**（BOM 会让 javac 报错），
     而 Python 读 PowerShell 写出的 JSON/日志要用 `utf-8-sig`；
     PowerShell 5.1 的 `>` 重定向默认写 **UTF-16LE**，读这种日志要先按 BOM 判断编码。
1. **中文路径 + PowerShell**：`cmd /c "... > 文件 2>&1"` 比 PowerShell 重定向稳；
   `Select-Object -First N` 截断原生命令输出会让 `$LASTEXITCODE` 变 1（假失败）。
2. **不要在中文串里用 ASCII 双引号**：写 Python 源码时 `add("…"满载"…")` 会直接 `SyntaxError`
   （本项目已踩 3 次）。中文引号用 `「」`。生成脚本再 `ast.parse` 自检一次。
3. **Python 源文件必须 UTF-8 无 BOM**；`Set-Content -Encoding UTF8` 会带 BOM 并让 javac 报错，
   用 `[System.IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))`。
4. PowerShell 没有 here-doc：长脚本先用 `write` 工具落盘再执行。
5. `JAVA_TOOL_OPTIONS` / `PYTHONIOENCODING` 不设，Python 测试会因 `UnicodeDecodeError` 假失败。
6. **别同时跑两个回归**（dist jar 与端口冲突）；上一次被 kill 的 java/python 进程会占住输出文件，
   先 `Get-Process java,python` 看一眼再重跑。
7. 工具输出里渲染出的乱码**不代表源文件损坏**（曾误判 50 行注释被破坏，dry-run 证明是渲染问题）。

## 7. 读报告时的四个时间戳陷阱（血泪）

1. `gameTimeMs` 是**普通游戏秒 ×1000**（`gameSecondsPerWallSecond` 自证倍速）。
   历史上把"1 铁锈秒 = 0.66 秒"套到普通时间上算错过；0.66 只是经济循环口径（40 tick）。
2. **`extractor_completed` / `factory_completed` 没有 `gameTimeMs`**；它们带的 `firstSeenGameMs`
   是**工地第一次被看见（= 开工）**，真正完工要 +约 17.5 游戏秒。已在事件里补 `completedAtGameMs`。
3. `*_planned` / `production_facility_finished` / `production_idle` **没有 gameTimeMs**，
   只有 `wallTimeMs`；要落在游戏时间轴上得用 observation 序列插值。
4. `production_idle` 被限流（约 10 游戏秒一条），所以按它统计的"被卡时间"是**下界**。

## 8. 当前未决问题 / 下一步（写这份文件时的状态）

- **主线**：`Economy v1b`（动态陆厂）已 **DONE_AND_LIVE_VALIDATED**：巨岛 5x 局由 Agent 自己把
  `landFactoryTarget` 从 2 提到 3（触发时 army 19/40、满载 82.2%、剩余 +12.8/游戏秒、两个硬储备为 0），
  第 3 座厂建成后已接单生产消费率 46.0 → 74.7/游戏秒。冰岛 5x 局正确拒绝（满载 76.8% < 80%）并获胜。
  证据：`evidence\economy_v1b_live.txt`。
- **本轮收尾（已完成）**：Q1 只读复算（结论：**维持 saturationPct 与 80%**，不采纳 demand-active 代理，
  证据 `evidence\q1_demand_active_proxy.txt`）；Q2 **承诺语义**（动态提升的目标不再被单帧
  `FACTORY_QUEUE_EMPTY` 否决，基线第 2 厂路径未动）；Q3 saturationPct 改 2 位小数。
  证据 `evidence\economy_v1b_tail.txt`。
- **待 ChatGPT/用户裁决**：无阻塞项。已登记 backlog：
  `ABOVE_FLOOR_ECONOMIC_LOSS_REEVALUATION`（越基线矿被摧毁后是否重新评估投资）。
- **ChatGPT 已授权并完成的基础设施任务**：**2-instance parallel runner v0** —— 端口 / reportDir /
  session / lock / workingDir / 进程生命周期 / 失败清理 / 证据自证；限恰好 2 个独立无头实例。
  原生 smoke、suite、强杀 A 后 B 独立完成、干净重跑、安装后 smoke 和完整回归证据见
  `助手交接\evidence\parallel_runner_v0.txt`。4 实例与 self-play 尚未授权实施。
- **固定条件 A/B v0 已由 DeepSeek 独立验收 ACCEPT**：两个并行 match 可用白名单 profile 分别设 cap32/40；AB 与 BA 位置互换、候选/条件/原始报告身份、聚合拒绝语义已经独立核查。证据 `助手交接\evidence\ab_v0_ds_acceptance.txt`、`ab_comparison_v0.txt` / `.json` / `_runs.zip`；完整细节见 `助手交接\HANDOFF_对话47.md` 与 `HANDOFF_Codex_AB_v0_2026-09-27.md`。原始 n=2/臂只描述能力，不裁决策略。
- **A/B campaign v0 已由 DeepSeek 独立验收 ACCEPT（E2+E3）**：`tools/run_ab_campaign.py` 输入 N，串行 AB/BA、原子 `campaign.json`、缺口续跑、有效 PARTIAL 与异常编排分流、最终只读聚合及磁盘预算。每次最多两个独立引擎；Windows junction 仅指向 campaign 自有资源缓存。安装版两批各臂 valid2/partial2/native n0；强杀本次编排器与 runner 后，续跑按登记身份回收两引擎、保留旧 run 并补缺口。DeepSeek 复核及 WinError 5 有界重试见 `助手交接\HANDOFF_对话48.md` / `evidence\ab_campaign_v0_ds_acceptance.txt`。N、地图、统计与策略语义仍须交接裁决。
- **World Model v0.1 / Recon v0 历史候选**：GPT 新合同已授权窄范围 `RECHECK_INTEL`；桥接新增旧址 3×3 合法可见的 `CLEARED`，客户端单兵核查高价值失联建筑并保留原六人主力门槛。完整回归 17 Java + 13 Python、失败 0；正式隔离原生局 `PASS/VICTORY`，7 个任务、9 次 queued、7 个已观察 move、12 条位置进展、7 次合法重见（其中 4 次与已观察移动命令有时间先后）。自然 `recon_site_cleared` 未出现；不能把胜利或重见归因于 Recon。见 `助手交接\HANDOFF_Codex_WorldModel_v01_Recon_v0_2026-09-28.md` 和 `evidence\world_model_v01_recon.txt` / `_native.json`。
- **上一轮 World Model v0 只读战争记忆**：桥接层仅从已有合法可见敌人快照建立/刷新 `enemyIntel`，失联仅保留最后已知值，换局清空；客户端当时只记录计数、策略未读新字段。那轮回归 17 Java + 12 Python、失败 0；隔离原生 PARTIAL 局 29 心跳。历史材料见 `助手交接\HANDOFF_Codex_vNext_World_Model_v0_2026-09-27.md`、`evidence\world_model_v0.txt` / `world_model_v0_native.json` / `world_model_v0_review.zip`。实验原生 replay 从 frame 6452 起 checksum 不一致，已撤回实验代码，未交付回放能力；多人 Match/self-play 未实现。

## 9. 协作约定（请遵守）

1. **证据优先于叙述**：任何结论都要能落到 `battle-*.jsonl` + 一个可复跑脚本 + 一段可复算的数字。
2. 证据分级：E0 观察 / E1 单条日志 / E2 可复跑脚本 / E3 独立交叉核对 / E4 实机回归。
3. **改动源码必须**：跑完整回归 → 复制 dist 到冒烟环境 → 更新谱系与 `CURRENT_STATE.md` → 更新
   `EVIDENCE_INDEX.md` 与 `CHATGPT_HANDOFF.zip`（`_analysis/evidence_index.py` 生成 delta manifest，
   只列出本轮 NEW/UPDATED，别重复上传历史文件）。
4. **不要替用户决定策略方向**：涉及策略语义的改动先写进 `助手交接\对话N.txt` 等裁决。
   用户的固定要求：**冻结 JAR 不要动**、按既定顺序推进、遇到会改变设计的问题才停下来交接。
5. 反停滞规则：每 3~5 个 GREEN 迭代必须产出可见能力（A 行为 / B 规模 / C 可复算改善），
   只有日志/文档/字段增加不算。
6. 报告自证字段：`agentJarSha256`、`gameSecondsPerWallSecond`、`mobileUnitHardCap`、决策间隔、收入模型来源；
   **地图名与 AI 难度报告里没有字段**，属用户口述，不要推断。

## 10. 一行速查

```
源码： 游戏环境\Rusted-Warfare-1.15-Agent-0.07\developer\src\io\rwagent\client\BattleClient.java
回归： powershell -File ...\developer\test-win.ps1 <game-lib.jar> <libs>     期望 failed steps: 0
候选： contentDigest b292e168…（安装件 0c53bdfa…，完整值见 §2）        冻结： 0681b4f7… / 8a550a37…
状态： CURRENT_STATE.md                  证据： 助手交接\evidence\EVIDENCE_INDEX.md
```
