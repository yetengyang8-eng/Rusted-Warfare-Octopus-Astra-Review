# Octopus G5 真人西班牙局反馈（2026-10-03）

基线：`ca2fc92` / G5。真人 GUI 通过 Universal Bridge 与 `bridge-agent` 同局，地图 `[10p] 10p 西班牙混战_by_MP97.tmx`，LOS fog，4000，1x。

证据入口：
- Bridge raw：`G:\铁锈战争 桥 工作空间\headless-runs\human-g5-spain-20261003-01`
- Agent raw：`...\agent\rw-agent-reports\battle-1791014693923-d7f87169.jsonl`
- 真人 replay：`G:\deepseek 工作台\游戏环境\rustedwarfare PC 1.15 原版\replays\ 10p 西班牙混战 by MP97 [v1.15] (3 10月 2026 16.04.39).replay`
- replay metadata：`frames=98864,time=1581824,commandCount=6731,resyncCount=0`

## 结论先行
G5 已经越过“能不能像样打一局”的阶段。长图自然局证明：4 支 General 自然形成、独立推进与撤退成立，长期生产/扩矿成立，桥无 desync/resync。用户肉眼认为已有少量真人感。

用户自述自己在该图综合水平处于铁锈顶尖/最强级别，因此本局不是“平均人类强度”基准。用户主观估计：面对第一次见到它的普通人类对手，当前 G5 可能已经有较高实战威胁，甚至有机会击败约一半；**这不是测得胜率，不得写成量化结论。**

但当前策略高度可被重复对局利用：前期第一波常以固定方式压入敌方基地，容易白送；对空军抓击也缺乏足够适应。灵活的人类初级玩家见过几次后就可能针对这一点，导致后续运营完全失去用武之地。
## 已证明的正向能力
- 自然形成 General：G1 ACTIVE 1:34；G2 12:28；G3 16:28；G4 19:33。`ADDITIONAL_GENERAL_NATURAL_BIRTH` 可由 NEEDS_EVIDENCE 升为 PROVEN。
- 约 20:34 时峰值可见 force-controlled 单位约 83：4 支 General + FREE + Local Response，并且多路目标不同，不是同一团兵复制 owner。
- 生产：158 个新战斗单位；订单约 58 小坦克、101 重坦、1 重炮。会从 T1 坦克过渡到 T2 工厂和重坦。
- 经济：完成过 11 次新矿命令，后期约 10 个同时存在矿；8 次 T2 矿升级、3 次 T3 矿升级。
- 战斗：用户肉眼观察到会撤退；raw 中多支 General 独立进入 RETREATING/CRITICAL_RETREAT。两座敌方 Command Center 在 General 直接推进期间被合法视野确认 CLEARED，可支持“G5 自己攻掉了两座敌方主基地”，但不要把 CLEARED 写成确认玩家击杀。
- Bridge/native：G5 2819 条 action 全部 native queued，无 reject；replay `resyncCount=0`。

## 真人基准节奏（用户9号 vs G5 6号）
同口径按命令/入队列时刻：首厂约 0:12 vs 0:23；首坦 0:23 vs 0:41；第6坦约 0:24 vs 1:25；首矿 0:26 vs 1:00；第3矿 0:42 vs 2:33；首厂升级 1:32 vs 3:06；首重坦 2:23 vs 3:47；第二陆厂 2:39 vs 3:27。

差距不是“G5 不醒”——有效决策间隔约 384ms game time。主要是阶段转换规则慢：G5 实际出到约 10 辆小坦才第一次升级；矿线也明显晚。中后期又出现“收入增长 > 产能增长”：最高 credits 约 17648，但 `factoryTarget` 基本仍为 2，`factoryTargetIncreases=0`，扩厂被 block 约 21 次。
## 优先修的真实漏洞
1. **前期第一波可被稳定针对。** 当前 General 容易按固定方式压入敌方基地，缺乏对基地静态火力、局部兵力交换和空军截杀风险的充分判断。重复对局中可预测性很高；不要用纯随机路线掩盖，优先让当前合法可见威胁/目标价值/撤退成本真正改变进攻决策。
2. **对空能力缺口会让整套后期结构失效。** 若第一波地面军被空军抓净，后续多 General、经济和补员都没有表达机会。需要在合法可见空中威胁出现后产生能力需求/避战/撤退/补反空闭环，而不是继续默认地面 attack-move。
3. **建造者 prospect 缺乏局部承诺。** 5924号 builder 约 10:26 被派往北面，11:34 已到 `(7410,410)` 附近，随后 11:42 又被改派到南面 `(6330,3710)`，约 13:08 才在南面下矿。用户回放确认北面已有两个探过且较安全的空矿。这次折返单程就浪费约 1分50秒；按 T1 矿实测约 12.07 credits/s，若能顺手吃掉1~2矿，短期毛收入机会成本约 1300~2700 credits，长期更高。
   - 代码原因：`PROSPECT` 到点后直接 `IDLE`；重新按全局候选排序。Economy 同时因 `mines=4 > mineTarget=3` 给出 `NO_INVESTMENT_INTENT`，导致“已经跑到安全矿区”没有转化为“当地开矿”。
   - 期望：加入 local cluster / travel sunk-cost / prospect commitment。已到安全资源区后优先吃掉当地可达、低威胁空矿，再允许跨图重规划。

## 操作量：不要再追求更高 APM
到 G5 被淘汰约 25:49：用户 1389 rc，G5 2819 rc；command APM 约 **53.8 vs 109.2**。但用户每条命令平均涉及约 **8.0** 单位，中位3；G5约 **4.4**，中位1。单单位命令占比约 **34.9% vs 67.3%**；总单位引用量却只差约11%（11171 vs 12419）。

G5 命令结构：约 1470 move、1154 attack-move、0 direct attack；用户约 827 move、64 attack-move、127 direct attack。G5 的普通 move 基本都是单单位，attack-move 才承担编队控制。20~25分钟时 G5 APM 已接近196，但并没有对应的人类196 APM效率。

因此下一阶段的方向应是 **meaningful progress / command efficiency**，而不是继续提高决策频率：减少重复覆盖、稳定编组复用、允许合适的 direct target attack、已经有效的命令不必每轮刷新。

## 建议下一断点（不要求一次全部完成）
优先顺序：① 第一波生存与反可利用性；② 空中威胁适应；③ 建造者局部资源簇承诺；④ 收入→扩厂/升级的战略吞吐；⑤ 命令效率。保持 G5 已证明的多 General、撤退证据边界和合法 fog，不为修漏洞退回固定脚本。
