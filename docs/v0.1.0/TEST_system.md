# Girlfriends v0.1.0 底层系统测试与回归记录

## 自动化测试

| 模块 | 测试类 | 覆盖点 | 结果 |
| --- | --- | --- | --- |
| 持久化 | `GirlfriendsWorldDataTest` | 关系状态序列化与反序列化、dirty-aware 更新、数据版本兼容 | 通过 |
| 好感度 | `RelationshipServiceTest` | 裁剪、阶段推导、每日重置、亲密确认资格 | 通过 |
| 礼物 | `GiftServiceTest` | 档位、公式、每日上限、拒收、权限 seam | 通过 |
| 委托 | `QuestServiceTest` | 单槽、固定委托发布与接取、随机过期、前置链路校验 | 通过 |
| 绑定 | `BindingServiceTest` | 爱慕绑定、动摇期、亲密锁定、降级 stale binding 清理 | 通过 |
| 家园 | `HomeServiceTest` | 入住、收益、争执、重复回血语义 | 通过 |
| 祝福 | `BlessingServiceTest` | 启用条件、数值修正、概率、参数驱动、实体有效性 | 通过 |
| 死亡重生 | `CharacterRespawnServiceTest` | 死亡清零、奖励保留、庇护所记录 | 通过 |
| 网络摘要 | `InteractionSummaryServiceTest` | 模糊好感、按钮权限摘要、UI-safe 本地化键 | 通过 |
| 网络权限 | `ServerboundPermissionTest` | 越权赠礼不改变好感值 | 通过 |

## 手工验收

| 场景 | 步骤 | 预期 | 结果 |
| --- | --- | --- | --- |
| 单人关系 | 创建世界，与同一角色进行好感变化操作 | 仅该玩家该角色关系变化 | 待执行 |
| 多人竞争 | 两名玩家提升同一角色好感并触发动摇期 | 3 个游戏日后领先者获得绑定 | 待执行 |
| 亲密锁定 | 一名玩家完成亲密确认 | 其他玩家无法继续赠礼和接取委托 | 待执行 |
| 家园绑定 | 玩家完成入住条件后绑定双人床 | 同一玩家只能有一名家园伙伴 | 待执行 |
| 跟随祝福 | 亲密确认后设置角色跟随玩家，分别验证划船、钓鱼、挖矿、近战、受伤、末影珍珠事件接入 | 仅同维度、32 格内、跟随同一玩家时触发对应祝福效果 | 待执行 |
| 角色死亡 | 击杀角色本体 | 关系、委托、绑定、家园清零，终章奖励保留 | 待执行 |
| 网络越权 | 客户端构造无权限赠礼或接取请求 | 服务端拒绝且 SavedData 不变 | 待执行 |

## 构建验证

- `./gradlew test`：通过（2026-06-05，全量测试无失败）
- `./gradlew build`：通过（2026-06-05，BUILD SUCCESSFUL）

## 环境限制

- `./gradlew runClient`：当前无 GUI 环境，无法启动 Minecraft 客户端进行烟雾测试
- `./gradlew runServer`：当前环境未预配专用服务端，无法启动
- `./gradlew runGameTestServer`：当前环境未预配 GameTest 结构，无法启动

以上手工验收和 Minecraft 运行时测试需在具备 Minecraft 客户端与 NeoForge 26.1.2 运行环境的开发机上执行。
