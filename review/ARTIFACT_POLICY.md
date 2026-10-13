# 数据完整性、依赖与再现性说明

本审查仓库于2026-10-13创建并扩充，保存八个冻结源码版本、八套对应 Agent JAR/运行工具、原生验证证据、完整相关本机 run 战斗 trace、真人1v9回放、地图、原始实战遥测和GUI逐单位采样。文件级目录、大小和位置见 [全部证据清单](FULL_EVIDENCE_INDEX.md)。

## 文件实际存储方式

- 普通 Git：全部源文件、handoff、关键 JSON 测试摘要、Agent 小型冻结 JAR、恢复脚本与版本补丁。
- Git LFS：巨大原始 battle `.jsonl.gz`、validation `.zip`、原始 `.replay`、未完成 `.partial` 文件等。**下载后需执行 `git lfs pull`**；GitHub 网页看到的指针不是实际数据内容。
- 每份 validation 压缩包汇总对应阶段的原生测试、trace 与自然运行报告，去掉了商业游戏本体、第三方素材与重复的构建缓存。部分测试文件仅作为历史失败记录，不能全数算最终绿色测试。
- 保留明显标注的中断日志和失败证据，不删除或美化，不将 INCONCLUSIVE/PARTIAL/ONGOING 当作原生 VICTORY。

## 不可转存的外部依赖

没有未经许可地上传铁锈战争商业游戏本体 `game-lib.jar`、游戏资产、JVM、第三方字体、系统 DLL、以及私密账号/密钥。它们仍在原始本机8套完整独立环境中。每份 `VERSION.json` 包含原环境路径和 Agent SHA256；[恢复脚本](../scripts/restore_local_kits.ps1)只复制用户本机合法完整 Kit 并校验冻结 Agent，不修改原游戏文件。

原 Windows Kit：`G:\deepseek 工作台\游戏环境\RW-Agent-Octopus-G6-...`；R15/R16重点自然 raw 来源 `G:\deepseek 工作台\_validation\octopus-r15-air-clearance-20261012\` 和 `G:\deepseek 工作台\_validation\octopus-r16-ground-20261012\`。凡未收入的私有系统依赖在本机均可追溯，无需重复搜索原工程目录。

## 证据等级

- 代码来源：精确 Git commit，可独立 diff。
- 成品来源：Agent JAR SHA256 + KIT_MANIFEST + 原 Windows kit 端口。
- 执行事实：Native replay/bridge 原生日志、后帧见证、允许的对局终局。
- 推断与假设：独立标记，不得把意图、命令accepted、后帧位置或区域清空与最后一击混淆。

尤其 R15 唯一已归档自主普通 Spain 原生胜利 ≠ 稳定胜率；R16同参数自然对照的未胜利 ≠ 已确认归因；R14有人工介入不能算自主胜利。

## 访问权限

GitHub 仓库已按所有者要求改为 **public**，不再需要访问这个仓库的额外授权。任何 GitHub 插件/第三方审查工具若报告404，应先检查其对这个新私有仓库的 OAuth/App 授权，不能因为返回404误判仓库不存在。用户还可通过授权的 Desktop Commander 本地副本直接审查。
