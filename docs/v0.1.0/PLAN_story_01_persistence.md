# Story 1 基础领域模型与持久化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 Girlfriends 底层系统的基础领域模型、角色类型注册表、祝福类型注册表、世界级持久化容器和序列化测试。

**Architecture:** 先补齐模组 ID 与 JUnit 配置，再实现纯 Java 数据模型与 NBT 序列化，最后接入 `SavedData` 容器。后续 Story 只能通过这些模型和容器读写关系、委托、家园、绑定和庇护所状态。

**Tech Stack:** Java 25、NeoForge 26.1.2.71、Minecraft NBT、SavedData、JUnit Platform。

---

## Files

- Modify: `build.gradle`
- Modify: `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/character/GirlfriendType.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/character/DimensionPolicy.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/character/GirlfriendTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/character/GirlfriendsRegistries.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingType.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/relationship/AffectionStage.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestType.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestState.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestInstance.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/relationship/RelationKey.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/relationship/PlayerCharacterRelation.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/binding/CharacterBindingState.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/HomeState.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/character/CharacterWorldState.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/death/ShelterRecord.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/persist/GirlfriendsWorldData.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/**/package-info.java`
- Create: `src/test/java/com/hexagram2021/girlfriends/common/persist/GirlfriendsWorldDataTest.java`

## Steps

- [ ] **Step 1: Enable JUnit Platform**

Modify `build.gradle` by appending this block after the existing `tasks.withType(JavaCompile).configureEach` block:

```groovy
test {
	useJUnitPlatform()
}
```

- [ ] **Step 2: Fix the mod id constant**

Modify `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java`:

```java
public static final String MODID = "girlfriends";
```

Run:

```bash
./gradlew test
```

Expected: Gradle completes successfully or reports no tests found without compilation errors.

- [ ] **Step 3: Write failing persistence round-trip test**

Create `src/test/java/com/hexagram2021/girlfriends/common/persist/GirlfriendsWorldDataTest.java`:

```java
package com.hexagram2021.girlfriends.common.persist;

import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class GirlfriendsWorldDataTest {
	@Test
	public void serializeAndDeserializeRelation() {
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ResourceLocation momoId = GirlfriendTypes.MOMO_ID;
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, momoId);
		relation.setAffection(321);
		relation.setConfirmedIntimacy(true);

		CompoundTag tag = data.save(new CompoundTag());
		GirlfriendsWorldData restored = GirlfriendsWorldData.load(tag);
		PlayerCharacterRelation restoredRelation = restored.getRelations().get(new RelationKey(playerUuid, momoId));

		Assertions.assertNotNull(restoredRelation);
		Assertions.assertEquals(321, restoredRelation.getAffection());
		Assertions.assertTrue(restoredRelation.isConfirmedIntimacy());
	}
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldDataTest
```

Expected: compilation fails because `GirlfriendsWorldData` and related domain classes do not exist.

- [ ] **Step 4: Implement registry-backed character type and value key**

Create `GirlfriendType` as a normal public class, not an enum. It must be safe for addon mods to register new instances without mixins:

```java
public class GirlfriendType {
	private final String descriptionId;
	private final DimensionPolicy dimensionPolicy;
	private final ResourceLocation favoriteGiftItem;
	private final ResourceLocation blessingTypeId;
	private final ResourceLocation shelterStructureKey;

	public GirlfriendType(String descriptionId, DimensionPolicy dimensionPolicy, ResourceLocation favoriteGiftItem,
			ResourceLocation blessingTypeId, ResourceLocation shelterStructureKey) {
		this.descriptionId = descriptionId;
		this.dimensionPolicy = dimensionPolicy;
		this.favoriteGiftItem = favoriteGiftItem;
		this.blessingTypeId = blessingTypeId;
		this.shelterStructureKey = shelterStructureKey;
	}

	public String getDescriptionId() {
		return this.descriptionId;
	}

	public DimensionPolicy getDimensionPolicy() {
		return this.dimensionPolicy;
	}

	public ResourceLocation getFavoriteGiftItem() {
		return this.favoriteGiftItem;
	}

	public ResourceLocation getBlessingTypeId() {
		return this.blessingTypeId;
	}

	public ResourceLocation getShelterStructureKey() {
		return this.shelterStructureKey;
	}
}
```

Create `DimensionPolicy` as a small value object for allowed spawn and respawn dimensions:

```java
public record DimensionPolicy(Set<ResourceLocation> allowedDimensions) {
	public boolean allows(ResourceLocation dimensionId) {
		return this.allowedDimensions.contains(dimensionId);
	}
}
```

Create `GirlfriendsRegistries` with custom NeoForge registry keys and registry instances for `GirlfriendType` and `BlessingType`:

```java
public final class GirlfriendsRegistries {
	public static final ResourceKey<Registry<GirlfriendType>> GIRLFRIEND_TYPE = ResourceKey.createRegistryKey(
		ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriend_type")
	);
	public static final ResourceKey<Registry<BlessingType>> BLESSING_TYPE = ResourceKey.createRegistryKey(
		ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "blessing_type")
	);

	public static final Registry<GirlfriendType> GIRLFRIEND_TYPE_REGISTRY = new RegistryBuilder<>(GIRLFRIEND_TYPE)
		.sync(true)
		.create();
	public static final Registry<BlessingType> BLESSING_TYPE_REGISTRY = new RegistryBuilder<>(BLESSING_TYPE)
		.sync(true)
		.create();

	private GirlfriendsRegistries() {
	}
}
```

Create the custom registry during NeoForge `NewRegistryEvent`; only after the registry exists may `GirlfriendTypes.REGISTER` attach entries to the mod event bus. Add a mod-bus event handler in `GirlfriendsMod` or a dedicated common registry handler:

```java
@SubscribeEvent
public static void onNewRegistry(NewRegistryEvent event) {
	event.register(GirlfriendsRegistries.BLESSING_TYPE_REGISTRY);
	event.register(GirlfriendsRegistries.GIRLFRIEND_TYPE_REGISTRY);
}
```

Then register built-in girlfriend types in the mod constructor:

```java
public GirlfriendsMod(IEventBus modEventBus, ModContainer modContainer) {
	BlessingTypes.REGISTER.register(modEventBus);
	GirlfriendTypes.REGISTER.register(modEventBus);
}
```

If the exact NeoForge 26.1.2 method name differs, inspect `NewRegistryEvent` in the dependency sources and keep the same lifecycle: create custom registry in `NewRegistryEvent`, then attach `DeferredRegister` entries to the mod event bus.

Create `GirlfriendTypes` with built-in registration entries and ID constants:

```java
public final class GirlfriendTypes {
	public static final DeferredRegister<GirlfriendType> REGISTER = DeferredRegister.create(
		GirlfriendsRegistries.GIRLFRIEND_TYPE,
		GirlfriendsMod.MODID
	);

	public static final ResourceLocation MOMO_ID = ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	public static final ResourceLocation YUXI_ID = ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");
	public static final ResourceLocation MEISHU_ID = ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu");
	public static final ResourceLocation WANYING_ID = ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying");
	public static final ResourceLocation YOURUO_ID = ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo");

	public static final DeferredHolder<GirlfriendType, GirlfriendType> MOMO = REGISTER.register("momo", () -> new GirlfriendType(
		"girlfriends.girlfriend_type.momo",
		new DimensionPolicy(Set.of(ResourceLocation.withDefaultNamespace("overworld"))),
		ResourceLocation.withDefaultNamespace("honeycomb"),
		BlessingTypes.NATURE_PEACE_ID,
		ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo_shelter")
	));

	private GirlfriendTypes() {
	}
}
```

The implementation must register all five built-in types: `momo`, `yuxi`, `meishu`, `wanying`, and `youruo`.

Create `AffectionStage` with `STRANGER/FAMILIAR/TRUST/AFFECTION/INTIMATE/HOME_PARTNER` and min/max fields.

Create `QuestType` with `FIXED/RANDOM`.

Create `QuestState` with `AVAILABLE/ACCEPTED/COMPLETED/EXPIRED`.

Create `RelationKey` as a `record RelationKey(UUID playerUuid, ResourceLocation girlfriendTypeId)`.

- [ ] **Step 5: Implement relation and state classes**

Implement `PlayerCharacterRelation`, `CharacterBindingState`, `HomeState`, `QuestInstance`, `CharacterWorldState`, and `ShelterRecord` with:

1. Private fields from `DR_system.md`.
2. Public getters and setters.
3. `serializeNBT()` returning `CompoundTag`.
4. Static `deserializeNBT(CompoundTag tag)` methods.
5. `data_version` written at the top-level state where needed.

- [ ] **Step 6: Implement GirlfriendsWorldData**

Implement `GirlfriendsWorldData` extending `SavedData`, with:

```java
public static final int DATA_VERSION = 1;
public static final String DATA_NAME = "girlfriends_world_data";
private final Map<ResourceLocation, CharacterWorldState> characters = new HashMap<>();
private final Map<RelationKey, PlayerCharacterRelation> relations = new HashMap<>();
private final Map<UUID, HomeState> homes = new HashMap<>();
```

Use `ResourceLocation` as the persisted and map key for character types. Runtime registry objects can be resolved from that ID when needed, but SavedData must not persist Java enum ordinals or enum names.

Expose:

```java
public PlayerCharacterRelation getOrCreateRelation(UUID playerUuid, ResourceLocation girlfriendTypeId)
public CharacterWorldState getCharacterState(ResourceLocation girlfriendTypeId)
public Map<RelationKey, PlayerCharacterRelation> getRelations()
public static GirlfriendsWorldData load(CompoundTag tag)
@Override public CompoundTag save(CompoundTag tag)
```

- [ ] **Step 7: Run persistence test**

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldDataTest
```

Expected: test passes.

- [ ] **Step 8: Run full test task**

Run:

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 9: Commit checkpoint if requested**

Only if the user asked for commits, run:

```bash
git add build.gradle src/main/java/com/hexagram2021/girlfriends src/test/java/com/hexagram2021/girlfriends
git commit -m "feat(v0.1.0): add base domain persistence"
```