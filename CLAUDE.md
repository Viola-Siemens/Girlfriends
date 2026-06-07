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

- **Minecraft 版本**: 1.26.1.2（NeoForge 26.1.2.71）
- **Java 版本**: Java 25（toolchain）
- **构建工具**: Gradle + NeoForge ModDev Plugin 2.0.141
- **测试框架**: JUnit Jupiter 5.13.4
- **映射**: Mojang official mappings

## 架构分层

代码位于 `com.hexagram2021.girlfriends` 包下，采用四层架构。

### 1. 领域模型层（Data Model）

纯数据结构，负责 NBT 序列化/反序列化。位于 `common/` 下各子包中的 `*State`、`*Record`、`*Result` 和 enum 类型。

| 关键类型 | 位置 | 职责 |
|---|---|---|
| `GirlfriendsWorldData` | `common/persist/` | 世界级 SavedData，持有所有 characters/relations/homes 三张核心表 |
| `CharacterWorldState` | `common/character/` | 单角色世界状态（存活、实体 UUID、委托槽、绑定、庇护所列表） |
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
| `GiftService` | 礼物档位判定、赠礼公式 `Δ = base × √((16-dailyGain)/16)`、每日上限 15 点、偏好揭示、绑定限制 |
| `QuestService` | 委托槽管理（每角色唯一槽）、固定委托顺序发布、随机委托 5~10 日过期、接取/完成/过期、owner 原子校验 |
| `BindingService` | 爱慕绑定建立、动摇期（3 游戏日）+ 挑战者转移、亲密锁定排他 |
| `HomeService` | 家园伙伴邀请/解除、双人床校验、家园回血 + 每日好感、争执事件（每日 1 次、扣 3~5 点） |
| `BlessingService` | 祝福生效条件校验（亲密+跟随+同维度+距离≤32）、祝福参数读取、效果判定 |
| `CharacterRespawnService` | 死亡集中清理（关系/委托/绑定/家园全清 + 终章奖励保留）、最近庇护所查找、待重生状态 |

所有 Service 构造方法支持注入外部依赖（如 `Function<Identifier, Optional<GirlfriendType>>`），便于单元测试中使用模拟实现。

### 3. Minecraft 集成层（Integration Layer）

- **`GirlfriendsMod`**: 模组入口，注册 Registry、网络包、ServerReloadListener
- **`GirlfriendsNetwork`**: 网络包注册与处理入口。所有 Serverbound handler 在服务端重新校验权限与状态（不信任客户端）
- **`GirlfriendsRegistries`**: 自定义注册表（`GIRLFRIEND_TYPE_REGISTRY`、`BLESSING_TYPE_REGISTRY`）
- **`GirlfriendTypes`** / **`BlessingTypes`**: 内置类型通过 DeferredRegister 注册

网络协议版本硬编码为 `"1"`，使用 NeoForge `PayloadRegistrar` 注册 play-to-client 和 play-to-server 包。

### 4. 表现层（Presentation Layer）

- **`GirlfriendsModClient`**: 客户端入口，注册配置界面扩展点
- **`ClientInteractionStore`**: 客户端交互摘要缓存

## 数据驱动系统

以下内容通过 `SimplePreparableReloadListener` 从数据包加载。

- **`GiftPreferenceManager`**: 从 `data/<ns>/girlfriends/gift_preferences/*.json` 加载角色礼物偏好（favorite/liked/accepted/disliked 四个档位 + tag 支持）
- **`BlessingParameterManager`**: 从 `data/<ns>/girlfriends/blessing_parameters/*.json` 加载祝福参数
- **`FixedQuestDefinitionManager`**: 固定委托定义加载
- **`RandomQuestTemplateManager`**: 随机委托模板加载

## 核心设计原则

1. **服务端权威**: 所有关系、奖励、委托、家园与祝福判定以服务端 `SavedData` 为准。客户端只展示摘要，不提交可信状态
2. **不可变注册表**: `GirlfriendType` / `BlessingType` 使用 NeoForge 自定义注册表（非 enum），支持附属模组扩展
3. **依赖注入**: 所有 Service 的依赖（如 WorldData、各种 Manager）通过构造函数传入，支持单元测试中替换
4. **委托单槽互斥**: `CharacterWorldState.currentQuest` 每个角色只有 1 个委托槽，固定委托优先于随机委托
5. **原子状态更新**: `GirlfriendsWorldData` 提供 `updateRelation()` / `updateCharacter()` / `updateHome()` 方法，接受 Consumer 并在内部调用 `setDirty()`
6. **NBT 序列化**: 所有持久化 key 使用小写蛇形命名（如 `pending_respawn`、`follow_mode`），根数据节点含 `data_version` 用于格式迁移

## 测试规范

- 测试类命名: `<ServiceName>Test`，与被测类同包但位于 `src/test/java/`
- 测试方法命名: 小驼峰描述行为，如 `changeAffectionClampsToRange`
- 测试直接构造 `GirlfriendsWorldData`（无参构造）并注入 Service，不依赖 Minecraft 环境
- 使用 `Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "xxx")` 构造角色 ID
- 固定 UUID 使用 `UUID.fromString("00000000-0000-0000-0000-00000000000X")` 确保可重现

## 暂未实现

模组目前只实现了底层系统架构，暂未实现角色创建、生成与交互的逻辑，等待后续开发完善。

## Git 提交规范

提交信息格式: `type(scope): subject`

type: feat / fix / docs / chore / test / style / revert / ci
scope: 需求/故事编号，如 REQ-7

## 文档

- `docs/v0.1.0/PRD.md`: 产品需求文档（角色内容、玩法规则、数值表）
- `docs/v0.1.0/DR_system.md`: 底层系统技术设计（架构、持久化、服务接口、性能分析）
- `docs/v0.1.0/GDD/`: 五位角色游戏设计文档（固定委托线、随机委托模板、偏好、终章奖励）
- `docs/v0.1.0/PLAN_story_*.md`: 各 Story 实现计划

## 文档基线

本文基于提交 `4749bfd4928a3637a5ab2ba7efb8b342c7da5bb6` 的内容创建，后续更新时可以以此为基准，阅读 git 变更，避免全量阅读项目内容。