# Story 6 家园系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现家园伙伴绑定、双人床校验抽象、家园回血、每日家园好感和争执事件。

**Architecture:** `HomeService` 只依赖可测试的床校验接口和关系/绑定服务，不直接在纯规则测试中访问 Minecraft 世界对象。实际床方块与距离检测在 Minecraft 集成层适配。

**Tech Stack:** Java 25、JUnit Platform、Story 1/2/5 服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/BedValidator.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/HomeService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/HomeTickResult.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/HomeConflictResult.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/home/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/home/HomeState.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/home/HomeServiceTest.java`

## Steps

- [ ] **Step 1: Write failing home invitation test**

Create `HomeServiceTest`:

```java
@Test
public void inviteHomeRequiresAffectionAndNoExistingPartner() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	HomeService homeService = new HomeService(data, relationshipService, bedPos -> true);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000012");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setAffection(900);
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setConfirmedIntimacy(true);
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getCompletedFixedQuests().add(10);

	boolean invited = homeService.inviteHome(playerUuid, GirlfriendTypes.MOMO_ID, "minecraft:overworld", 1, 64, 1);

	Assertions.assertTrue(invited);
	Assertions.assertEquals(GirlfriendTypes.MOMO_ID, data.getOrCreateHome(playerUuid).getGirlfriendTypeId());
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.home.HomeServiceTest
```

Expected: compilation fails because `HomeService` does not exist.

- [ ] **Step 2: Implement BedValidator and results**

Create `BedValidator` functional interface:

```java
boolean isValid(HomeAnchor homeAnchor);
```

Create `HomeAnchor(String dimensionId, int x, int y, int z)` as a record if `HomeState` does not already hold equivalent fields.

Create records:

```java
HomeTickResult(boolean healed, int affectionDelta)
HomeConflictResult(boolean triggered, int homePartnerDelta, int visitorDelta)
```

- [ ] **Step 3: Implement HomeService.inviteHome**

Implement:

```java
public boolean inviteHome(UUID playerUuid, ResourceLocation girlfriendTypeId, String dimensionId, int x, int y, int z)
public boolean releaseHomePartner(UUID playerUuid)
```

Rules:

1. Relation affection must be at least 900.
2. `confirmedIntimacy` must be true.
3. Completed fixed quests must contain 10.
4. Existing active home partner blocks invitation.
5. Bed validator must return true.

- [ ] **Step 4: Run invitation test**

Run HomeService test command. Expected: test passes.

- [ ] **Step 5: Add daily benefit test**

Add test:

```java
@Test
public void homeBenefitAddsDailyAffectionOnce() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	HomeService homeService = new HomeService(data, relationshipService, bedPos -> true);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000013");
	PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, GirlfriendTypes.YUXI_ID);
	relation.setAffection(900);
	relation.setConfirmedIntimacy(true);
	relation.getCompletedFixedQuests().add(10);
	homeService.inviteHome(playerUuid, GirlfriendTypes.YUXI_ID, "minecraft:overworld", 0, 64, 0);

	HomeTickResult first = homeService.applyHomeBenefit(playerUuid, true, true);
	HomeTickResult second = homeService.applyHomeBenefit(playerUuid, true, true);

	Assertions.assertEquals(2, first.affectionDelta());
	Assertions.assertEquals(0, second.affectionDelta());
	Assertions.assertEquals(902, relation.getAffection());
}
```

- [ ] **Step 6: Implement home benefit**

Implement:

```java
public HomeTickResult applyHomeBenefit(UUID playerUuid, boolean playerNearBed, boolean partnerNearBed)
```

Rules:

1. No active home state returns no effect.
2. Both player and partner must be near bed.
3. Heal flag is true when both are near bed.
4. Daily affection +2 only when `dailyHomeGainClaimed` is false.

- [ ] **Step 7: Add conflict test**

Add test that current home partner and visitor both lose 3..5 affection only once per day. Use deterministic supplier returning 4.

- [ ] **Step 8: Implement conflict method**

Implement:

```java
public HomeConflictResult triggerConflict(UUID playerUuid, GirlfriendType visitorType, IntSupplier penaltySupplier)
```

Rules:

1. Require active home partner.
2. Visitor must not equal current home partner.
3. Player relation with visitor must have confirmed intimacy.
4. Daily conflict flag prevents repeated trigger.
5. Apply negative affection to both current home partner and visitor.

- [ ] **Step 9: Run tests**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.home.HomeServiceTest
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 10: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/home src/test/java/com/hexagram2021/girlfriends/common/home
git commit -m "feat(v0.1.0): add home system rules"
```
