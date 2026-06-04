# Girlfriends v0.1.0 底层系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/v0.1.0/DR_system.md` 落地 Girlfriends v0.1.0 的底层系统，实现关系、礼物、委托、绑定、家园、祝福、死亡重生、网络同步与回归验收基础能力。

**Architecture:** 采用服务端权威的 NeoForge 单体模组架构，核心规则拆成可单元测试的领域模型与领域服务，Minecraft 事件、网络、GUI 和实体行为只通过边界接口接入。世界级长期状态集中保存到 `GirlfriendsWorldData`，所有 Serverbound 请求在服务端重新校验权限与状态。

**Tech Stack:** Java 25、Minecraft 26.1.2、NeoForge 26.1.2.71、Gradle、JUnit Platform、NBT/SavedData、NeoForge Event/Network API。

---

## 1. 参考文档

- 产品需求：`docs/v0.1.0/PRD.md`
- 技术设计：`docs/v0.1.0/DR_system.md`
- 设计评审：`docs/v0.1.0/DR_review.md`

## 2. 执行原则

1. 先实现纯 Java 领域模型和服务，再接入 Minecraft 事件、网络和客户端表现。
2. 每个 Story 先写失败测试，再实现最小代码，再运行对应测试。
3. 所有 public 类、接口和方法必须有中文 Javadoc。
4. 新增 Java 包必须补 `package-info.java`。
5. 代码缩进使用 Tab，大括号使用 K&R 风格。
6. 不实现缺少 GDD 的角色 AI、具体委托目标参数、结构细节、对白和终章奖励内容。
7. 不直接信任客户端状态，所有 Serverbound 网络包必须重新校验实体、距离、权限、物品和当前 SavedData。
8. 不主动提交 git；计划中的提交检查点仅在用户明确要求提交时执行。

## 3. 当前工程前置修正

当前 `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java` 中 `MODID` 仍是模板值 `examplemod`，Story 1 必须先改为 `girlfriends`，并补充根日志器。

```java
public static final String MODID = "girlfriends";
```

`build.gradle` 已引入 `junit-platform-console-standalone`，Story 1 需要补齐 `test { useJUnitPlatform() }`，否则 JUnit 5 测试不会按预期运行。

```groovy
test {
    useJUnitPlatform()
}
```

## 4. 主执行顺序

| 顺序 | Story | 子计划 | 依赖 | 核心验收 |
| --- | --- | --- | --- | --- |
| 1 | 基础领域模型与持久化 | `docs/v0.1.0/PLAN_story_01_persistence.md` | 无 | 领域状态可创建、修改、序列化和反序列化。 |
| 2 | 好感度与每日计数系统 | `docs/v0.1.0/PLAN_story_02_relationship.md` | Story 1 | 好感裁剪、阶段推导、每日重置和死亡清零通过测试。 |
| 3 | 礼物系统 | `docs/v0.1.0/PLAN_story_03_gift.md` | Story 1、Story 2 | 礼物档位、公式、每日上限、拒收和绑定限制通过测试。 |
| 4 | 委托槽与委托框架 | `docs/v0.1.0/PLAN_story_04_quest.md` | Story 1、Story 2 | 单槽互斥、固定委托发布、随机委托刷新和过期通过测试。 |
| 5 | 多人绑定与竞争 | `docs/v0.1.0/PLAN_story_05_binding.md` | Story 2、Story 4 | 爱慕绑定、动摇期、绑定转移和亲密锁定通过测试。 |
| 6 | 家园系统 | `docs/v0.1.0/PLAN_story_06_home.md` | Story 2、Story 5 | 家园入住、双人床、回血、每日好感和争执防重通过测试。 |
| 7 | 跟随状态与祝福系统 | `docs/v0.1.0/PLAN_story_07_blessing.md` | Story 2、Story 5 | 五类祝福启用条件和事件修正边界通过测试。 |
| 8 | 角色死亡、庇护所记录与重生接口 | `docs/v0.1.0/PLAN_story_08_respawn.md` | Story 1、Story 2、Story 4、Story 5、Story 6 | 死亡清零、终章奖励保留、最近庇护所查找和待重生通过测试。 |
| 9 | 网络同步与交互摘要 | `docs/v0.1.0/PLAN_story_09_network.md` | Story 2、Story 3、Story 4、Story 6、Story 7 | GUI 摘要只读、Serverbound 越权请求被拒绝。 |
| 10 | 测试与回归验收 | `docs/v0.1.0/PLAN_story_10_regression.md` | Story 1 到 Story 9 | 核心服务、持久化、网络安全和手工验收清单齐备。 |

## 5. 全局包结构

```text
src/main/java/com/hexagram2021/girlfriends/
	common/
		character/
		relationship/
		gift/
		quest/
		binding/
		home/
		blessing/
		death/
		network/
		persist/
src/test/java/com/hexagram2021/girlfriends/
	common/
		relationship/
		gift/
		quest/
		binding/
		home/
		blessing/
		death/
		persist/
```

## 6. 全局验证命令

每个 Story 完成后至少运行对应测试，Story 10 完成后运行完整验证。

```bash
./gradlew test
```

期望结果：Gradle `test` 任务成功结束，失败数为 0。

```bash
./gradlew build
```

期望结果：Gradle `build` 任务成功结束，生成模组 jar。

## 7. 角色类型扩展约束

`GirlfriendType` 和 `BlessingType` 禁止实现为 enum，必须是 NeoForge 自定义注册表中的可注册数据类。

领域服务、SavedData、网络包和测试示例统一使用 `ResourceLocation girlfriendTypeId` 作为角色类型参数和持久化 key；只有在需要读取角色元数据时，才通过注册表从 ID 解析到 `GirlfriendType` 对象。

内置角色 ID 由 `GirlfriendTypes` 提供，附属模组可以向 `GirlfriendsRegistries.GIRLFRIEND_TYPE` 注册新角色类型，不需要 mixin 本模组代码。

自定义 `GirlfriendType` 与 `BlessingType` 注册表必须在 NeoForge `NewRegistryEvent` 中创建，`GirlfriendTypes.REGISTER.register(modEventBus)` 与 `BlessingTypes.REGISTER.register(modEventBus)` 只负责向已经创建的注册表挂载内置条目。

## 8. Story 间接口冻结点

1. Story 1 完成后冻结 `GirlfriendType` 注册表对象结构、`GirlfriendTypes` 内置注册项、`GirlfriendsRegistries` 注册表 key、`AffectionStage`、`QuestType`、`QuestState`、`RelationKey`、`GirlfriendsWorldData` 的基本字段命名。
2. Story 2 完成后冻结 `RelationshipService` 的好感修改入口，其他系统不得直接修改关系字段。
3. Story 4 完成后冻结 `QuestInstance` 状态流转，后续固定委托和随机委托只能通过 `QuestService` 操作。
4. Story 5 完成后冻结绑定与亲密锁定权限规则，Story 3、Story 4、Story 9 需要调用同一套权限判断。
5. Story 9 完成后冻结客户端可见摘要字段，GUI 只能读摘要，不读完整 SavedData。

## 9. 完成标准

1. 10 个 Story 子计划均已执行。
2. `./gradlew test` 通过。
3. `./gradlew build` 通过。
4. 手工验收记录覆盖单人关系、多玩家竞争、家园绑定、角色死亡和网络越权拒绝。
5. 未实现 GDD 尚未定义的角色 AI、具体委托内容、结构装饰、对白和终章奖励。