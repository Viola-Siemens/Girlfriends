# 角色实体与 AI 系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将五位首发角色作为可交互的 Minecraft 实体加入游戏，每位角色拥有完整的日程 AI、差异化传感器和独立的数据文件（礼物偏好、固定委托、随机委托模板）

**Architecture:** 抽象基类 `GirlfriendEntity extends PathfinderMob` 统一管理身份、跟随和持久化；五个子类覆写 AI 日程；基于原版 Villager Brain + Behavior 体系实现分时段日程；`ShelterStructureBlockEntity` 技术方块控制首次生成；TetrachordLib KD Tree 支持 O(log n) 最近庇护所查找

**Tech Stack:** Java 25、Minecraft 26.1.2、NeoForge 26.1.2.71、TetrachordLib 26.1+1.0.3、Brain/Memory/Sensor 体系、HumanoidMobRenderer

---

## 文件结构映射

| 职责 | 文件 | 层 |
|------|------|-----|
| 抽象基类 | `common/entity/GirlfriendEntity.java` | 领域实体 |
| 实体注册 | `common/entity/GirlfriendsEntities.java` | Minecraft 集成 |
| 记忆类型 | `common/entity/ai/GirlfriendsMemoryTypes.java` | AI 基础设施 |
| 传感器类型 | `common/entity/ai/GirlfriendsSensorTypes.java` | AI 基础设施 |
| 日程类型 | `common/entity/ai/GirlfriendsScheduleTypes.java` | AI 基础设施 |
| AI 行为包 | `common/entity/ai/behavior/GirlfriendAiPackages.java` | AI 编排 |
| 庇护所传感器 | `common/entity/ai/sensor/GirlfriendShelterSensor.java` | 共用 AI |
| 跟随行为 | `common/entity/ai/behavior/FollowPlayerBehavior.java` | 共用 AI |
| 庇护所方块实体 | `common/entity/spawn/ShelterStructureBlockEntity.java` | 生成 |
| 控制方块 | `common/entity/spawn/ShelterControllerBlock.java` | 生成 |
| 生成处理 | `common/entity/spawn/ShelterSpawnHandler.java` | 生成 |
| 模型层注册 | `client/model/GirlfriendsModelLayers.java` | 表现层 |
| 渲染器 | `client/renderer/GirlfriendRenderer.java` | 表现层 |
| 客户端入口 | `client/GirlfriendsModClient.java` (修改) | 表现层 |
| 模组主类 | `GirlfriendsMod.java` (修改) | 集成入口 |
| 角色类型 | `common/character/GirlfriendTypes.java` (修改) | Bug 修复 |

| 子类 | 实体文件 | 专属 Sensor | 数据文件(×19) |
|------|---------|------------|--------------|
| MomoEntity | `common/entity/MomoEntity.java` | `FlowerSensor.java` | gift + 10 fixed + 8 random |
| YuxiEntity | `common/entity/YuxiEntity.java` | `WaterSensor.java` | gift + 10 fixed + 8 random |
| MeishuEntity | `common/entity/MeishuEntity.java` | `OreSensor.java` | gift + 10 fixed + 8 random |
| WanyingEntity | `common/entity/WanyingEntity.java` | `HostileSensor.java` | gift + 10 fixed + 8 random |
| YouruoEntity | `common/entity/YouruoEntity.java` | `PearlSensor.java` | gift + 10 fixed + 8 random |

---

### Task 0: 前置修正与依赖引入

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/character/GirlfriendTypes.java`

- [ ] **Step 0.1: 添加 Tetrachord-Lib 依赖**

在 `build.gradle` 的 `repositories` 块中添加 mavenLocal，并在 `dependencies` 中添加 implementation：

```groovy
repositories {
    mavenLocal()
}
dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    implementation 'com.hexagram2021.tetrachordlib:tetrachordlib:26.1+1.0.3'

    testImplementation 'org.junit.platform:junit-platform-console-standalone:1.9.2'
}
```

- [ ] **Step 0.2: 修复 GirlfriendTypes.java 中的 favoriteGiftItem bug**

修改渔溪、梅疏、晚萤、幽若的 `favoriteGiftItem`：

```java
// 渔溪：amethyst_shard → nautilus_shell
Identifier.withDefaultNamespace("nautilus_shell"),
// 梅疏：painting → iron_ingot
Identifier.withDefaultNamespace("iron_ingot"),
// 晚萤：iron_sword → blaze_rod
Identifier.withDefaultNamespace("blaze_rod"),
// 幽若：heart_of_the_sea → ender_pearl
Identifier.withDefaultNamespace("ender_pearl"),
```

- [ ] **Step 0.3: 修复 GirlfriendTypes.java 中的 dimensionPolicy bug**

修改晚萤和幽若的维度策略：

```java
// 晚萤：Set.of(Level.OVERWORLD.identifier()) → 下界
new DimensionPolicy(Set.of(Level.NETHER.identifier())),
// 幽若：Set.of(Level.OVERWORLD.identifier()) → 末地
new DimensionPolicy(Set.of(Level.END.identifier())),
```

- [ ] **Step 0.4: 运行测试验证无回归**

```bash
./gradlew test
```
期望：所有已存在测试通过，BUILD SUCCESSFUL

- [ ] **Step 0.5: 提交**

```bash
git add build.gradle src/main/java/com/hexagram2021/girlfriends/common/character/GirlfriendTypes.java
git commit -m "fix(REQ-7): 修复 GirlfriendTypes 中的 favoriteGiftItem 和 dimensionPolicy 错误，添加 Tetrachord-Lib 依赖"
```

---

### Task 1: AI 基础设施 — Memory / Sensor / Schedule 类型

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/GirlfriendsMemoryTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/GirlfriendsSensorTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/GirlfriendsScheduleTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/package-info.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/package-info.java`

- [ ] **Step 1.1: 创建 package-info.java**

```java
// common/entity/package-info.java
/**
 * 角色实体与 AI 系统包喵~
 *
 * @author liudongyu
 */
package com.hexagram2021.girlfriends.common.entity;
```

```java
// common/entity/ai/package-info.java
/**
 * 角色 AI 系统包 — 日程 Brain、Sensor、Behavior 与记忆类型喵~
 *
 * @author liudongyu
 */
package com.hexagram2021.girlfriends.common.entity.ai;
```

- [ ] **Step 1.2: 创建日程时段枚举**

```java
// common/entity/ai/GirlfriendsScheduleTypes.java
package com.hexagram2021.girlfriends.common.entity.ai;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * 角色日程时段定义喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsScheduleTypes {
	/** 清晨 (0~2000 tick) 喵~ */
	public static final int MORNING = 0;
	/** 上午工作时间 (2000~6000 tick) 喵~ */
	public static final int DAY_WORK = 1;
	/** 下午活动时间 (6000~11000 tick) 喵~ */
	public static final int AFTERNOON = 2;
	/** 傍晚收尾 (11000~13000 tick) 喵~ */
	public static final int SUNSET = 3;
	/** 夜晚休息 (13000~24000 tick) 喵~ */
	public static final int NIGHT_REST = 4;

	/** 日程名称查找表喵~ */
	public static final Int2ObjectMap<String> SCHEDULE_NAMES = new Int2ObjectOpenHashMap<>();

	static {
		SCHEDULE_NAMES.put(MORNING, "morning");
		SCHEDULE_NAMES.put(DAY_WORK, "day_work");
		SCHEDULE_NAMES.put(AFTERNOON, "afternoon");
		SCHEDULE_NAMES.put(SUNSET, "sunset");
		SCHEDULE_NAMES.put(NIGHT_REST, "night_rest");
	}

	/**
	 * 根据游戏 tick 获取当前时段喵~
	 *
	 * @param dayTime 当日游戏时间 (0~24000) 喵~
	 * @return 时段常量喵~
	 */
	public static int getSchedule(long dayTime) {
		if (dayTime < 2000) {
			return MORNING;
		} else if (dayTime < 6000) {
			return DAY_WORK;
		} else if (dayTime < 11000) {
			return AFTERNOON;
		} else if (dayTime < 13000) {
			return SUNSET;
		}
		return NIGHT_REST;
	}

	private GirlfriendsScheduleTypes() {
	}
}
```

- [ ] **Step 1.3: 创建 MemoryModuleType 注册**

```java
// common/entity/ai/GirlfriendsMemoryTypes.java
package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

/**
 * 角色 AI 记忆模块类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsMemoryTypes {
	public static final DeferredRegister<MemoryModuleType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, GirlfriendsMod.MODID);

	/** 当前日程时段 (Integer) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> CURRENT_SCHEDULE =
			REGISTER.register("current_schedule", () -> new MemoryModuleType<>(Optional.empty()));

	/** 庇护所位置 (GlobalPos) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> MEETING_POINT =
			REGISTER.register("meeting_point", () -> new MemoryModuleType<>(Optional.empty()));

	/** 移动目标 (BlockPos) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> WALK_TARGET =
			REGISTER.register("walk_target", () -> new MemoryModuleType<>(Optional.empty()));

	/** 最近可见玩家 (Player) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Player>> NEAREST_VISIBLE_PLAYER =
			REGISTER.register("nearest_visible_player", () -> new MemoryModuleType<>(Optional.empty()));

	/** 附近花朵位置列表 (List<BlockPos>) — 沫沫专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<BlockPos>>> NEARBY_FLOWERS =
			REGISTER.register("nearby_flowers", () -> new MemoryModuleType<>(Optional.of(List.of())));

	/** 附近水域位置 (GlobalPos) — 渔溪专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> NEAREST_WATER =
			REGISTER.register("nearest_water", () -> new MemoryModuleType<>(Optional.empty()));

	/** 附近可挖掘矿石 (List<BlockPos>) — 梅疏专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<BlockPos>>> NEARBY_ORES =
			REGISTER.register("nearby_ores", () -> new MemoryModuleType<>(Optional.of(List.of())));

	/** 附近敌对生物 (LivingEntity) — 晚萤专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<LivingEntity>> NEAREST_HOSTILE =
			REGISTER.register("nearest_hostile", () -> new MemoryModuleType<>(Optional.empty()));

	/** 末影珍珠瞬移目标 (BlockPos) — 幽若专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> PEARL_TARGET =
			REGISTER.register("pearl_target", () -> new MemoryModuleType<>(Optional.empty()));

	private GirlfriendsMemoryTypes() {
	}
}
```

- [ ] **Step 1.4: 创建 SensorType 注册**

```java
// common/entity/ai/GirlfriendsSensorTypes.java
package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.ai.sensor.GirlfriendShelterSensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 角色 AI 传感器类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsSensorTypes {
	public static final DeferredRegister<SensorType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, GirlfriendsMod.MODID);

	/** 庇护所位置传感器喵~ */
	public static final DeferredHolder<SensorType<?>, SensorType<GirlfriendShelterSensor>> SHELTER_SENSOR =
			REGISTER.register("shelter_sensor", () -> new SensorType<>(GirlfriendShelterSensor::new));

	private GirlfriendsSensorTypes() {
	}
}
```

角色专属 Sensor（如 `FlowerSensor`、`WaterSensor` 等）在各自的 Story 中注册，避免在此阶段引入未定义的实体类喵~

- [ ] **Step 1.5: 编译验证**

```bash
./gradlew classes
```
期望：COMPILE SUCCESS

---

### Task 2: GirlfriendEntity 抽象基类

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendEntity.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendsEntities.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java`

- [ ] **Step 2.1: 创建 GirlfriendEntity 抽象基类**

```java
// common/entity/GirlfriendEntity.java
package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * 角色实体抽象基类喵~
 * <p>
 * 统一管理角色身份、跟随状态、AI 日程切换和持久化喵~
 * 子类通过覆写 {@link #getGirlfriendTypeId()} 返回对应角色类型 ID，
 * 并注册差异化的 Brain、Sensor 和 Behavior 喵~
 *
 * @author liudongyu
 */
public abstract class GirlfriendEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_FOLLOW_MODE =
			SynchedEntityData.defineId(GirlfriendEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<UUID>> DATA_FOLLOW_TARGET =
			SynchedEntityData.defineId(GirlfriendEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private static final String TAG_GIRLFRIEND_ID = "girlfriend_id";
	private static final String TAG_FOLLOW_MODE = "follow_mode";
	private static final String TAG_FOLLOW_TARGET = "follow_target";

	protected GirlfriendEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * 获取该实体的角色类型 ID 喵~
	 *
	 * @return 角色类型注册表 key 喵~
	 */
	public abstract ResourceLocation getGirlfriendTypeId();

	/**
	 * 解析角色类型对象喵~
	 *
	 * @return 角色类型，若注册表缺失则返回 null 喵~
	 */
	@Nullable
	public GirlfriendType getGirlfriendType() {
		return this.registryAccess()
				.registryOrThrow(GirlfriendsRegistries.GIRLFRIEND_TYPE_KEY)
				.get(this.getGirlfriendTypeId());
	}

	/**
	 * 获取当前跟随模式喵~
	 *
	 * @return 跟随模式喵~
	 */
	public FollowMode getFollowMode() {
		return FollowMode.fromId(this.entityData.get(DATA_FOLLOW_MODE));
	}

	/**
	 * 设置跟随模式喵~
	 *
	 * @param mode 新的跟随模式喵~
	 */
	public void setFollowMode(FollowMode mode) {
		this.entityData.set(DATA_FOLLOW_MODE, mode.ordinal());
	}

	/**
	 * 获取跟随目标玩家 UUID 喵~
	 *
	 * @return 跟随目标 UUID，可能为空喵~
	 */
	@Nullable
	public UUID getFollowTargetUuid() {
		return this.entityData.get(DATA_FOLLOW_TARGET).orElse(null);
	}

	/**
	 * 设置跟随目标玩家 UUID 喵~
	 *
	 * @param uuid 目标玩家 UUID，可为 null 喵~
	 */
	public void setFollowTargetUuid(@Nullable UUID uuid) {
		this.entityData.set(DATA_FOLLOW_TARGET, Optional.ofNullable(uuid));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FOLLOW_MODE, FollowMode.STAY.ordinal());
		builder.define(DATA_FOLLOW_TARGET, Optional.empty());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString(TAG_GIRLFRIEND_ID, this.getGirlfriendTypeId().toString());
		tag.putString(TAG_FOLLOW_MODE, this.getFollowMode().name());
		if (this.getFollowTargetUuid() != null) {
			tag.putUUID(TAG_FOLLOW_TARGET, this.getFollowTargetUuid());
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains(TAG_FOLLOW_MODE)) {
			this.setFollowMode(FollowMode.valueOf(tag.getString(TAG_FOLLOW_MODE)));
		}
		if (tag.hasUUID(TAG_FOLLOW_TARGET)) {
			this.setFollowTargetUuid(tag.getUUID(TAG_FOLLOW_TARGET));
		}
	}

	@Override
	protected Brain.Provider<?> brainProvider() {
		return Brain.provider(GirlfriendsMemoryTypes.REGISTER.getEntries().stream()
						.filter(h -> h.getId().getNamespace().equals("girlfriends"))
						.map(h -> (MemoryModuleType<?>)h.value())
						.toList(),
				com.google.common.collect.ImmutableList.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Brain<?> makeBrain(net.minecraft.world.entity.ai.Brain dynamic) {
		return dynamic;
	}
}
```

- [ ] **Step 2.2: 创建实体注册类（骨架）**

```java
// common/entity/GirlfriendsEntities.java
package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 角色实体类型注册表喵~
 * <p>
 * 各角色实体在对应 Story 中注册: Story 11—沫沫, Story 12—渔溪, 以此类推喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsEntities {
	public static final DeferredRegister<EntityType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, GirlfriendsMod.MODID);

	private GirlfriendsEntities() {
	}
}
```

- [ ] **Step 2.3: 在 GirlfriendsMod 中注册实体和 AI 类型**

修改 `GirlfriendsMod.java` 构造函数，添加：

```java
// 在构造函数中添加
GirlfriendsEntities.REGISTER.register(modEventBus);
GirlfriendsMemoryTypes.REGISTER.register(modEventBus);
GirlfriendsSensorTypes.REGISTER.register(modEventBus);
```

- [ ] **Step 2.4: 编译验证**

```bash
./gradlew classes
```
期望：COMPILE SUCCESS

---

### Task 3: 客户端渲染器

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/client/model/GirlfriendsModelLayers.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/client/renderer/GirlfriendRenderer.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java`

- [ ] **Step 3.1: 创建模型层注册**

```java
// client/model/GirlfriendsModelLayers.java
package com.hexagram2021.girlfriends.client.model;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/**
 * 角色实体模型层注册喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsModelLayers {
	public static final ModelLayerLocation MOMO =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo"), "main");
	public static final ModelLayerLocation YUXI =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi"), "main");
	public static final ModelLayerLocation MEISHU =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu"), "main");
	public static final ModelLayerLocation WANYING =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying"), "main");
	public static final ModelLayerLocation YOURUO =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo"), "main");

	private GirlfriendsModelLayers() {
	}
}
```

- [ ] **Step 3.2: 创建统一渲染器**

```java
// client/renderer/GirlfriendRenderer.java
package com.hexagram2021.girlfriends.client.renderer;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.client.model.GirlfriendsModelLayers;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;

/**
 * 角色实体渲染器喵~
 * <p>
 * 统一使用原版 HumanoidModel（参考 PlayerModel），
 * 通过纹理路径区分五位角色喵~
 *
 * @author liudongyu
 */
public class GirlfriendRenderer extends HumanoidMobRenderer<GirlfriendEntity, HumanoidModel<GirlfriendEntity>> {
	private final Identifier textureLocation;

	/**
	 * 创建渲染器喵~
	 *
	 * @param context    渲染上下文喵~
	 * @param characterId 角色 ID（用于纹理路径）喵~
	 */
	public GirlfriendRenderer(EntityRendererProvider.Context context, String characterId) {
		super(context,
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
				0.5f);
		this.textureLocation = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID,
				"textures/entity/" + characterId + "/" + characterId + ".png");
		this.addLayer(new HumanoidArmorLayer<>(this,
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
				context.getEquipmentRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(GirlfriendEntity entity) {
		return this.textureLocation;
	}
}
```

- [ ] **Step 3.3: 在客户端入口注册渲染器**

在 `GirlfriendsModClient` 中（客户端初始化回调），添加渲染器注册。由于实体类尚未全部定义，在此先添加框架注释，Story 11 起逐角色注册：

```java
// 当实体类定义后，逐角色注册：
// event.registerEntityRenderer(GirlfriendsEntities.MOMO.get(),
//     ctx -> new GirlfriendRenderer(ctx, "momo"));
```

- [ ] **Step 3.4: 编译验证**

```bash
./gradlew classes
```
期望：COMPILE SUCCESS

---

### Task 4: 共用 AI 组件 — ShelterSensor 与 FollowBehavior

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/sensor/GirlfriendShelterSensor.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/sensor/package-info.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/behavior/GirlfriendAiPackages.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/behavior/package-info.java`

- [ ] **Step 4.1: 创建庇护所传感器**

```java
// common/entity/ai/sensor/GirlfriendShelterSensor.java
package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Set;

/**
 * 庇护所位置传感器喵~
 * <p>
 * 从 WorldData 中查找角色已发现的庇护所记录，
 * 将最近的可用庇护所位置写入 MEETING_POINT 记忆喵~
 *
 * @author liudongyu
 */
public class GirlfriendShelterSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RATE = 200; // 每 200 tick 扫描一次喵~

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(GirlfriendsMemoryTypes.MEETING_POINT.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		GirlfriendsWorldData worldData = GirlfriendsWorldData.getInstance(level);
		if (worldData == null) {
			return;
		}
		// 查找该角色的已发现庇护所
		worldData.findNearestAvailableShelter(entity.getGirlfriendTypeId(), level.dimension().location(),
				entity.blockPosition()).ifPresent(shelter ->
				entity.getBrain().setMemory(GirlfriendsMemoryTypes.MEETING_POINT.get(),
						GlobalPos.of(level.dimension(), shelter.pos())));
	}
}
```

- [ ] **Step 4.2: 创建 AI 行为包工具类**

```java
// common/entity/ai/behavior/GirlfriendAiPackages.java
package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsScheduleTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

/**
 * 角色 AI 行为包工具类喵~
 * <p>
 * 类似原版 VillagerGoalPackages，为 Brain 提供不同时段的行为编排喵~
 * 子类覆写 workActivities() 等方法注册角色专属日程行为喵~
 *
 * @author liudongyu
 */
public final class GirlfriendAiPackages {
	private GirlfriendAiPackages() {
	}

	/**
	 * 核心行为包 — 始终生效的生存行为喵~
	 *
	 * @param entity 角色实体喵~
	 * @return 核心行为对喵~
	 */
	public static List<Pair<Integer, ? extends BehaviorControl<? super GirlfriendEntity>>> coreActivities(
			GirlfriendEntity entity) {
		return ImmutableList.of(
				Pair.of(0, new Swim(0.8f))
		);
	}

	/**
	 * 默认闲置行为包 — 无特定时段时的回退行为喵~
	 *
	 * @param entity 角色实体喵~
	 * @return 闲置行为对喵~
	 */
	public static List<Pair<Integer, ? extends BehaviorControl<? super GirlfriendEntity>>> idleActivities(
			GirlfriendEntity entity) {
		return ImmutableList.of(
				Pair.of(0, new RandomStroll()),
				Pair.of(1, new SetEntityLookTarget()),
				Pair.of(2, new DoNothing(30, 60))
		);
	}

	/**
	 * 将当前游戏时间对应的时段写入 Brain 喵~
	 *
	 * @param entity 角色实体喵~
	 * @return 时段检查行为喵~
	 */
	public static BehaviorControl<GirlfriendEntity> updateSchedule(GirlfriendEntity entity) {
		return new Behavior<>(
				Map.of(),
				() -> {
					if (entity.level() instanceof ServerLevel) {
						long dayTime = entity.level().getDayTime() % 24000;
						int schedule = GirlfriendsScheduleTypes.getSchedule(dayTime);
						entity.getBrain().setMemory(GirlfriendsMemoryTypes.CURRENT_SCHEDULE.get(), schedule);
					}
				}
		);
	}

	/**
	 * 根据当前时段选择对应的 Activity 喵~
	 * 子类应覆写以下方法来注册角色专属的日程行为包喵~
	 *
	 * @param entity 角色实体喵~
	 * @return 所有时段的 Activity 行为包喵~
	 */
	public static List<Pair<Activity, List<Pair<Integer, ? extends BehaviorControl<? super GirlfriendEntity>>>>>
			allScheduleActivities(GirlfriendEntity entity) {
		return ImmutableList.of(
				Pair.of(Activity.IDLE, idleActivities(entity)),
				Pair.of(Activity.CORE, coreActivities(entity))
		);
	}
}
```

- [ ] **Step 4.3: 编译验证**

```bash
./gradlew classes
```
期望：COMPILE SUCCESS

---

### Task 5: Story 11 — 沫沫（momo）实体 + AI + 数据文件

这是第一个完整角色实现，后续角色的模式与此相同喵~

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/MomoEntity.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/sensor/FlowerSensor.java`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_preferences/momo.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/fixed_quests/momo/` (10 files)
- Create: `src/main/resources/data/girlfriends/girlfriends/random_quest_templates/momo/` (8 files)
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendsEntities.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java`

- [ ] **Step 5.1: 注册 EntityType 到 GirlfriendsEntities**

在 `GirlfriendsEntities.java` 中添加：

```java
public static final DeferredHolder<EntityType<?>, EntityType<MomoEntity>> MOMO =
		REGISTER.register("momo", () -> EntityType.Builder
				.of(MomoEntity::new, MobCategory.MISC)
				.sized(0.6f, 1.8f)
				.clientTrackingRange(48)
				.updateInterval(3)
				.build("momo"));
```

- [ ] **Step 5.2: 创建 MomoEntity**

```java
// common/entity/MomoEntity.java
package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.GirlfriendAiPackages;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * 沫沫实体喵~
 * <p>
 * 热爱花草与蜜蜂的女孩，生成于繁华森林喵~
 * 日程：清晨浇花 → 上午采集花朵+照料蜂箱 → 下午散步 → 傍晚整理干花 → 夜晚赏月喵~
 *
 * @author liudongyu
 */
public class MomoEntity extends GirlfriendEntity {
	public MomoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public ResourceLocation getGirlfriendTypeId() {
		return GirlfriendTypes.MOMO_ID;
	}

	@Override
	protected Brain.Provider<MomoEntity> brainProvider() {
		return Brain.provider(
				List.of(
						GirlfriendsMemoryTypes.CURRENT_SCHEDULE.get(),
						GirlfriendsMemoryTypes.MEETING_POINT.get(),
						GirlfriendsMemoryTypes.WALK_TARGET.get(),
						GirlfriendsMemoryTypes.NEAREST_VISIBLE_PLAYER.get(),
						GirlfriendsMemoryTypes.NEARBY_FLOWERS.get()
				),
				List.of(
						GirlfriendsSensorTypes.SHELTER_SENSOR.get()
				)
		);
	}

	@Override
	protected Brain<?> makeBrain(net.minecraft.world.entity.ai.Brain dynamic) {
		Brain<MomoEntity> brain = (Brain<MomoEntity>) dynamic;
		// 注册所有 Activity 的行为包喵~
		brain.addActivity(Activity.CORE, GirlfriendAiPackages.coreActivities(this));
		brain.addActivity(Activity.IDLE, GirlfriendAiPackages.idleActivities(this));
		// 设置初始 Activity 喵~
		brain.setCoreActivities(Set.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.setActiveActivityIfPossible(Activity.IDLE);
		return brain;
	}
}
```

- [ ] **Step 5.3: 创建 FlowerSensor**

```java
// common/entity/ai/sensor/FlowerSensor.java
package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 花朵传感器 — 检测 8 格内所有花朵方块喵~
 *
 * @author liudongyu
 */
public class FlowerSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RANGE = 8;
	private static final int SCAN_RATE = 100; // 每 100 tick 扫描一次喵~

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(GirlfriendsMemoryTypes.NEARBY_FLOWERS.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		BlockPos entityPos = entity.blockPosition();
		List<BlockPos> flowers = new ArrayList<>();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++) {
			for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++) {
				for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
					mutable.set(entityPos.getX() + dx, entityPos.getY() + dy, entityPos.getZ() + dz);
					Block block = level.getBlockState(mutable).getBlock();
					if (block instanceof FlowerBlock || block == Blocks.FLOWERING_AZALEA ||
							block == Blocks.FLOWERING_AZALEA_LEAVES || block == Blocks.SPORE_BLOSSOM) {
						flowers.add(mutable.immutable());
					}
				}
			}
		}
		entity.getBrain().setMemory(GirlfriendsMemoryTypes.NEARBY_FLOWERS.get(), flowers);
	}
}
```

- [ ] **Step 5.4: 注册渲染器**

在 `GirlfriendsModClient` 中添加：

```java
event.registerEntityRenderer(GirlfriendsEntities.MOMO.get(),
		ctx -> new GirlfriendRenderer(ctx, "momo"));
```

- [ ] **Step 5.5: 创建礼物偏好 JSON**

```json
// data/girlfriends/girlfriends/gift_preferences/momo.json
{
	"favorite": ["minecraft:honeycomb"],
	"liked_tags": ["minecraft:flowers"],
	"liked_items": [
		"minecraft:honey_bottle",
		"minecraft:beehive",
		"minecraft:bee_nest",
		"minecraft:bone_meal",
		"minecraft:composter"
	],
	"accepted_tags": [],
	"accepted_items": [
		"minecraft:wheat_seeds",
		"minecraft:wheat",
		"minecraft:apple",
		"minecraft:sweet_berries",
		"minecraft:oak_log",
		"minecraft:dirt",
		"minecraft:grass_block"
	],
	"disliked_tags": [],
	"disliked_items": [
		"minecraft:rotten_flesh",
		"minecraft:spider_eye",
		"minecraft:bone",
		"minecraft:wither_skeleton_skull"
	]
}
```

- [ ] **Step 5.6: 创建固定委托定义 JSON**

以沫沫第 1 条固定委托为例（完整 10 条按此模式）：

```json
// data/girlfriends/girlfriends/fixed_quests/momo/fq_momo_01_flower_shadow.json
{
	"quest_id": "girlfriends:fq_momo_01_flower_shadow",
	"character_id": "girlfriends:momo",
	"fixed_index": 1,
	"required_stage": "familiar",
	"required_affection": 100,
	"prerequisite_fixed_indices": [],
	"objectives": [
		{
			"type": "item_delivery",
			"required_items": [
				{"item": {"tag": "minecraft:small_flowers"}, "count": 3}
			],
			"description_key": "quest.girlfriends.fq_momo_01.objective_flowers"
		},
		{
			"type": "item_delivery",
			"required_items": [
				{"item": {"tag": "minecraft:saplings"}, "count": 3}
			],
			"description_key": "quest.girlfriends.fq_momo_01.objective_saplings"
		},
		{
			"type": "block_stay",
			"required_blocks": ["minecraft:oak_fence"],
			"count": 20,
			"description_key": "quest.girlfriends.fq_momo_01.objective_fence"
		}
	],
	"rewards": {
		"affection": 50,
		"unlock_random_templates": ["girlfriends:rt_momo_01_flower_collect", "girlfriends:rt_momo_02_honey_supply"]
	},
	"title_key": "quest.girlfriends.fq_momo_01.title",
	"description_key": "quest.girlfriends.fq_momo_01.description"
}
```

其余 9 条固定委托文件路径为：
- `fq_momo_02_honey_letter.json` (信赖)
- `fq_momo_03_glass_bud.json` (信赖)
- `fq_momo_04_field_bloom.json` (信赖)
- `fq_momo_05_rain_greenhouse.json` (爱慕)
- `fq_momo_06_honey_breeze.json` (爱慕)
- `fq_momo_07_garden_color.json` (爱慕)
- `fq_momo_08_plant_together.json` (亲密确认)
- `fq_momo_09_glass_dome.json` (亲密)
- `fq_momo_10_garden_home.json` (家园前置)

每条 JSON 按 GDD 01_沫沫.md 第 6 节中的多目标拆分逐条编写，此处篇幅所限不展开全部 10 个文件，实施时按 GDD 严格落地喵~

- [ ] **Step 5.7: 创建随机委托模板 JSON**

以沫沫第 1 类随机委托为例（完整 8 类按此模式）：

```json
// data/girlfriends/girlfriends/random_quest_templates/momo/rt_momo_01_flower_collect.json
{
	"template_id": "girlfriends:rt_momo_01_flower_collect",
	"character_id": "girlfriends:momo",
	"unlock_stage": "stranger",
	"quest_type": "random",
	"weight": 10,
	"expire_days_range": [5, 10],
	"objectives": [
		{
			"type": "item_delivery",
			"required_items": [
				{"item": {"tag": "minecraft:small_flowers"}, "count": 8}
			],
			"description_key": "quest.girlfriends.rt_momo_01.objective"
		}
	],
	"rewards": {
		"base_affection": 8,
		"items": [
			{"item": "minecraft:bone_meal", "count": 4},
			{"item": "minecraft:wheat_seeds", "count": 4}
		]
	},
	"title_key": "quest.girlfriends.rt_momo_01.title",
	"description_key": "quest.girlfriends.rt_momo_01.description"
}
```

其余 7 类模板文件路径（按 GDD 01_沫沫.md 第 7 节）：
- `rt_momo_02_honey_supply.json`
- `rt_momo_03_seedling_expand.json`
- `rt_momo_04_shelter_tidy.json`
- `rt_momo_05_garden_accompany.json`
- `rt_momo_06_rare_color.json`
- `rt_momo_07_bee_guard.json`
- `rt_momo_08_home_decorate.json`

- [ ] **Step 5.8: 编译与全量测试**

```bash
./gradlew test
```
期望：全部测试通过，BUILD SUCCESSFUL。若编译错误，根据错误信息修正喵~

- [ ] **Step 5.9: 提交沫沫 Story**

```bash
git add -A
git commit -m "feat(REQ-7): Story 11 — 沫沫实体、花朵传感器、日程 AI；10 条固定委托 + 8 类随机模板 + 礼物偏好数据文件"
```

---

### Task 6: Story 12 — 渔溪（yuxi）实体 + AI + 数据文件

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/YuxiEntity.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/sensor/WaterSensor.java`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_preferences/yuxi.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/fixed_quests/yuxi/` (10 files)
- Create: `src/main/resources/data/girlfriends/girlfriends/random_quest_templates/yuxi/` (8 files)
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendsEntities.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java`

- [ ] **Step 6.1: 创建 YuxiEntity**

参考 `MomoEntity` 模式，关键差异：
- `getGirlfriendTypeId()` 返回 `GirlfriendTypes.YUXI_ID`
- Brain 注册 `NEAREST_WATER` 记忆和 `WaterSensor`
- 日程：清晨检查码头 → 上午垂钓 → 下午整理渔获 → 傍晚看日落 → 夜晚看海图

```java
// common/entity/YuxiEntity.java
package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.GirlfriendAiPackages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * 渔溪实体喵~
 * <p>
 * 喜欢水、热爱海洋且性格安静的女孩，生成于沙滩喵~
 *
 * @author liudongyu
 */
public class YuxiEntity extends GirlfriendEntity {
	public YuxiEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public ResourceLocation getGirlfriendTypeId() {
		return GirlfriendTypes.YUXI_ID;
	}

	@Override
	protected Brain.Provider<YuxiEntity> brainProvider() {
		return Brain.provider(
				List.of(
						GirlfriendsMemoryTypes.CURRENT_SCHEDULE.get(),
						GirlfriendsMemoryTypes.MEETING_POINT.get(),
						GirlfriendsMemoryTypes.WALK_TARGET.get(),
						GirlfriendsMemoryTypes.NEAREST_VISIBLE_PLAYER.get(),
						GirlfriendsMemoryTypes.NEAREST_WATER.get()
				),
				List.of(GirlfriendsSensorTypes.SHELTER_SENSOR.get())
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Brain<?> makeBrain(net.minecraft.world.entity.ai.Brain dynamic) {
		Brain<YuxiEntity> brain = (Brain<YuxiEntity>) dynamic;
		brain.addActivity(Activity.CORE, GirlfriendAiPackages.coreActivities(this));
		brain.addActivity(Activity.IDLE, GirlfriendAiPackages.idleActivities(this));
		brain.setCoreActivities(Set.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.setActiveActivityIfPossible(Activity.IDLE);
		return brain;
	}
}
```

- [ ] **Step 6.2: 创建 WaterSensor**

```java
// common/entity/ai/sensor/WaterSensor.java
// 扫描 12 格内水域方块（water 或 waterlogged），将最近的水域位置写入 NEAREST_WATER 记忆喵~
public class WaterSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RANGE = 12;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(GirlfriendsMemoryTypes.NEAREST_WATER.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		BlockPos entityPos = entity.blockPosition();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++) {
			for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++) {
				for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
					mutable.set(entityPos.getX() + dx, entityPos.getY() + dy, entityPos.getZ() + dz);
					if (!level.getFluidState(mutable).isEmpty()) {
						entity.getBrain().setMemory(GirlfriendsMemoryTypes.NEAREST_WATER.get(),
								GlobalPos.of(level.dimension(), mutable.immutable()));
						return;
					}
				}
			}
		}
		entity.getBrain().eraseMemory(GirlfriendsMemoryTypes.NEAREST_WATER.get());
	}
}
```

- [ ] **Step 6.3: 注册实体 + 渲染器 + 生成蛋**

在 `GirlfriendsEntities.java` 中添加 `YUXI` EntityType；在 `GirlfriendsModClient` 中注册渲染器。

- [ ] **Step 6.4: 创建数据文件**

按 GDD `02_渔溪.md` 创建：
- `gift_preferences/yuxi.json` — 最喜好鹦鹉螺壳，喜好鱼类/海晶类
- `fixed_quests/yuxi/fq_yuxi_01_tide_greeting.json` 等 10 条固定委托
- `random_quest_templates/yuxi/rt_yuxi_01_common_fish.json` 等 8 类随机模板

- [ ] **Step 6.5: 编译与全量测试**

```bash
./gradlew test
```
期望：全部测试通过

- [ ] **Step 6.6: 提交渔溪 Story**

```bash
git add -A
git commit -m "feat(REQ-7): Story 12 — 渔溪实体、水域传感器、日程 AI；10 条固定委托 + 8 类随机模板 + 礼物偏好数据文件"
```

---

### Task 7: Story 13 — 梅疏（meishu）实体 + AI + 数据文件

与 Story 12 模式完全相同喵~ 关键差异：

- **MeishuEntity**：`getGirlfriendTypeId()` 返回 `MEISHU_ID`，注册 `OreSensor`
- **OreSensor**：扫描 8 格内矿石标签方块（`minecraft:iron_ores`、`minecraft:copper_ores`、`minecraft:diamond_ores` 等）
- **注册**：ENTITY_TYPE `meishu`，渲染器路径 `"meishu"`
- **数据文件**：按 GDD `03_梅疏.md` 创建 19 个 JSON

提交信息：`feat(REQ-7): Story 13 — 梅疏实体、矿石传感器、日程 AI；10 条固定委托 + 8 类随机模板 + 礼物偏好数据文件`

---

### Task 8: Story 14 — 晚萤（wanying）实体 + AI + 数据文件

关键差异：

- **WanyingEntity**：`getGirlfriendTypeId()` 返回 `WANYING_ID`，注册 `HostileSensor`，额外覆写 `registerGoals()` 添加 `MeleeAttackGoal`
- **HostileSensor**：扫描 16 格内敌对生物（`Monster.class`），写入 `NEAREST_HOSTILE`
- **主动攻击行为**：Brain 中添加 `StartAttacking` behavior，当 `NEAREST_HOSTILE` 存在时优先攻击
- **注册**：ENTITY_TYPE `wanying`，渲染器路径 `"wanying"`
- **数据文件**：按 GDD `04_晚萤.md` 创建

提交信息：`feat(REQ-7): Story 14 — 晚萤实体、敌对生物传感器、战斗 AI；10 条固定委托 + 8 类随机模板 + 礼物偏好数据文件`

---

### Task 9: Story 15 — 幽若（youruo）实体 + AI + 数据文件

关键差异：

- **YouruoEntity**：`getGirlfriendTypeId()` 返回 `YOURUO_ID`，注册 `PearlSensor`
- **PearlSensor**：随机在 5 格范围内选取瞬移目标写入 `PEARL_TARGET`
- **瞬移行为**：Brain 中添加自定义 behavior，定期（约每 600 tick）将幽若传送到 `PEARL_TARGET` 位置并播放末影人传送粒子
- **注册**：ENTITY_TYPE `youruo`，渲染器路径 `"youruo"`
- **数据文件**：按 GDD `05_幽若.md` 创建

提交信息：`feat(REQ-7): Story 15 — 幽若实体、末影珍珠传感器、瞬移 AI；10 条固定委托 + 8 类随机模板 + 礼物偏好数据文件`

---

### Task 10: Story 16 — 庇护所结构生成与集成

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/spawn/ShelterStructureBlockEntity.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/spawn/ShelterControllerBlock.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/spawn/ShelterSpawnHandler.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/spawn/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/persist/GirlfriendsWorldData.java` — 添加 KD Tree
- Modify: `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java` — 注册方块实体

- [ ] **Step 10.1: 创建 ShelterStructureBlockEntity**

```java
// common/entity/spawn/ShelterStructureBlockEntity.java
package com.hexagram2021.girlfriends.common.entity.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 庇护所结构方块实体喵~
 * <p>
 * 作为庇护所结构的锚点，控制角色首次生成与重生喵~
 *
 * @author liudongyu
 */
public class ShelterStructureBlockEntity extends BlockEntity {
	private static final String TAG_CHARACTER_ID = "character_id";
	private static final String TAG_SHELTER_POS = "shelter_pos";
	private static final String TAG_GENERATED_DAY = "generated_day";
	private static final String TAG_AVAILABLE = "available";
	private static final String TAG_ACTIVE = "active";

	@Nullable
	private ResourceLocation characterId;
	private BlockPos shelterPos = BlockPos.ZERO;
	private long generatedDay;
	private boolean availableForRespawn = true;
	private boolean active;

	public ShelterStructureBlockEntity(BlockPos pos, BlockState blockState) {
		super(GirlfriendsBlockEntities.SHELTER_CONTROLLER.get(), pos, blockState);
	}

	public void setShelterData(ResourceLocation characterId, long day) {
		this.characterId = characterId;
		this.shelterPos = this.worldPosition;
		this.generatedDay = day;
		this.setChanged();
	}

	@Nullable
	public ResourceLocation getCharacterId() {
		return this.characterId;
	}

	public boolean isAvailableForRespawn() {
		return this.availableForRespawn;
	}

	public void setAvailableForRespawn(boolean available) {
		this.availableForRespawn = available;
		this.setChanged();
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
		this.setChanged();
	}

	public GlobalPos getShelterGlobalPos() {
		return GlobalPos.of(this.level.dimension(), this.shelterPos);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains(TAG_CHARACTER_ID)) {
			this.characterId = ResourceLocation.parse(tag.getString(TAG_CHARACTER_ID));
		}
		if (tag.contains(TAG_SHELTER_POS)) {
			this.shelterPos = BlockPos.of(tag.getLong(TAG_SHELTER_POS));
		}
		this.generatedDay = tag.getLong(TAG_GENERATED_DAY);
		this.availableForRespawn = tag.getBoolean(TAG_AVAILABLE);
		this.active = tag.getBoolean(TAG_ACTIVE);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (this.characterId != null) {
			tag.putString(TAG_CHARACTER_ID, this.characterId.toString());
		}
		tag.putLong(TAG_SHELTER_POS, this.shelterPos.asLong());
		tag.putLong(TAG_GENERATED_DAY, this.generatedDay);
		tag.putBoolean(TAG_AVAILABLE, this.availableForRespawn);
		tag.putBoolean(TAG_ACTIVE, this.active);
	}
}
```

- [ ] **Step 10.2: 创建方块实体类型注册**

在 `GirlfriendsMod.java` 或新建的 `GirlfriendsBlockEntities.java` 中注册：

```java
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShelterStructureBlockEntity>> SHELTER_CONTROLLER =
		BLOCK_ENTITIES.register("shelter_controller", () -> BlockEntityType.Builder
				.of(ShelterStructureBlockEntity::new, SHELTER_CONTROLLER_BLOCK.get())
				.build(null));
```

- [ ] **Step 10.3: 创建 ShelterControllerBlock**

技术性不可见方块，仅作为庇护所锚点：

```java
// common/entity/spawn/ShelterControllerBlock.java
public class ShelterControllerBlock extends Block implements EntityBlock {
	public ShelterControllerBlock() {
		super(BlockBehaviour.Properties.of()
				.strength(-1.0F, 3600000.0F)  // 不可破坏
				.noLootTable()
				.noOcclusion()
				.noCollission());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShelterStructureBlockEntity(pos, state);
	}
}
```

- [ ] **Step 10.4: 在 GirlfriendsWorldData 中添加 KD Tree**

参照 `MISCTWFSavedData.java` 模式：

```java
// 在 GirlfriendsWorldData 中添加
private static final Function<ResourceLocation, KDTree<BlockPos, Integer>> KD_TREE_COMPUTE =
		k -> KDTree.newLinkedKDTree(3);
private final Map<ResourceLocation, KDTree<BlockPos, Integer>> shelterKDTreesByDimension;

// 构造函数中初始化
this.shelterKDTreesByDimension = Maps.newHashMap();

// 插入庇护所
public void insertShelterToKDTree(ResourceLocation dimension, BlockPos pos) {
	this.shelterKDTreesByDimension.computeIfAbsent(dimension, KD_TREE_COMPUTE)
			.insert(KDTree.BuildNode.of(pos, MDUtils.vec3i(pos)));
	this.setDirty();
}

// 最近庇护所查找
public Optional<ShelterRecord> findNearestAvailableShelter(
		ResourceLocation characterId, ResourceLocation dimension, BlockPos from) {
	KDTree<BlockPos, Integer> tree = this.shelterKDTreesByDimension.get(dimension);
	if (tree == null || tree.isEmpty()) {
		return Optional.empty();
	}
	// 从 CharacterWorldState 的 discovered_shelters 中过滤该角色的可用庇护所喵~
	// 然后在 KD Tree 中找到最近的喵~
	// （实际实现需结合 ShelterRecord 列表 + KD Tree 索引）
}
```

- [ ] **Step 10.5: 编译与全量测试**

```bash
./gradlew test
```
期望：全部测试通过

- [ ] **Step 10.6: 提交 Story 16**

```bash
git add -A
git commit -m "feat(REQ-7): Story 16 — 庇护所结构方块实体、控制方块、KD Tree 集成与实体生成流程"
```

---

## 验收标准

1. **Story 11-15**：每位角色实体可生成、拥有独立皮肤、执行日程 AI、对应数据文件可被 Manager 正确加载
2. **Story 16**：庇护所方块实体正确管理角色生成与重生，KD Tree 支持 O(log n) 最近庇护所查找
3. **回归**：每个 Story 完成后 `./gradlew test` 全部通过
4. **编译**：每个 Story 完成后 `./gradlew build` BUILD SUCCESSFUL
