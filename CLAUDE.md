# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

《Girlfriends》是一款面向 Minecraft 生存模式的 NeoForge 模组（modId: `girlfriends`），提供五位可攻略女性角色的养成体验。核心玩法包括好感度、委托、赠礼、跟随祝福、家园同居与多人绑定竞争。

五位首发角色：沫沫（momo）、渔溪（yuxi）、梅疏（meishu）、晚萤（wanying）、幽若（youruo）。

## 构建与测试

```bash
# 构建整个模组
./gradlew build

# 仅编译
./gradlew classes

# 运行全部单元测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.hexagram2021.girlfriends.common.relationship.RelationshipServiceTest"

# 运行单个测试方法
./gradlew test --tests "com.hexagram2021.girlfriends.common.relationship.RelationshipServiceTest.changeAffectionClampsToRange"

# 运行 client（调试模组）
./gradlew runClient

# 运行 server
./gradlew runServer

# 运行 gameTestServer
./gradlew runGameTestServer

# 运行 data generation
./gradlew runData

# 刷新依赖
./gradlew --refresh-dependencies
```

## 技术栈

- **Minecraft 版本**: 26.1.2（NeoForge 26.1.2.71）
- **Java 版本**: Java 25（toolchain）
- **构建工具**: Gradle + NeoForge ModDev Plugin 2.0.141
- **测试框架**: JUnit Jupiter 5.13.4
- **映射**: Mojang official mappings

## 架构分层

代码位于 `com.hexagram2021.girlfriends` 包下，分为五层。

### 1. 领域模型层（Data Model）

纯数据结构，负责 NBT 序列化/反序列化。位于 `common/` 下各子包中的 `*State`、`*Record`、`*Result` 和 enum 类型。

| 关键类型 | 位置 | 职责 |
|---|---|---|
| `GirlfriendsWorldData` | `common/persist/` | 世界级 SavedData，持有所有 characters/relations/homes 三张核心表 |
| `CharacterWorldState` | `common/character/` | 单角色世界状态（存活、实体 UUID、委托槽、绑定、庇护所列表、跟随模式） |
| `PlayerCharacterRelation` | `common/relationship/` | 玩家对单角色的关系（好感值 0~1000、亲密确认、每日计数、已完成固定委托集合） |
| `HomeState` | `common/home/` | 玩家家园绑定（伙伴 ID、床锚点、活跃标记） |
| `CharacterBindingState` | `common/binding/` | 角色多人绑定与动摇期状态 |
| `QuestInstance` | `common/quest/` | 委托实例（类型、状态、owner、过期日、进度 NBT） |
| `ShelterRecord` | `common/death/` | 庇护所记录（维度、坐标、注册日、可用标记） |

所有 `*State` 类遵循 `serializeNBT()` / `static deserializeNBT(CompoundTag)` 模式。

### 2. 领域服务层（Service Layer）

纯规则逻辑，接受 `GirlfriendsWorldData` + 可注入的外部依赖（如定义获取器、偏好管理器），不直接依赖 Minecraft 对象，可单元测试。

| 服务 | 职责 |
|---|---|
| `RelationshipService` | 好感查询/变更/裁剪（0~1000）、阶段推导（数值+确认标记联合判定）、每日计数重置、角色死亡批量清空 |
| `GiftService` | 礼物档位判定、赠礼公式 `Δ = base × √((16-dailyGain)/16)`、每日上限 15 点、偏好揭示、绑定限制、通过 `GiftQuoteManager` 抽取角色台词反馈 |
| `QuestService` | 委托槽管理（每角色唯一槽）、固定委托顺序发布、随机委托 5~10 日过期、接取/完成/过期、owner 原子校验 |
| `BindingService` | 爱慕绑定建立、动摇期（3 游戏日）+ 挑战者转移、亲密锁定排他 |
| `HomeService` | 家园伙伴邀请/解除、双人床校验、家园回血 + 每日好感、争执事件（每日 1 次、扣 3~5 点） |
| `BlessingService` | 祝福生效条件校验（亲密+跟随+同维度+距离≤32）、祝福参数读取、效果判定 |
| `CharacterRespawnService` | 死亡集中清理（关系/委托/绑定/家园全清 + 终章奖励保留）、最近庇护所查找、待重生状态 |

所有 Service 构造方法支持注入外部依赖（如 `Function<Identifier, Optional<GirlfriendType>>`），便于单元测试中使用模拟实现。

### 3. 实体与 AI 层（Entity & AI Layer）

角色实体和 AI 行为系统。实体注册在 `common/entity/`，AI 组件在 `common/entity/ai/`。

**实体架构**:
- `GirlfriendEntity`（抽象基类，extends `PathfinderMob` implements `InventoryCarrier`）— 统一管理跟随模式、爱慕玩家 UUID、18 格背包、自动回血、定时同步到 WorldState、Brain 刷新
- 子类覆写 `getGirlfriendTypeId()` 返回角色类型 ID，并提供差异化 Brain Provider 及 Behavior 注册
- 目前实现：`MomoEntity`（沫沫），其余四位角色待后续 Story 实现

**AI 子系统**（使用原版 Brain/Schedule 体系）:
- `GirlfriendsActivities` — 六个自定义 Activity：`MORNING`、`DAY_WORK`、`AFTERNOON`、`SUNSET`、`NIGHT_REST`、`FOLLOW`，通过 DeferredRegister 注册到原版 Activity 注册表
- `GirlfriendsMemoryTypes` — 自定义 MemoryModuleType：庇护所位置、家园床点、花朵/蜂箱/水域/矿石位置（各角色专属）、骨粉产出标记等
- `GirlfriendsSensorTypes` — 自定义 SensorType：`ShelterSensor`、`FlowerSensor`、`BeehiveSensor`
- `GirlfriendsEnvironmentAttributes` — 环境属性（如沫沫日程表），驱动 AI 按游戏时刻自动切换 Activity
- `GirlfriendCommonAiPackages` — 所有角色共用的 Core Activity 行为（跟随判定、睡眠、日程更新等）
- 通用 Behavior：`StayCloseToIntimatePlayer`（跟随贴近）、`GirlfriendCalmDown`（恐慌冷却）、`GirlfriendPanicTrigger`（恐慌触发）、`RunOneLoop`（循环执行行为列表）、`BackToShelter`（返回庇护所）、`ShelterBoundRandomStroll`（庇护所附近漫步）
- 沫沫专属 Behavior：`PlantAndHarvestFlower`（种花/采花）、`ProduceBoneMeal`（生产骨粉）

AI 设计关键点：
- 使用原版 `Brain.provider()` + `ActivityData` + `Schedule` 体系，不自行造轮子
- 每个角色通过 `EnvironmentAttribute<Activity>` 注册日程表，由 `UpdateActivityFromSchedule` 行为驱动时段切换
- 跟随模式（FOLLOW）作为独立 Activity，与日常日程互斥：在跟随状态下 Brain 调度 FOLLOW activity，否则按日程执行
- Brain 支持运行时刷新（`refreshBrain`），在跟随模式切换等场景重建行为树

### 4. Minecraft 集成层（Integration Layer）

- **`GirlfriendsMod`**: 模组入口，注册自定义 Registry、EntityType、Activity、MemoryModuleType、SensorType、EnvironmentAttribute、Item、Block、DataComponentType、CreativeModeTab、网络包、ServerReloadListener、命令、实体事件监听
- **`GirlfriendEntityEvents`**: 实体生命周期事件处理器 — EntityJoinLevel（标记存活、同步 UUID）、ServerTick.Post（每日维护：重置计数器、过期/刷新随机委托）、LivingDeath（标记死亡、记录死亡坐标）、RightClickBlock（花盆+洒水壶交互阻止）
- **`GirlfriendsNetwork`**: 网络包注册与处理入口。所有 Serverbound handler 校验玩家是否可以接触到目标实体（`canReachEntity`，距离 ≤8）。不直接引用客户端类，clientbound handler 中的 UI 操作通过静态消费者注入模式委托给 `GirlfriendsModClient`。赠礼反馈采用双通道：角色台词走聊天栏（`sendSystemMessage`），好感度变化走 action bar。当前注册 7 个 serverbound + 2 个 clientbound 包
- **`GirlfriendsRegistries`**: 自定义注册表（`GIRLFRIEND_TYPE_REGISTRY`、`BLESSING_TYPE_REGISTRY`）
- **`GirlfriendTypes`** / **`BlessingTypes`**: 内置类型通过 DeferredRegister 注册
- **`GirlfriendsItems`**: 模组物品注册（手捧花 `BOUQUET`、洒水壶 `WATERING_CAN`，后者挂载 `WATER_LEVEL` 数据组件）
- **`GirlfriendsBlocks`**: 模组方块注册（目前主要提供方块标签）
- **`GirlfriendsDataComponentTypes`**: 物品数据组件类型注册（`WATER_LEVEL`，`Codec.INT` 持久化 + `ByteBufCodecs.INT` 网络同步）
- **`GirlfriendsCreativeModeTabs`**: 创造模式物品栏标签页
- **`AffectionCommand`**: `/affection get|set|add <girlfriend> <player> [value]` 调试命令，权限等级 GAMEMASTERS

网络协议版本硬编码为 `"1"`，使用 NeoForge `PayloadRegistrar` 注册 play-to-client 和 play-to-server 包。

**网络包清单**:

| 方向 | 包类型 | 用途 |
|---|---|---|
| C→S | `ServerboundGiveGiftPacket` | 玩家手持物品向角色赠礼 |
| C→S | `ServerboundAcceptQuestPacket` | 玩家接取角色当前委托 |
| C→S | `ServerboundSetFollowModePacket` | 切换角色跟随模式（需亲密确认） |
| C→S | `ServerboundDeliverQuestPacket` | 玩家交付已完成委托 |
| C→S | `ServerboundConfirmIntimacyPacket` | 玩家确认亲密关系 |
| C→S | `ServerboundInviteHomePacket` | 玩家邀请角色同居 |
| C→S | `ServerboundGiveGiftFromSlotPacket` | 玩家从指定背包槽位向角色赠礼 |
| S→C | `ClientboundSyncInteractionDataPacket` | 同步交互摘要到客户端（handler 通过注入打开 Screen） |
| S→C | `ClientboundQuestIconPacket` | 同步委托图标状态 |

### 5. 表现层（Presentation Layer）

- **`GirlfriendsModClient`**: 客户端入口，注册 EntityRenderer、ModelLayer（五位角色全部注册）、Screen，并通过 `GirlfriendsNetwork.setScreenOpener()` 注入主交互界面打开逻辑
- **`ClientInteractionStore`**: 客户端交互摘要缓存（`InteractionSummary` + `QuestIconSummary`）
- **`GirlfriendRenderer`**: 角色实体渲染器（使用 `GirlfriendModel`）
- **`GirlfriendsModelLayers`**: 模型层定义（五位角色：MOMO / YUXI / MEISHU / WANYING / YOURUO）
- **`MainInteractionScreen`**: 主交互界面（角色名、好感度模糊显示、委托入口、赠礼入口、跟随切换、亲密确认、家园邀请）
- **`QuestViewScreen`**: 委托查看界面（任务描述、目标进度、奖励预览）
- **`GiftScreen`**: 赠礼界面（背包槽位选择，通过 `ServerboundGiveGiftFromSlotPacket` 提交）

## 数据驱动系统

以下内容通过 `SimplePreparableReloadListener` 从数据包加载。

- **`GiftPreferenceManager`**: 从 `data/<ns>/girlfriends/gift_preferences/*.json` 加载角色礼物偏好（favorite/liked/accepted/disliked 四个档位 + tag 支持）
- **`GiftQuoteManager`**: 从 `data/<ns>/girlfriends/gift_quotes/*.json` 加载角色赠礼回复台词语料，按五档位（favorite 按物品 ID 细分）提供随机 i18n key 抽取，供 `GiftService` 在赠礼时生成角色台词喵~
- **`BlessingParameterManager`**: 从 `data/<ns>/girlfriends/blessing_parameters/*.json` 加载祝福参数
- **`FixedQuestDefinitionManager`**: 固定委托定义加载（含 objective 解析）
- **`RandomQuestTemplateManager`**: 随机委托模板加载

## 日程与标签数据

AI 日程系统通过数据包驱动，使用原版 Timeline/Schedule 体系：

- `data/<ns>/timeline/<character>_schedule.json`: 单角色日程表（如 `momo_schedule.json`），定义各时刻对应的 Activity
- `data/<ns>/tags/timeline/girlfriends_schedule.json`: 角色日程标签，标记需要加载日程的角色集合
- `data/minecraft/tags/timeline/universal.json`: 接入原版通用 Timeline 系统

物品/方块标签文件（`data/<ns>/tags/`）用于定义角色拾取物过滤、草方块转换等行为。

## 委托目标处理器（Quest Objective Handler）

委托目标使用策略模式，`QuestObjectiveHandler` 接口定义六个生命周期方法：

```java
boolean canAccept(QuestInstance)     // 接取条件校验
void onAccept(QuestInstance)         // 接取时初始化进度
void onEvent(QuestInstance, Object)  // 处理目标相关事件
boolean isCompleted(QuestInstance)   // 完成判定
CompoundTag serializeProgress()      // 序列化进度
void deserializeProgress(CompoundTag) // 反序列化进度
```

已注册的处理器类型（在 JSON 中由 `type` 字段指定）：
`item_delivery`、`structure_visit`、`block_stay`、`collect`、`build`、`accompany`、`fight`。

部分处理器逻辑待后续 Story 根据 GDD 完善（标记 TODO）。

## 核心设计原则

1. **服务端权威**: 所有关系、奖励、委托、家园与祝福判定以服务端 `SavedData` 为准。客户端只展示摘要，不提交可信状态
2. **不可变注册表**: `GirlfriendType` / `BlessingType` 使用 NeoForge 自定义注册表（非 enum），支持附属模组扩展；Activity / MemoryModuleType / SensorType 使用原版注册表
3. **依赖注入**: 所有 Service 的依赖（如 WorldData、各种 Manager）通过构造函数传入，支持单元测试中替换
4. **委托单槽互斥**: `CharacterWorldState.currentQuest` 每个角色只有 1 个委托槽，固定委托优先于随机委托
5. **原子状态更新**: `GirlfriendsWorldData` 提供 `updateRelation()` / `updateCharacter()` / `updateHome()` 方法，接受 Consumer 并在内部调用 `setDirty()`
6. **NBT 序列化**: 所有持久化 key 使用小写蛇形命名（如 `pending_respawn`、`follow_mode`），根数据节点含 `data_version` 用于格式迁移
7. **实体-状态双向同步**: `syncToWorldState`（实体→持久化）每 ~10 秒自动执行；`syncFromWorldState`（持久化→实体）在实体加载时执行；命令/网络包在修改后即时同步
8. **原版 AI 体系**: 使用 Minecraft 原版的 Brain/Schedule/Activity/Memory/Sensor 体系，不自行造 AI 调度轮子
9. **物理端隔离**: `common/` 包下的代码不得 import `net.minecraft.client.*` 或 `com.hexagram2021.girlfriends.client.*` 中的任何类，否则在专用服务端加载时 JVM 会因 `NoClassDefFoundError` 崩溃。当 common 代码需要触发客户端行为（如打开 Screen）时，通过静态消费者注入模式：在 common 侧暴露 `@Nullable` 静态字段 + `setXxx()` 方法，由 `GirlfriendsModClient` 在 `onClientSetup` 中注入实现，服务端保持 null 即可安全 no-op 喵~

## 测试规范

- 测试类命名: `<ServiceName>Test`，与被测类同包但位于 `src/test/java/`
- 测试方法命名: 小驼峰描述行为，如 `changeAffectionClampsToRange`
- 测试直接构造 `GirlfriendsWorldData`（无参构造）并注入 Service，不依赖 Minecraft 环境
- 使用 `Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "xxx")` 构造角色 ID
- 固定 UUID 使用 `UUID.fromString("00000000-0000-0000-0000-00000000000X")` 确保可重现

已有测试类：`GirlfriendsWorldDataTest`、`RelationshipServiceTest`、`GiftServiceTest`、`GiftQuoteManagerTest`、`QuestServiceTest`、`BindingServiceTest`、`HomeServiceTest`、`BlessingServiceTest`、`CharacterRespawnServiceTest`、`InteractionSummaryServiceTest`、`ClientInteractionStoreTest`、`ServerboundPacketSerializationTest`、`ServerboundPermissionTest`

## 开发状态

- **已完成**: 底层系统架构（Story 01~09）— 持久化、关系、赠礼、委托、绑定、家园、祝福、重生、网络协议
- **已完成**: REQ-7 角色实体与 AI — `GirlfriendEntity` 抽象基类、`MomoEntity` 完整实现（含日程 AI 和传感器）、客户端渲染器（五位角色模型层全部注册）、命令系统、实体事件处理器、模组物品（手捧花、洒水壶+数据组件）、创造模式物品栏
- **已完成**: REQ-7 赠礼台词系统 — `GiftQuoteManager` 语料管理器，五个角色共 133 条中英双语台词，赠礼反馈拆分为角色台词（聊天栏）+ 好感度变化（action bar）双通道
- **已完成**: REQ-8 玩家交互界面 — `MainInteractionScreen` 主界面、`QuestViewScreen` 委托查看、`GiftScreen` 背包赠礼，4 个新增 Serverbound 包（交付委托/确立关系/邀请同居/背包送礼）
- **进行中**: 委托目标处理器具体逻辑（部分标记 TODO，等待 GDD 细化）
- **待实现**: 渔溪/梅疏/晚萤/幽若四个角色的实体子类及专属 AI Behavior、委托奖励发放、角色自然生成逻辑

## Git 提交规范

提交信息格式: `type(scope): subject`

type: feat / fix / docs / chore / test / style / revert / ci
scope: 需求/故事编号，如 REQ-7

## 文档

- `docs/v0.1.0/PRD.md`: 产品需求文档（角色内容、玩法规则、数值表）
- `docs/v0.1.0/DR_system.md`: 底层系统技术设计（架构、持久化、服务接口、性能分析）
- `docs/v0.1.0/GDD/`: 五位角色游戏设计文档（`00_总游戏设计文档.md`、`01_沫沫.md` ~ `05_幽若.md`）
- `docs/v0.1.0/PLAN_story_*.md`: 各 Story 实现计划
- `docs/v0.1.0/TEST_system.md`: 测试记录与手工验收清单
- `docs/superpowers/specs/2026-06-07-character-entity-ai-design.md`: REQ-7 角色实体与 AI 技术规格
- `docs/superpowers/plans/2026-06-07-character-entity-ai-plan.md`: REQ-7 实施计划
- `docs/superpowers/specs/2026-06-15-player-interaction-ui-design.md`: REQ-8 玩家交互界面技术规格
- `docs/superpowers/plans/2026-06-15-player-interaction-ui-plan.md`: REQ-8 实施计划
- `docs/superpowers/specs/2026-06-20-gift-quote-system-design.md`: 礼物回复台词系统设计规格
- `docs/superpowers/plans/2026-06-20-gift-quote-system-plan.md`: 礼物回复台词系统实施计划

## 参考项目目录

- `Sources-26.1.2`: Minecraft 26.1.2 和 NeoForge 源码
- `Tetrachord-Lib`: TetrachordLib 模组，提供了 KD 树、线段树等数据结构

## 文档基线

本文基于提交 `7cdf22d`（Merge pull request #3 - gift_dialogue）的内容创建。后续更新时可以以此为基准，阅读 git 变更，避免全量阅读项目内容。
