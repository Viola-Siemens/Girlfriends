# Story 7 跟随状态与祝福系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现跟随状态记录、祝福启用条件和五位角色祝福的服务端规则计算。

**Architecture:** `BlessingService` 只判断祝福是否可生效并计算修正结果，具体 NeoForge 事件接入放在独立 handler 中。这样高频事件可以用 O(1) 查询，不扫描世界状态。

**Tech Stack:** Java 25、JUnit Platform、NeoForge Event API、Story 1/2/5 服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/FollowMode.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingType.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingTypes.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingContext.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingParameterManager.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/BlessingService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/CommonBlessingEventHandlers.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/blessing/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/character/CharacterWorldState.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/blessing/BlessingServiceTest.java`

## Steps

- [ ] **Step 1: Write failing active blessing test**

Create `BlessingServiceTest`:

```java
@Test
public void blessingRequiresConfirmedIntimacyAndFollowMode() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	BlessingService service = new BlessingService(data, relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
	PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, GirlfriendTypes.WANYING_ID);
	relation.setAffection(700);
	relation.setConfirmedIntimacy(true);
	data.getCharacterState(GirlfriendTypes.WANYING_ID).setFollowTargetUuid(playerUuid);
	data.getCharacterState(GirlfriendTypes.WANYING_ID).setFollowMode(FollowMode.FOLLOW);
	data.getCharacterState(GirlfriendTypes.WANYING_ID).setAlive(true);

	Assertions.assertTrue(service.hasActiveBlessing(playerUuid, GirlfriendTypes.WANYING_ID, true, 16.0D));
	Assertions.assertFalse(service.hasActiveBlessing(playerUuid, GirlfriendTypes.WANYING_ID, false, 16.0D));
	Assertions.assertFalse(service.hasActiveBlessing(playerUuid, GirlfriendTypes.WANYING_ID, true, 40.0D));
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.blessing.BlessingServiceTest
```

Expected: compilation fails because blessing classes do not exist.

- [ ] **Step 2: Implement follow mode and registry-backed blessing metadata**

Create `FollowMode` values:

```java
STAY, FOLLOW, HOME
```

`BlessingType` is not an enum. It is a registry-backed data class created in Story 1 and must carry at least a stable `ResourceLocation` behavior key or strategy key.

`BlessingTypes` must provide built-in IDs and registered entries for:

```java
NATURE_PEACE_ID,
SAILING_AND_FISHING_ID,
MINING_EXTRA_DROP_ID,
MELEE_AND_DEFENSE_ID,
ENDER_PEARL_CONSERVE_ID
```

`GirlfriendType` points to a blessing type ID through `getBlessingTypeId()`; `BlessingService` resolves behavior from that registered blessing type ID. Addon mods can register new `BlessingType` entries and provide handlers without mixins.

Create `BlessingParameterManager` extending `SimplePreparableReloadListener`. It loads JSON from:

```text
data/<namespace>/girlfriends/blessing_parameters/<path>.json
```

Use it for configurable values such as boat speed multiplier, fishing double probability, mining extra drop probability, melee damage multiplier, incoming damage multiplier, and ender pearl conserve probability.

- [ ] **Step 3: Implement BlessingService.hasActiveBlessing**

Implement:

```java
public boolean hasActiveBlessing(UUID playerUuid, ResourceLocation girlfriendTypeId, boolean sameDimension, double distance)
```

Rules:

1. Relation must have confirmed intimacy.
2. Character state must be alive.
3. Follow mode must be `FOLLOW`.
4. Follow target UUID must equal player UUID.
5. Same dimension must be true.
6. Distance must be <= 32.0D.

- [ ] **Step 4: Run active blessing test**

Run BlessingService test command. Expected: test passes.

- [ ] **Step 5: Add effect calculation tests**

Add tests:

```java
@Test
public void wanyingDamageModifiersAreMultiplicative() {
	BlessingService service = new BlessingService(new GirlfriendsWorldData(), new RelationshipService(new GirlfriendsWorldData()));
	Assertions.assertEquals(14.0D, service.applyMeleeDamageBlessing(10.0D), 0.001D);
	Assertions.assertEquals(7.5D, service.applyIncomingDamageBlessing(10.0D), 0.001D);
}

@Test
public void probabilityUsesInjectedRandomValue() {
	BlessingService service = new BlessingService(new GirlfriendsWorldData(), new RelationshipService(new GirlfriendsWorldData()));
	Assertions.assertTrue(service.rollTwentyFivePercent(0.24D));
	Assertions.assertFalse(service.rollTwentyFivePercent(0.25D));
}
```

- [ ] **Step 6: Implement blessing effect helpers**

Implement:

```java
public double applyBoatSpeedBlessing(double baseSpeed)
public double applyMeleeDamageBlessing(double baseDamage)
public double applyIncomingDamageBlessing(double baseDamage)
public boolean rollTwentyFivePercent(double randomValue)
```

Rules:

1. Boat speed multiplier is 1.5.
2. Melee damage multiplier is 1.4.
3. Incoming damage multiplier is 0.75.
4. Probability succeeds when random value is `< 0.25D`.

- [ ] **Step 7: Add NeoForge event handler skeleton**

Create `CommonBlessingEventHandlers` with public static subscriber methods for planned events, but only delegate to service seams when event APIs are confirmed during implementation.

Do not add unverified event method signatures. If exact NeoForge event class name is unclear, leave this file out and document the integration in Story 10 manual checklist.

- [ ] **Step 8: Run tests**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.blessing.BlessingServiceTest
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 9: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/blessing src/test/java/com/hexagram2021/girlfriends/common/blessing
git commit -m "feat(v0.1.0): add blessing service"
```
