# Girlfriends v0.1.0 角色实体与 AI 系统设计

> **设计日期**: 2026-06-07
> **范围**: 分阶段 I — 角色实体 + AI + 数据文件
> **依赖**: Story 1-10 全部完成；Tetrachord-Lib (KD Tree)；Minecraft Sources-26.1.2

## 1. 设计目标

将五位首发角色（沫沫、渔溪、梅疏、晚萤、幽若）作为可交互的 Minecraft 实体加入游戏，每位角色拥有完整的日程 AI、差异化行为逻辑和独立的数据文件，补齐 PRD v0.1.0 的内容闭环。

## 2. 实体类层次结构

### 2.1 抽象基类 `GirlfriendEntity`

```
PathfinderMob
└── GirlfriendEntity (abstract)
    ├── MomoEntity
    ├── YuxiEntity
    ├── MeishuEntity
    ├── WanyingEntity
    └── YouruoEntity
```

**`GirlfriendEntity`** 继承 `net.minecraft.world.entity.PathfinderMob`，提供：

| 职责 | 说明 |
|------|------|
| 角色身份 | 持有 `ResourceLocation girlfriendTypeId`，NBT 持久化 |
| 跟随状态 | `FollowMode` (STAY/FOLLOW/HOME)，`@Nullable UUID followTargetUuid` |
| 交互缓存 | 客户端交互摘要缓存引用 |
| 死亡标记 | `boolean pendingRespawn` |
| 抽象方法 | `getGirlfriendTypeId()` — 由子类返回对应 `GirlfriendTypes.XXX_ID` |
| AI 钩子 | `registerGoals()` — 注册 Vanilla Goal；`createBrain()` — 创建 Brain 实例 |
| 生成钩子 | `getSpawnBiomes()` — 返回允许生成的群系标签 |

**实体属性**：
- 生命值: 40 (×20)
- 移动速度: 0.5
- 护甲: 0 (晚萤通过 AI 获得战斗增益)
- 追踪范围: 48 格
- 更新频率: 3 tick

**NBT 持久化字段**：
- `girlfriend_id`: string — 角色类型 ID
- `follow_mode`: string — 跟随模式
- `follow_target`: uuid (nullable) — 跟随目标玩家

### 2.2 五个子类

| 子类 | girlfriendTypeId | 生成维度 | 生成群系 | 差异化行为 |
|------|-----------------|---------|---------|-----------|
| `MomoEntity` | `girlfriends:momo` | 主世界 | 繁华森林 | 靠近花朵、蜜蜂互动 |
| `YuxiEntity` | `girlfriends:yuxi` | 主世界 | 沙滩 | 面向水域、钓鱼动画 |
| `MeishuEntity` | `girlfriends:meishu` | 主世界 | 尖峭山峰 | 靠近矿石、敲击动画 |
| `WanyingEntity` | `girlfriends:wanying` | 下界 | — | 主动索敌、近战攻击 |
| `YouruoEntity` | `girlfriends:youruo` | 末地 | — | 短距瞬移、末影珍珠投掷 |

## 3. AI 日程系统

### 3.1 Brain 架构

参考 `net.minecraft.world.entity.ai.behavior.VillagerGoalPackages` 和 `net.minecraft.world.entity.ai.sensing.Sensor` 体系。

**核心组件**：

```
GirlfriendAiPackages (工具类，类似 VillagerGoalPackages)
├── coreActivities(GirlfriendEntity)   — 全局行为：躲避危险、睡觉
├── idleActivities(GirlfriendEntity)   — 默认日程行为
├── morningActivities(GirlfriendEntity) — 清晨日程
├── workActivities(GirlfriendEntity)   — 上午/下午工作日程
├── eveningActivities(GirlfriendEntity)— 傍晚日程
├── nightActivities(GirlfriendEntity)  — 夜晚休息日程
```

### 3.2 日程时段定义

| 时段 | 游戏 tick | 活动类型 | 通用行为 | 角色差异化工作 |
|------|----------|---------|---------|-------------|
| 清晨 | 0 ~ 2000 | `MORNING` | 在庇护所附近轻度活动 | 沫沫浇花 / 渔溪检查码头 / 梅疏检查矿道 / 晚萤巡逻 / 幽若检查样本 |
| 上午 | 2000 ~ 6000 | `DAY_WORK` | 核心工作行为 | 沫沫采集花朵+照料蜂箱 / 渔溪垂钓 / 梅疏采矿 / 晚萤战斗训练 / 幽若采集紫颂果 |
| 下午 | 6000 ~ 11000 | `AFTERNOON` | 次要活动 | 沫沫散步观察 / 渔溪整理渔获 / 梅疏冶炼试验 / 晚萤清剿敌对生物 / 幽若实验+记录 |
| 傍晚 | 11000 ~ 13000 | `SUNSET` | 收尾活动 | 沫沫整理干花 / 渔溪看日落 / 梅疏堆放材料 / 晚萤守望 / 幽若观测平台静坐 |
| 夜晚 | 13000 ~ 24000 | `NIGHT_REST` | 庇护所内休息 | 沫沫窗边赏月 / 渔溪看海图 / 梅疏研究样本 / 晚萤篝火旁磨刀 / 幽若整理数据 |

### 3.3 Memory 模块类型

注册专用 `MemoryModuleType`：
- `SCHEDULE` — 当前日程时段枚举
- `MEETING_POINT` — 庇护所位置 `GlobalPos`
- `LOOK_TARGET` — 注视目标
- `WALK_TARGET` — 移动目标 `BlockPos`
- `NEAREST_VISIBLE_PLAYER` — 最近可见玩家 `Player`
- 可选传感器记忆：`NEARBY_FLOWERS`、`NEARBY_WATER`、`NEARBY_ORES`、`NEARBY_HOSTILE` 等

### 3.4 Sensor 注册

**通用 Sensor**：
- `NearestVisiblePlayerSensor` — 检测附近玩家
- `GirlfriendShelterSensor` — 检测庇护所位置

**角色专属 Sensor**（举例）：
- `FlowerSensor` (沫沫) — 检测 8 格内花朵方块
- `WaterSensor` (渔溪) — 检测 12 格内水域
- `OreSensor` (梅疏) — 检测 8 格内矿石
- `HostileSensor` (晚萤) — 检测 16 格内敌对生物
- `PearlSensor` (幽若) — 检测末影珍珠落点

### 3.5 日程切换逻辑

每 20 tick 检查当前游戏时间：
1. 确定当前时段
2. 若 Brain 中 `SCHEDULE` 与当前时段不同，清除活动队列并切换到对应的 `BehaviorControl` 包
3. Brain 按优先级顺序选择第一个满足条件的 behavior 执行

## 4. 实体注册

### 4.1 注册类 `GirlfriendsEntities`

```java
public final class GirlfriendsEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, GirlfriendsMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MomoEntity>> MOMO =
        REGISTER.register("momo", () -> EntityType.Builder
            .of(MomoEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(48)
            .updateInterval(3)
            .build("momo"));

    // 同理注册 YUXI, MEISHU, WANYING, YOURUO
}
```

### 4.2 生成蛋

为每位角色注册 SpawnEggItem，使用各自的主题色：
- 沫沫：鹅黄 #F5E6A3
- 渔溪：海蓝 #5B8FA8
- 梅疏：铁灰 #8B8B8B
- 晚萤：暗红 #A0522D
- 幽若：深紫 #6A0DAD

## 5. 渲染系统

### 5.1 模型

使用原版 `HumanoidModel<GirlfriendEntity>`，参考 `net.minecraft.client.model.PlayerModel` 实现。

- 所有角色使用统一的 `HumanoidModel`，通过纹理区分
- 默认动画：行走时四肢摆动、注视玩家时头部跟踪
- 角色特有动画（后续扩展）：幽若瞬移粒子、晚萤攻击挥剑、梅疏敲击动作

### 5.2 渲染器

`GirlfriendRenderer extends HumanoidMobRenderer<GirlfriendEntity, HumanoidModel<GirlfriendEntity>>`

- 材质路径：`assets/girlfriends/textures/entity/<id>/<id>.png`
- 模型层注册：通过 `GirlfriendsModelLayers` 创建五个 `ModelLayerLocation`
- 渲染缩放：1.0（与玩家等大）

### 5.3 现有纹理资源

所有 5 张 PNG 皮肤已上传：
- `src/main/resources/assets/girlfriends/textures/entity/momo/momo.png`
- `src/main/resources/assets/girlfriends/textures/entity/yuxi/yuxi.png`
- `src/main/resources/assets/girlfriends/textures/entity/meishu/meishu.png`
- `src/main/resources/assets/girlfriends/textures/entity/wanying/wanying.png`
- `src/main/resources/assets/girlfriends/textures/entity/youruo/youruo.png`

## 6. 生成与庇护所

### 6.1 技术方块实体 `ShelterStructureBlockEntity`

```java
public class ShelterStructureBlockEntity extends BlockEntity {
    ResourceLocation characterId;       // 对应角色类型
    GlobalPos shelterPos;               // 庇护所全局坐标
    long generatedDay;                  // 生成日
    boolean availableForRespawn;        // 重生可用
    boolean active;                     // 是否已激活
}
```

### 6.2 控制方块 `ShelterControllerBlock`

- 不可见且不可破坏的技术性方块
- 作为庇护所结构的中心锚点
- 持有 `ShelterStructureBlockEntity`

### 6.3 生成流程

区块首次加载 → `ShelterStructureBlockEntity#onLoad()`:
1. 查询 `GirlfriendsWorldData` 中该角色 `CharacterWorldState`
2. 若 `alive = true` 且 `entity_uuid` 对应的实体存在 → 跳过
3. 否则在 `shelterPos` 上方生成角色实体
4. 更新 `CharacterWorldState.entity_uuid`、`alive = true`、`pending_respawn = false`
5. 注册庇护所记录 `CharacterRespawnService.registerShelter(...)`
6. 将位置插入该角色的 KD Tree

### 6.4 KD Tree 最近庇护所搜索

参考 `MISCTWFSavedData.java` 的 KD Tree 管理模式喵~

**数据结构**：在 `GirlfriendsWorldData` 中维护：
```java
// 按维度分组的 KD Tree，每棵树存储庇护所 BlockPos
private final Map<ResourceLocation, KDTree<BlockPos, Integer>> shelterKDTreesByDimension;

// KD Tree 工厂函数
private static final Function<ResourceLocation, KDTree<BlockPos, Integer>> KD_TREE_COMPUTE =
    k -> KDTree.newLinkedKDTree(3);
```

**坐标转换**：使用 TetrachordLib 的 `MDUtils.vec3i(BlockPos)` 将 `BlockPos` 转换为 `IMultidimensional<Integer>`（3 维：x, y, z），进行插入、删除和最近邻查询喵~

**插入**：
```java
shelterKDTreesByDimension.computeIfAbsent(dimension, KD_TREE_COMPUTE)
    .insert(KDTree.BuildNode.of(blockPos, MDUtils.vec3i(blockPos)));
```

**最近邻查询**：
```java
KDTree<BlockPos, Integer> tree = shelterKDTreesByDimension.get(dimension);
if (tree != null && !tree.isEmpty()) {
    KDNode<BlockPos, Integer> nearest = tree.findClosest(MDUtils.vec3i(targetPos));
    return nearest.other(); // BlockPos 本身
}
```

**序列化/反序列化**：参照 `MISCTWFSavedData` 模式 — 按维度分组存储为 `CompoundTag`，每个维度下是 `ListTag`，每条记录存储 `BlockPos.asLong()`；反序列化时通过 `KDTree.build(BuildNode[])` 从已有数据重建树喵~

**查询复杂度**：`findClosest` 操作 O(log n)，适用于庇护所数量在百级以内的场景；若未来单角色庇护所超过 1000 个，可增加按区块分桶索引优化喵~

## 7. 数据文件

### 7.1 礼物偏好 JSON

路径：`data/girlfriends/girlfriends/gift_preferences/<character_id>.json`

按 PRD + GDD 定义每角色 4 档偏好（最喜好 + 喜好 + 普通接受 + 厌恶）。

### 7.2 固定委托定义 JSON

路径：`data/girlfriends/girlfriends/fixed_quests/<character_id>/<quest_id>.json`

每角色 10 条固定委托，包含阶段要求、触发条件、多目标拆分（使用已实现的 `QuestObjectiveHandler` 通用类型）、奖励和协助规则。

### 7.3 随机委托模板 JSON

路径：`data/girlfriends/girlfriends/random_quest_templates/<character_id>/<template_id>.json`

每角色 8 类随机委托模板，包含解锁阶段、目标类型、过期天数范围、基础好感和资源奖励。

### 7.4 修复现有 bug

`GirlfriendTypes.java` 中的以下字段与 PRD 不符，需要修正：

| 角色 | 字段 | 当前值（错误） | 正确值 |
|------|------|-------------|--------|
| 渔溪 | favoriteGiftItem | `amethyst_shard` | `nautilus_shell` |
| 梅疏 | favoriteGiftItem | `painting` | `iron_ingot` |
| 晚萤 | favoriteGiftItem | `iron_sword` | `blaze_rod` |
| 幽若 | favoriteGiftItem | `heart_of_the_sea` | `ender_pearl` |
| 晚萤 | dimensionPolicy | `OVERWORLD` | `NETHER` |
| 幽若 | dimensionPolicy | `OVERWORLD` | `END` |

## 8. 依赖引入

### 8.1 Tetrachord-Lib

`build.gradle` 添加：
```groovy
repositories {
    mavenLocal() // 或 flatDir
}
dependencies {
    implementation 'com.hexagram2021.tetrachordlib:tetrachordlib:26.1+1.0.3'
}
```

## 9. 包结构增补

```
src/main/java/com/hexagram2021/girlfriends/
    common/
        entity/
            GirlfriendEntity.java (abstract)
            MomoEntity.java
            YuxiEntity.java
            MeishuEntity.java
            WanyingEntity.java
            YouruoEntity.java
            ai/
                GirlfriendsMemoryTypes.java
                GirlfriendsSensorTypes.java
                GirlfriendsScheduleTypes.java
                sensor/
                    GirlfriendShelterSensor.java
                    FlowerSensor.java
                    WaterSensor.java
                    OreSensor.java
                    HostileSensor.java
                    PearlSensor.java
                behavior/
                    GirlfriendAiPackages.java
                    MoveToShelterBehavior.java
                    FollowPlayerBehavior.java
                    ...
            spawn/
                ShelterStructureBlockEntity.java
                ShelterControllerBlock.java
                ShelterSpawnHandler.java
    client/
        model/
            GirlfriendsModelLayers.java
        renderer/
            GirlfriendRenderer.java
        ...

src/main/resources/
    data/girlfriends/girlfriends/
        gift_preferences/
            momo.json
            yuxi.json
            meishu.json
            wanying.json
            youruo.json
        fixed_quests/
            momo/ (10 files)
            yuxi/ (10 files)
            meishu/ (10 files)
            wanying/ (10 files)
            youruo/ (10 files)
        random_quest_templates/
            momo/ (8+ files)
            yuxi/ (8+ files)
            meishu/ (8+ files)
            wanying/ (8+ files)
            youruo/ (8+ files)
```

## 10. Story 拆分

| Story | 名称 | 内容 | 验收 |
|-------|------|------|------|
| 11 | 沫沫实体 + AI + 数据 | `MomoEntity`、FlowerSensor、日程 AI、10条固定委托、8类随机模板、礼物偏好 | 沫沫在繁华森林生成、执行日程、接受赠礼判定 |
| 12 | 渔溪实体 + AI + 数据 | `YuxiEntity`、WaterSensor、日程 AI、10条固定委托、8类随机模板、礼物偏好 | 渔溪在沙滩生成、面向水域、日程行为正确 |
| 13 | 梅疏实体 + AI + 数据 | `MeishuEntity`、OreSensor、日程 AI、10条固定委托、8类随机模板、礼物偏好 | 梅疏在尖峭山峰生成、矿井行为、日程行为正确 |
| 14 | 晚萤实体 + AI + 数据 | `WanyingEntity`、HostileSensor+主动攻击、日程 AI、10条固定委托、8类随机模板、礼物偏好 | 晚萤在下界生成、主动索敌、战斗日程正确 |
| 15 | 幽若实体 + AI + 数据 | `YouruoEntity`、PearlSensor+短距瞬移、日程 AI、10条固定委托、8类随机模板、礼物偏好 | 幽若在末地生成、瞬移行为、研究日程正确 |
| 16 | 庇护所结构生成 + 集成 | `ShelterStructureBlockEntity`、`ShelterControllerBlock`、KD Tree 集成、实体生成与重生流程 | 庇护所生成时角色出现在旁、死亡后从最近庇护所重生 |

每完成一个 Story 需通过 `./gradlew test` 确认无回归问题喵~

## 11. 风险与约束

1. **Brain 系统复杂度**：村民 Brain 体系代码量大，建议先参考 `VillagerGoalPackages` 提取最简核心，再逐步丰富
2. **跨维度实体管理**：晚萤（下界）和幽若（末地）的实体加载需考虑玩家跨维度旅行时的实体生命周期
3. **KD Tree 线程安全**：Shelter KD Tree 仅在服务端使用，需确保插入/查询都在服务端线程
4. **皮肤纹理兼容性**：已确认五位角色 PNG 纹理与玩家模型 UV 映射格式兼容，可直接复用/继承 `PlayerModel` 实现喵~
5. **数据文件完整性**：50 条固定委托的 JSON 编写量大，需严格按 GDD 中的多目标拆分逐条落地

## 12. 参考来源

- `docs/v0.1.0/PRD.md` — 产品需求文档
- `docs/v0.1.0/DR_system.md` — 底层系统技术设计
- `docs/v0.1.0/GDD/` — 五位角色游戏设计文档
- `docs/v0.1.0/PLAN_system.md` — 底层系统实现计划
- `net.minecraft.world.entity.npc.Villager` — Brain 体系参考
- `net.minecraft.world.entity.ai.behavior.VillagerGoalPackages` — 日程 AI 参考
- `D:\Projects\Tetrachord-Lib` — KD Tree 数据结构依赖
- `D:\Projects\Sources-26.1.2` — Minecraft 源码参考
