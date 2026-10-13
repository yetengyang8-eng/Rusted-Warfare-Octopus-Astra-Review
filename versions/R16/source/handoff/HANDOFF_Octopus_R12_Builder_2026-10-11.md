# R12 Builder 施工闭环交接 — 2026-10-11

分支 `codex/octopus-g3-execution-20261002`；起点 `f4d81e8`。本轮只修 Builder，不改 General/Scout/Air/Repair 主循环，不提高命令额度，不 push/deploy，不操作用户桌面游戏。

## 真人事实与根因

输入 R11B `runs/20261011-023343-8804` 的冻结 gzip partial 前缀：79,943,862 bytes、346,532行、malformed0，SHA `9f955ddec85cae8a3a612861f40d8fd1847433ad95464995e6f1c48c73f384df`。这是部分日志，不是完整比赛。末次完整 own sample 在3427961ms：12Builder、5矿；46个曾观测 Builder ID不等于死亡/购买次数。

已识别 Builder 的命令记录：815 MOVE accepted、18建矿 accepted；未发现 Recon owner 接管这些 Builder。第二启动 `17624` 被端口锁拒绝，无两Agent争控证据。Builder36298原生订单为空，在后续采样中超过545游戏秒未出现大于2world位移，仅有22 MOVE、没有建矿。raw341654/341834仍提供5570/1710的当前合法报价700，距离266.507，native credits1079/1201。原生余额不等于有效可花资金；不能把全部闲置因果仅归给这一条记录。

源码与旧JAR复现确认：`resourceCommitted` 把600范围 regional preference、旧static tile当独占施工；附近工人可互相挡住不同矿点。Allocator保留了别人的已分配工作作为本地 backlog；资金链又要求到旧rally/旧cluster，即使实际位置已有合法本地工作。原生 endpoint只返回最近一座矿，遇到真实预约也无法选择第二座。

另有命令兼容风险：本地记账结束不代表 native build/repair结束；普通找矿 MOVE不能覆盖当前实际施工。这个保护由focused证明，不能宣称原局每条MOVE都曾取消施工。

## 提交与实现

- `7b9e4e5`：区域偏好不排他，预约只占具体工作点；真实本地矿quote独立筹资并施工，旧rally不否决当前工作；最近矿已分配时请求真实native替代报价；baseline与Strategy共用具体矿排除，owner/BuildJob独立；保留当前native build/repair；确认native idle后恢复丢失MOVE；静态approach暂时失败不永久污染候选集；补workerTrace。
- `a38b061`：旧R4 active-travel fixture补后帧MOVE witness，增加确认idle恢复和缺字段UNKNOWN负例。产品源码未变化。
- 最终文档/evidence提交见Git；不能reset/clean三个人为保留的launcher/backup输入。

`/expansion/plan`新增可选URL编码`excludeSites=x,y;x,y`。visibility先于资源格读取，native footprint/path/range/action/price及最终command guard保持权威。UNKNOWN不猜合法性；无计时退款，不放掉paid ghost。owner/generation/ABA、每actor冲突、unsettled credit/producer slot保持。更多见[Builder契约](../docs/OCTOPUS_R12_BUILDER_WORK.md)。

## 验证

新增Builder focused14、native7；另9套关联focused296checks通过，合计310个controller checks。R4校准后27checks。旧R11 JAR明确复现附近两个不同合法矿、足够合成预算却不能同时预约的失败。native fixture为simulationTicks0：两条不同矿命令进实际native queue，隐藏矿报价/施工拒绝；不是自然到位、扣款或建成证明。

第一次完整Windows为120/121，唯一失败旧R4将`orderType:null`误称active travel。失败log保留；修正显式后帧native MOVE，没有削弱active命令不重复的断言。最终完整Windows **121/121 PASS、86 Java、32 Python/506 tests、3 build，failed steps: 0、exit0**。证据根 `G:\deepseek 工作台\_validation\octopus-r12-builder-20261011`，最终回归在`corrected-full/`。

候选、Spain与人工kit使用同一JAR SHA `61d8a341b3fff0445e67978d7deb2a6d4e349589e683a1f35afc35c9317b98cf`。最终完整回归独立重建JAR SHA `c31b20d054fc1f840cbf76c24052d7d9d43b5fdcc1ff3a72db0c222b0c022ee7`；ZIP元数据不同，**210个可执行/资源entry逐个SHA一致**。不冒称两个归档完整SHA相同。279输入/ 1895kit文件零漂移，三个人工输入原SHA保持。

## Spain自然局

同产品JAR、原MP97地图SHA `342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746`，native Hard1/player0/5×，3600游戏秒预算；零普通军队、无资金/HP/位置/fog注入。raw闭合gzip310386行、malformed0，SHA `3351bd2201a1233d85a468c1de9ca94f034343e06e6211c971d87dd59e50447b`。

结果 **PARTIAL/ONGOING**，Agent exit2是预算到期语义，stderr无异常；不是native胜负或真人验收。结束checkpoint gameTime 3608112ms/frame225507，ready {'commandCenter': 1, 'landFactory': 4, 'extractorT2': 6, 'combatEngineer': 3, 'repairbay': 5, 'amphibiousJet': 7, 'heavyTank': 113, 'heavyArtillery': 6, 'builder': 8, 'extractorT1': 8, 'fabricatorT1': 1}，现金20712.0。

实际native建矿 accepted **62**，后帧新ready矿实体与目标位置匹配 **54**，raw own样本峰值ready矿16、Builder11。匹配是own实体/位置证据，不是对单个订单施工者的绝对因果归属；未匹配部分仍UNKNOWN，不能猜失败、退款或击杀。

替代报价链自然触发1次且有后帧ready：Builder19686排除3330/6610后，真实quote返回3590/6590；后来实体23552在1205648ms/frame75353 ready。`costSourceRequestPath=/expansion/plan?unitId=19686&excludeSites=3330.0%2C6610.0`；报价来源保留，不以/state冒充价格。自然局证明当地施工与继续扩矿，不证明原真人player5全部罢工已消失，不是受控AB。

## 人工入口与下一断点

`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-2026-10-11-R12`，port47699：用户`1-Start-Game.bat`→自己选Spain/条件并开始→`2-Run-Agent-7200.bat`。1200/2400可选；wall safety14400，预算到期不关游戏。默认控制台/`rw-agent-live.html`直读Bridge Game/Frame。只准备，未启动。见[人工说明](../docs/OCTOPUS_R12_HUMAN_VALIDATION.md)。

下一只看同原局条件的Builder：附近多工人是否分别落矿、当前位置真实合法报价到建成的延迟、native施工是否被普通MOVE覆盖。停工要分当前威胁、实际可花资金、真实预约与遗留区域偏好；不提高频率代替诊断。Builder存活/远端补缺、旧施工付款UNKNOWN、千单位晚期性能仍未在本轮解决；General/Scout/Air质量保持原断点。

[证据总表](../evidence/octopus-r12-builder-2026-10-11/validation-summary.json)；[真人紧凑证据](../evidence/octopus-r12-builder-2026-10-11/human-r11b-builder-prefix.json)；[自然Builder证据](../evidence/octopus-r12-builder-2026-10-11/natural-hard-spain-builders.json)。保留所有主要诊断失败；初始native fixture遗漏path-grid的第一次完整log曾被覆盖，summary明确记录，不当产品失败。
