# Story 2 好感度与每日计数系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现玩家与角色之间的好感度变化、阶段推导、每日计数重置与角色死亡清零入口。

**Architecture:** 所有好感修改必须通过 `RelationshipService`，其他系统只能请求变更，不能直接写关系字段。阶段由数值与亲密确认状态实时推导，不重复持久化。

**Tech Stack:** Java 25、JUnit Platform、Story 1 领域模型。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/relationship/AffectionChangeSource.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/relationship/RelationshipService.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/relationship/PlayerCharacterRelation.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/relationship/RelationshipServiceTest.java`

## Steps

- [ ] **Step 1: Write failing stage and clamp tests**

Create `RelationshipServiceTest` with tests for clamp and stage:

```java
@Test
public void changeAffectionClampsToRange() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService service = new RelationshipService(data);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");

	service.changeAffection(playerUuid, GirlfriendTypes.MOMO_ID, AffectionChangeSource.GIFT, 1200);
	Assertions.assertEquals(1000, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getAffection());

	service.changeAffection(playerUuid, GirlfriendTypes.MOMO_ID, AffectionChangeSource.PLAYER_ATTACK, -2000);
	Assertions.assertEquals(0, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getAffection());
}

@Test
public void stageRequiresIntimacyConfirmation() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService service = new RelationshipService(data);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000003");
	PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID);
	relation.setAffection(700);

	Assertions.assertEquals(AffectionStage.AFFECTION, service.getEffectiveStage(relation));
	relation.setConfirmedIntimacy(true);
	Assertions.assertEquals(AffectionStage.INTIMATE, service.getEffectiveStage(relation));
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.relationship.RelationshipServiceTest
```

Expected: compilation fails because `RelationshipService` does not exist.

- [ ] **Step 2: Implement AffectionChangeSource**

Create enum values:

```java
GIFT,
FIXED_QUEST,
RANDOM_QUEST,
HOME_DAILY,
HOME_CONFLICT,
PLAYER_ATTACK,
CHARACTER_DEATH_RESET
```

- [ ] **Step 3: Implement RelationshipService**

Implement constructor and methods:

```java
public RelationshipService(GirlfriendsWorldData worldData)
public PlayerCharacterRelation getRelation(UUID playerUuid, ResourceLocation girlfriendTypeId)
public int changeAffection(UUID playerUuid, ResourceLocation girlfriendTypeId, AffectionChangeSource source, int rawDelta)
public AffectionStage getNumericStage(int affection)
public AffectionStage getEffectiveStage(PlayerCharacterRelation relation)
public void resetDailyCounters(long currentDay)
public void resetCharacterRelations(ResourceLocation girlfriendTypeId)
```

Rules:

1. Clamp affection to 0..1000.
2. Return `AFFECTION` for 700..899 if `confirmedIntimacy` is false.
3. Return `HOME_PARTNER` only when affection is at least 900 and `homePartner` state is set later by HomeService.
4. `resetDailyCounters` resets gift/home/conflict flags only when `lastDailyResetDay != currentDay`.
5. `resetCharacterRelations` clears relations for one character except final reward ownership if Story 1 stores it separately.

- [ ] **Step 4: Run stage and clamp tests**

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.relationship.RelationshipServiceTest
```

Expected: tests pass.

- [ ] **Step 5: Add daily reset test**

Add test:

```java
@Test
public void resetDailyCountersOnlyOncePerDay() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService service = new RelationshipService(data);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000004");
	PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, GirlfriendTypes.YUXI_ID);
	relation.setDailyGiftGain(15);
	relation.setDailyHomeGainClaimed(true);
	relation.setDailyConflictTriggered(true);

	service.resetDailyCounters(10L);

	Assertions.assertEquals(0, relation.getDailyGiftGain());
	Assertions.assertFalse(relation.isDailyHomeGainClaimed());
	Assertions.assertFalse(relation.isDailyConflictTriggered());
	Assertions.assertEquals(10L, relation.getLastDailyResetDay());
}
```

Run the same test command. Expected: test passes.

- [ ] **Step 6: Run full test task**

Run:

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 7: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/relationship src/test/java/com/hexagram2021/girlfriends/common/relationship
git commit -m "feat(v0.1.0): add relationship service"
```