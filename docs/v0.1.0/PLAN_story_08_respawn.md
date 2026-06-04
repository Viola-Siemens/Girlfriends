# Story 8 角色死亡、庇护所记录与重生接口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现角色死亡集中清理、终章奖励保留、庇护所注册、最近庇护所查找与待重生状态。

**Architecture:** `CharacterRespawnService` 是死亡清理的唯一入口，集中调用关系、委托、绑定和家园清理逻辑。庇护所由结构系统通过注册接口写入，重生逻辑不扫描世界结构。

**Tech Stack:** Java 25、JUnit Platform、Story 1/2/4/5/6 服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/death/CharacterRespawnService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/death/RespawnResult.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/death/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/death/ShelterRecord.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/character/CharacterWorldState.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/death/CharacterRespawnServiceTest.java`

## Steps

- [ ] **Step 1: Write failing death reset test**

Create `CharacterRespawnServiceTest`:

```java
@Test
public void deathClearsCharacterStateButKeepsFinalReward() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000015");
	PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, GirlfriendTypes.MEISHU_ID);
	relation.setAffection(900);
	relation.setConfirmedIntimacy(true);
	relation.setClaimedFinalReward(true);
	relation.getCompletedFixedQuests().add(10);

	service.handleCharacterDeath(GirlfriendTypes.MEISHU_ID, "minecraft:overworld", 10, 64, 10);

	PlayerCharacterRelation restored = data.getOrCreateRelation(playerUuid, GirlfriendTypes.MEISHU_ID);
	Assertions.assertEquals(0, restored.getAffection());
	Assertions.assertFalse(restored.isConfirmedIntimacy());
	Assertions.assertTrue(restored.isClaimedFinalReward());
	Assertions.assertTrue(restored.getCompletedFixedQuests().isEmpty());
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.death.CharacterRespawnServiceTest
```

Expected: compilation fails because `CharacterRespawnService` does not exist.

- [ ] **Step 2: Implement RespawnResult and service constructor**

Create:

```java
public record RespawnResult(boolean respawned, boolean pendingRespawn, ShelterRecord shelterRecord) {
}
```

Implement constructor:

```java
public CharacterRespawnService(GirlfriendsWorldData worldData, RelationshipService relationshipService)
```

- [ ] **Step 3: Implement handleCharacterDeath**

Implement:

```java
public RespawnResult handleCharacterDeath(ResourceLocation girlfriendTypeId, String dimensionId, int x, int y, int z)
```

Rules:

1. Clear current quest.
2. Clear binding state.
3. Reset all relations for this character but preserve `claimedFinalReward`.
4. Release home states pointing to this character.
5. Mark character `alive = false`.
6. Store death position fields.
7. Try nearest shelter and set pending if absent.

- [ ] **Step 4: Run death reset test**

Run CharacterRespawnService test command. Expected: test passes.

- [ ] **Step 5: Add shelter registration and nearest test**

Add test registering two shelters and asserting nearest one is selected:

```java
@Test
public void nearestShelterIsSelectedForRespawn() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);
	service.registerShelter(GirlfriendTypes.MOMO_ID, "minecraft:overworld", 0, 64, 0, 1L);
	service.registerShelter(GirlfriendTypes.MOMO_ID, "minecraft:overworld", 100, 64, 100, 1L);

	RespawnResult result = service.handleCharacterDeath(GirlfriendTypes.MOMO_ID, "minecraft:overworld", 10, 64, 10);

	Assertions.assertTrue(result.respawned());
	Assertions.assertFalse(result.pendingRespawn());
	Assertions.assertEquals(0, result.shelterRecord().x());
}
```

- [ ] **Step 6: Implement shelter registration and lookup**

Implement:

```java
public ShelterRecord registerShelter(ResourceLocation girlfriendTypeId, String dimensionId, int x, int y, int z, long currentDay)
public Optional<ShelterRecord> findNearestShelter(ResourceLocation girlfriendTypeId, String dimensionId, int x, int y, int z)
public void tryRespawnPendingCharacter(ResourceLocation girlfriendTypeId)
```

Use squared distance to avoid unnecessary square root.

- [ ] **Step 7: Add pending respawn test**

Add test that death without shelters returns `pendingRespawn = true` and character state stays not alive.

- [ ] **Step 8: Run tests**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.death.CharacterRespawnServiceTest
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 9: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/death src/test/java/com/hexagram2021/girlfriends/common/death
git commit -m "feat(v0.1.0): add character respawn rules"
```
