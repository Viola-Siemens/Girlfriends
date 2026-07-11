# 渔溪钓鱼行为技术设计

## 概述

为 `YuxiEntity` 添加名为 `FishNearbyWater` 的 AI Behavior，使其能够在手持钓竿时，以类似玩家的方式进行垂钓——生成浮标实体、等待鱼上钩、收竿产出战利品。

## 文件清单

### 新增文件

| 文件 | 位置 | 职责 |
|---|---|---|
| `GirlfriendFishingHook.java` | `common/entity/` | 自定义浮标 Projectile 实体，适配 `LivingEntity` owner |
| `FishNearbyWater.java` | `common/entity/ai/behavior/` | 钓鱼 Behavior，管理抛竿→等待→收竿→冷却完整生命周期 |
| `GirlfriendFishingHookRenderer.java` | `client/renderer/` | 客户端浮标渲染器，参照原版 `FishingHookRenderer` 实现 |

### 修改文件

| 文件 | 变更 |
|---|---|
| `GirlfriendsEntities.java` | 注册 `GIRLFRIEND_FISHING_HOOK` EntityType |
| `YuxiEntity.java` | `dayWork` activity 中注册 `FishNearbyWater` behavior |
| `GirlfriendsModClient.java` | 注册 `GirlfriendFishingHookRenderer` |

## 架构与数据流

```
WaterSensor → NEAREST_WATER 记忆
    ↓
FishNearbyWater Behavior（dayWork priority 2）
    ↓ (检查背包有钓竿 → 装备到主手 → 面朝水域)
    ↓ spawn
GirlfriendFishingHook（状态机：FLYING → BOBBING → BITING）
    ↓ retrieve
LootTable(BuiltInLootTables.FISHING) → ItemEntity → 插入渔溪背包
```

## GirlfriendFishingHook 实体

### 继承关系

直接继承 `net.minecraft.world.entity.projectile.Projectile`，不继承原版 `FishingHook`。

原因：原版 `FishingHook` 在构造器、`tick()`、`shouldStopFishing()`、`retrieve()` 全链路绑定 `Player` 类型，`getPlayerOwner()` 返回 null 时立即 discard。适配成本高于自建。

### 状态机

```
FLYING  →  碰到水面  →  BOBBING  →  timeUntilLured=0 & timeUntilHooked=0 & nibble>0  →  BITING
                                                         ↓ retrieve
                                                       DISCARD
```

| 状态 | 说明 |
|---|---|
| `FLYING` | 从 owner 发射后向水面飞行，受重力影响 |
| `BOBBING` | 已落水，等待鱼上钩。期间产生 splash/bubble/fishing 粒子 |
| `BITING` | 鱼已咬钩，`nibble > 0`，浮标下沉。Behavior 检测到此状态后调用 `retrieve()` |

### 关键字段

```java
private State state = State.FLYING;
private int timeUntilLured;    // 鱼被吸引剩余 tick（初始化: 100~600）
private int timeUntilHooked;   // 鱼咬钩剩余 tick（初始化: 20~80）
private int nibble;            // 咬钩窗口倒计时（初始化: 20~40）
private int outOfWaterTime;    // 离水计数
private final RandomSource random;
```

### SynchedData

```java
private static final EntityDataAccessor<Boolean> DATA_BITING =
    SynchedEntityData.defineId(GirlfriendFishingHook.class, EntityDataSerializers.BOOLEAN);
```

仅同步 `DATA_BITING` 一个标志，供客户端渲染判断浮标下沉状态。

### 核心方法

| 方法 | 可见性 | 说明 |
|---|---|---|
| `GirlfriendFishingHook(EntityType<?>, Level)` | public | 供 `EntityType.Builder` 反射构造 |
| `setOwner(LivingEntity)` + 参数设置 | public | Behavior 在构造后调用，设置 owner、计算抛射方向和初速度 |
| `tick()` | public | 状态机驱动（override） |
| `isBiting()` | public | `return this.nibble > 0`，供 Behavior 判定收竿时机 |
| `retrieve(ItemStack rod)` | public | 查 FISHING 战利品表、生成 ItemEntity 飞向 owner、消耗钓竿耐久、discard |
| `shouldStopFishing()` | private | 检查 owner 存活 & 距离≤32 格 & owner 主手持有钓竿 |

### 与原版 FishingHook 的差异

| 特性 | 原版 FishingHook | GirlfriendFishingHook |
|---|---|---|
| owner 类型 | `Player` | `LivingEntity` |
| 勾实体 | 支持 (`HOOKED_IN_ENTITY`) | 不支持 |
| 收竿触发 | 玩家右键 | Behavior 通过 `isBiting()` 自动触发 |
| 统计/进度 | `Stats.FISH_CAUGHT` / `CriteriaTriggers` | 无 |
| `openWater` 判定 | 有（5×5×4 区域检测） | 简化：仅检查 `outOfWaterTime < 10` |
| `ItemFishedEvent` | 触发 NeoForge 事件 | 不触发 |
| 战利品表 | `BuiltInLootTables.FISHING` | 相同 |

### 抛射初速度计算

参照原版 `FishingHook(Player, Level, int, int)` 的计算公式：

```
yRot_rad = -yRot * π/180
xRot_rad = -xRot * π/180
dir = Vec3(-sin(yRot_rad), clamp(-sin(xRot_rad)/cos(xRot_rad), -5, 5), -cos(yRot_rad))
velocity = dir * (0.6/len + random.triangle(0.5, 0.0103))
```

发射起点为 `owner.getEyePosition()`，方向为 owner 当前朝向（yRot / xRot）。

### 粒子效果

| 阶段 | 粒子 | 触发条件 |
|---|---|---|
| BOBBING - 等待 | `SPLASH`（溅水） | `timeUntilLured` 期间，随机在 bobber 周围水面产生 |
| BOBBING - 即将上钩 | `FISHING`（鱼线轨迹）+ `BUBBLE`（气泡） | `timeUntilHooked` 期间，鱼接近浮标的轨迹 |
| BITING | `BUBBLE` + `FISHING` | 咬钩瞬间大量粒子爆发 |
| BOBBING | `BUBBLE`（气泡） | 随机在浮标位置产生 |

### retrieve 逻辑

1. 调用 `level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING)` 获取原版钓鱼战利品表
2. 构造 `LootParams`（含 `ORIGIN`、`TOOL`、`THIS_ENTITY`、`ATTACKING_ENTITY`、luck）
3. 调用 `lootTable.getRandomItems(params)` 获取产出物品列表
4. 每个物品生成 `ItemEntity`，初速度指向 owner 方向（参照原版 `retrieve` 的速度计算）
5. 物品自然飞向渔溪后，由 `GirlfriendEntity` 的 `InventoryCarrier` 接口自动拾取
6. 消耗钓竿 1 点耐久
7. `discard()` 浮标

## FishNearbyWater Behavior

### 行为类型

继承 `net.minecraft.world.entity.ai.behavior.Behavior<GirlfriendEntity>`，支持跨 tick 生命周期。

### 生命周期

```
IDLE
  → tryStart: 冷却=0 & NEAREST_WATER 存在 & 背包有钓竿 & 距水域≤8格
  → start: 装备钓竿到主手 → 面朝水域 → spawn GirlfriendFishingHook
  → canStillUse: hook 存活 & fishingTick≤1200 & 钓竿仍在主手
    → tick: 每 tick 检查 hook.isBiting()
      → true: hook.retrieve(rod) → 物品飞向渔溪 → 物品入背包 → 进入冷却
  → stop: 若 hook 仍存活则 discard，清理状态
```

### 所需记忆

```java
Set.of(GirlfriendsMemoryTypes.NEAREST_WATER.get())
```

### 关键参数

| 参数 | 值 | 说明 |
|---|---|---|
| 最大钓鱼时间 | 1200 tick（60 秒） | 超时放弃当前抛竿，避免卡死 |
| 收竿后冷却 | 100 tick（5 秒） | 与 WaterSensor 的 `ticksToIgnore` 对齐 |
| 作业距离 | 8 格 | 距 `NEAREST_WATER` 记忆位置的最远距离 |
| dayWork priority | 2 | GoToTargetLocation(NEAREST_WATER) priority 1，RandomLookAround priority 3 |

### 钓竿管理

- `start`：遍历背包查找钓竿，找到后与主手物品互换（参照 `PlantAndHarvestFlower.prepareBoneMeal` 的互换模式）
- `canStillUse`：检查主手物品仍为钓竿（`ItemAbilities.FISHING_ROD_CAST`），若钓竿被换走/丢弃则终止
- `stop`：钓竿保留在背包，不丢弃（耐久是消耗品，由 `retrieve` 消耗）
- 若钓竿耐久耗尽破损，`canStillUse` 检测到主手无钓竿后终止，下次 `tryStart` 需背包有另一根钓竿

### YuxiEntity 注册

仅修改 `dayWork` activity：

```java
dayWork.add(
    Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) GoToTargetLocation.create(
        GirlfriendsMemoryTypes.NEAREST_WATER.get(), 4, 0.5F)),
    Pair.of(2, new FishNearbyWater()),
    Pair.of(3, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(
        UniformInt.of(150, 300), 30.0F, -10.0F, 0.0F)),
    Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
);
```

其他 activity（panic / morning / afternoon / sunset / nightRest / follow / core）不变。

## EntityType 注册

在 `GirlfriendsEntities.java` 中新增：

```java
public static final DeferredHolder<EntityType<?>, EntityType<GirlfriendFishingHook>> GIRLFRIEND_FISHING_HOOK =
    REGISTER.register("girlfriend_fishing_hook", () -> EntityType.Builder
        .of(GirlfriendFishingHook::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(64)
        .updateInterval(1)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriend_fishing_hook"))));
```

- `updateInterval(1)`：浮标位置每 tick 变化，需高频同步
- `MobCategory.MISC`：与角色实体一致
- 无需 `EntityAttributeCreationEvent` 注册（非 LivingEntity）

## 客户端渲染

`GirlfriendFishingHookRenderer` 在 `GirlfriendsModClient.onRegisterRenderers` 中注册：

```java
event.registerEntityRenderer(
    GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK.get(),
    GirlfriendFishingHookRenderer::new
);
```

渲染器内部参照原版 `FishingHookRenderer` 实现 bobber 模型渲染，从 `GirlfriendFishingHook` 的 `DATA_BITING` 标志判断浮标下沉状态。

## 约束与边界

1. **服务端权威**：所有钓鱼判定（计时器、粒子触发、战利品表查询、耐久消耗）在服务端执行
2. **物理端隔离**：`GirlfriendFishingHook` 和 `FishNearbyWater` 位于 `common/` 包，不 import `client.*` 类
3. **不可交互**：浮标仅供视觉展示，玩家无法右键收竿（非 Player owner）
4. **仅 dayWork 钓鱼**：跟随模式、恐慌模式、其他时段不执行钓鱼行为
5. **无限水源**：不消耗水源方块，不对水域做修改
6. **依赖 WaterSensor**：`NEAREST_WATER` 记忆由已注册的 `WaterSensor` 提供，无需新增传感器
