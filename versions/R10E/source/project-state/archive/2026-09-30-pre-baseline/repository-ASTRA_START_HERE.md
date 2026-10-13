# Astra 中转入口

本仓库是 `G:\deepseek 工作台` 的公开工程中转镜像，供无法访问用户本机文件系统的 Astra / Codex / DeepSeek 使用。

## 当前任务与候选

本轮正式任务为 `ASTRA_BREAKTHROUGH_MISSION_GLOBAL_STRATEGY_2026-09-30.md`。Astra 已交付 Global Strategy 最终候选，源码 commit `2f310ec`；最终 JAR SHA256 `76711a8ee2716af5d5b0b66d5c53b159ae28f4f744dcbb24cae492d96f0342ed`。

建议阅读顺序：
1. `project-state/CURRENT_STATE.md` 顶部最新状态。
2. `handoff/HANDOFF_Astra_GlobalStrategy_2026-09-30.md`，了解最终设计、候选身份和交付时原生验证。
3. `ASTRA_FEEDBACK_GLOBAL_STRATEGY_DESKTOP_2026-09-30.md`，了解最终候选随后取得的 Windows 桌面反馈。
4. `evidence/global-strategy-desktop-2026-09-30/README.md`，展开三场 5x / 2400 秒桌面长局、专项审计和压缩 raw reports。
5. `knowledge/README.md` 及其索引的 baseline / conflicts / targeting / movement / terrain；知识不能覆盖最新动态证据。
6. 本轮输入 E4：`evidence/astra-target-stability-desktop-2026-09-30/`。上一轮 Recon 桌面验收保留在原目录。

当前源码候选已增加目标接近位置证据、能力需求、工程师任务和容量/投资分配。交付时原生验证见 `evidence/global-strategy-2026-09-30/`；随后相同最终 JAR 已进入独立 Windows 桌面环境并完成三场长局 E4。

## 最近桌面基线

- **Global Strategy 最终 JAR `76711a8e...`：三场 Windows 桌面 5x / 2400 秒 E4，1 场 Big Island + 2 场 400×370 西班牙 10p，均 PARTIAL/ONGOING。Spain 两局在 Match 启动前仅人工生产 1 个 builder，之后全程无人工干预。**
- Target Stability JAR `165bd3b2...` 已有 5x / 1800 秒桌面 E4；整体稳定，但暴露 seaFactory 的 LAND 接近能力缺口。
- 更早 Recon Execution `feb7ad39...` 有 1x Frontier 正验收；5x 那局没有建立 Frontier，不是其正例。
- 兼容 1.15 `game-lib.jar` SHA256：`8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`。
- 三场最新长局均没有原生终局，不能把它们写成胜率或因果性能结论。

## 现在可以自己跑原生 headless
`astra-relay/headless-engine-1.15.zip` 提供 `tools/run_headless.py` 真正需要的 `game-lib.jar + assets + libs + res`；无需 G: 盘。
先读 `astra-relay/HEADLESS_ENGINE_README.md`，再运行：
```text
python tools/prepare_headless_engine.py --out .engine/rw115
python tools/run_headless.py --game-dir .engine/rw115 --agent-jar YOUR_AGENT.jar --mode smoke --speed 4 --timeout 120 --out headless-runs/smoke
```

## 工程历史与外部资料
- `project-history/raw-dialogue/` — 用户明确授权公开的本项目原始 `对话N / 输出N / HANDOFF_对话N`；只作历史检索，不能覆盖最新证据。
- `knowledge/` — Knowledge Packet、静态核验与来源清单。
- `THIRD_PARTY_REFERENCES.md` — 公开第三方仓库及用途。
- `REPLAY_CORPUS_README.md` / `replay-corpus-manifest.json` — 本机九图真人 Replay 语料的存在、规模和边界；全量语料未镜像。

## 1.15 引擎身份
`astra-relay/HEADLESS_ENGINE_MANIFEST.json` 对 headless 包的文件逐个记录 SHA256。冻结 `game-lib.jar` 与公开 `TapeRTS/Tape/1.15/game-lib.jar` 身份一致。

## 不变的安全边界
战争迷雾、UNKNOWN、控制权限和证据等级继续执行；不要因获得完整静态资源而把未探索地图/隐藏动态敌情直接喂给策略。不要重新启用已隔离的危险反射探针。

## 直接回写
若 Astra 环境已经连接 GitHub 并获得本仓库写权限，按 `ASTRA_WRITEBACK.md` 在 `astra/<task>-YYYYMMDD` 分支提交，不直接覆盖稳定 main。没有写凭据时继续交付 patch/ZIP，由中转端落分支。
