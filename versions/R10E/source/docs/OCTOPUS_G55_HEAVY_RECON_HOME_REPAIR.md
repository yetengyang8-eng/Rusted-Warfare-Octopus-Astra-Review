# G5.5 R6：重坦、开局侦察与本土修复

这是 G5 的运营修正，R6 是候选轮次，**不是 G6 Capability Search**。不增加全局操作额度、决策频率、新 General 类型或终局搜索。

## 普通生产与技术

G5 默认 `rwagent.allowSmallTanks=false`。普通购买、preferred/fallback 诊断及产能统计排除 `tank/c_tank`；地图/原生命令、原版单位和旧兵种兼容性不变。显式 `-Drwagent.allowSmallTanks=true` 可用于以后独立实验，当前人工入口不设置它。关闭 G5 的历史兼容模式默认仍允许小坦。

第一次没有已知 ready scout 时，在现有 landFactory 当前合法菜单选择一个 scout，保留其它已选支出、军事 slot 和原生 guard。接受后标记已下单，不在同轮多厂重复；receipt 不表示单位已生产完成。若进程接手时己方已有 ready scout，则不重复开局购买。该进程不会因 scout 缺席而推断死亡或重复补一只开局 scout。当前合法空中威胁产生的普通反空补充仍独立有效，可以额外造 scout。

无允许的普通产品、且当前有合法 upgrade 的 T1 厂，复用现有**单个 investment** 选中实际原生升级报价筹资；保留实际 factory/action/source observation 与 request path。其它购买保护这张已选账单，升级使用自己的承诺时扣回本项储备，防止双重保护。此阶段不等待六名普通兵或“全军恢复完毕”，也不编造尚未出现在 T2 菜单里的重坦价格。开局 scout 已下单后，不再次为同一只 scout 储蓄。quote 失效、超时或接受升级后释放这项筹资；接受后资金转入原有 native credit hold，后帧 queue/tier witness 仍负责效果确认。矿完工只释放对应 NEW_MINE investment，不能顺手清掉技术承诺。

T2 厂实际取得 heavyTank 菜单后，沿原有持续生产链购买；钱不足时不会再退回小坦消耗剩余资金。`armyTarget` 继续只组织兵力。没有固定重坦数量、建造顺序、地图路线或“高现金违规”检查器。

## 开局 scout 与 Recon

第一只实际观察到的 ready scout 先进入原有 FREE，再在 Commander 补员前交给独立 `recon:0` 待命租约。执行具体 frontier 时转到既有 `recon:N`，真实 generation 更新；结束释放后可再保留给 Recon，不混入首支 General 的成军人数。

它复用已有 `/scout/plan?role=recon`、合法 tile route、当前/记忆危险走廊检查、后帧移动与探图 witness。仅对这只开局 scout 去掉“先有六兵/先成为残血小坦才可侦察”的旧门槛。仍可因本土紧急情况、当前危险走廊、严重受损、路线失效撤回；侦察用 MOVE，不能因为发现 ghost 发起攻击。是否长期存活要看自然局和真人，不能由 fixture 的位置修改证明。

## 修复施工

已有恢复需求先选距离己方基地较近的空闲施工者。施工者离 home 超过180 world 时，走现有 RETURN/MOVE 到本土；一次服务请求仅调回一个施工者，不把扩张 Builder 全部拉回。后帧位置到达后重新读当前 repairbay construction-plan。接受的站点必须在 home 650 world 内，并通过原有当前可见威胁、保守记忆风险和 native footprint/construct guard。650 是本土施工范围约束，不是“区域已安全”的断言。

修复候选先于空闲 Builder 接新的普通 prospect，因此不会一边声称要本土修复、一边先把同一闲置工人发到远端。已有 BUILD/PROSPECT/Capability 的忙碌工人仍由原任务管理，不增加新经济大脑。修复本身继续真实支出/施工/ready/HP witness；没有合法本土站点时不改为远端建造，也不宣称治疗条件成立。

## 基地威胁

未知战斗资料时，武装建筑 HP 的 exposure 系数由2降到0.5：这只是减少建筑血量对保守代理的夸大，不是修改原版火力或声称得到真实 DPS。已知 CombatProfiles/FightEstimate 的原版基地射程、伤害、重坦对空兼容性不变。严重实际交换恶化仍使用 MOVE 撤离。

记忆 Base Threat Zone 继续保护小队的普通穿越/探图。若某个当前合法可见目标路径被基地圈阻挡，另外评估**阻挡基地本身**的当前交换；只有该基地资料已知且交换可承受，才允许具体进攻通过。不能用基地旁一只易杀单位的短 TTK 来证明基地可攻，不能用 LOST_CONTACT 为强攻授权。

## 验证边界

`G55OperationsHarness` 覆盖默认禁用与显式恢复、无允许 replacement 的真实升级预算、技术储备释放、scout lease/generation、本土回程与后帧施工、六重坦当前基地进攻与失联边界。它是合成输入，位置到达不算自然运动。

`G55NativeOperationsHarness` 使用原版菜单、HTTP/native command.k，证明 T1 首只 scout、同帧不重复、已有 scout 后升级、T2 多厂重坦与 queue witness。显式 seed/0 ticks，不证明自然生产完成或胜利。最终 Spain 自然局与完整 Windows 结果见本轮 handoff；人工验收独立进行。
