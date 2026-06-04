# Story 3 礼物系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现礼物偏好、赠礼公式、每日上限、拒收规则和绑定限制下的赠礼入口。

**Architecture:** 礼物系统通过 `GiftService` 调用 `RelationshipService` 修改好感，不直接写关系字段。礼物偏好通过继承 `SimplePreparableReloadListener` 的 `GiftPreferenceManager` 从数据包 JSON 加载，服务层只读取 Manager 提供的不可变偏好视图。

**Tech Stack:** Java 25、JUnit Platform、Minecraft Item/ResourceLocation 抽象边界、Story 1/2 服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftPreferenceLevel.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftPreference.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftPreferenceManager.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftResult.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/package-info.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/gift/GiftServiceTest.java`

## Steps

- [ ] **Step 1: Write failing gift formula test**

Create `GiftServiceTest`:

```java
@Test
public void favoriteGiftUsesDiminishingReturnsAndDailyCap() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	GiftService giftService = new GiftService(relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000005");

	GiftResult result = giftService.applyGift(playerUuid, GirlfriendTypes.MOMO_ID, GiftPreferenceLevel.FAVORITE);

	Assertions.assertFalse(result.rejected());
	Assertions.assertEquals(5, result.affectionDelta());
	Assertions.assertEquals(5, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getAffection());
	Assertions.assertEquals(5, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getDailyGiftGain());
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.gift.GiftServiceTest
```

Expected: compilation fails because gift classes do not exist.

- [ ] **Step 2: Implement gift DTOs and enum**

Create `GiftPreferenceLevel` with base deltas:

```java
FAVORITE(5), LIKED(4), ACCEPTED(2), DISLIKED(-2), REJECTED(0)
```

Create `GiftResult(boolean rejected, int affectionDelta, GiftPreferenceLevel level, String messageKey)` as a record.

Create `GiftPreference` with character type ID and item/tag IDs as sets of `ResourceLocation` strings.

Create `GiftPreferenceManager` extending `SimplePreparableReloadListener<Map<ResourceLocation, GiftPreference>>`. It must load JSON files from:

```text
data/<namespace>/girlfriends/gift_preferences/<path>.json
```

Each JSON file maps to one character type ID derived from file location, for example `data/girlfriends/girlfriends/gift_preferences/momo.json` maps to `girlfriends:momo`.

The manager should parse these fields:

```json
{
	"favorite": ["minecraft:honeycomb"],
	"liked_tags": ["minecraft:flowers"],
	"liked_items": ["minecraft:honey_bottle"],
	"accepted_tags": [],
	"accepted_items": [],
	"disliked_tags": [],
	"disliked_items": ["minecraft:rotten_flesh"]
}
```

Reload output must be immutable after apply, so `GiftService` can read preferences safely during gameplay.

- [ ] **Step 3: Implement GiftService.applyGift**

Implement a pure calculation entry for tests and a production entry backed by the manager:

```java
public GiftResult applyGift(UUID playerUuid, ResourceLocation girlfriendTypeId, GiftPreferenceLevel level)
public GiftResult applyGiftItem(UUID playerUuid, ResourceLocation girlfriendTypeId, ItemStack itemStack)
```

`applyGiftItem` asks `GiftPreferenceManager` for the target character's preference and resolves the item into a `GiftPreferenceLevel`; `applyGift` keeps formula testing independent from Minecraft item registry setup.

Rules:

1. `REJECTED` returns rejected result with delta 0.
2. Positive gifts are rejected when `dailyGiftGain >= 15`.
3. Formula is `baseDelta * sqrt((16 - dailyGiftGain) / 16.0D)`.
4. Positive delta floors but is at least 1.
5. Negative delta rounds toward zero but is at most -1.
6. Positive `dailyGiftGain` is capped to 15.
7. Affection change goes through `RelationshipService.changeAffection`.

- [ ] **Step 4: Run formula test**

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.gift.GiftServiceTest
```

Expected: test passes.

- [ ] **Step 5: Add cap and disliked tests**

Add tests:

```java
@Test
public void positiveGiftRejectedAfterDailyCap() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	GiftService giftService = new GiftService(relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000006");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setDailyGiftGain(15);

	GiftResult result = giftService.applyGift(playerUuid, GirlfriendTypes.MOMO_ID, GiftPreferenceLevel.LIKED);

	Assertions.assertTrue(result.rejected());
	Assertions.assertEquals(0, result.affectionDelta());
}

@Test
public void dislikedGiftStillReducesAffectionAtDailyCap() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	GiftService giftService = new GiftService(relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000007");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setAffection(100);
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setDailyGiftGain(15);

	GiftResult result = giftService.applyGift(playerUuid, GirlfriendTypes.MOMO_ID, GiftPreferenceLevel.DISLIKED);

	Assertions.assertFalse(result.rejected());
	Assertions.assertEquals(-1, result.affectionDelta());
	Assertions.assertEquals(99, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getAffection());
}
```

Run the same test command. Expected: tests pass.

- [ ] **Step 6: Add binding permission seam**

Add a constructor overload to `GiftService` accepting a predicate:

```java
BiPredicate<UUID, ResourceLocation> canReceiveGift
```

Default constructor uses `(playerUuid, girlfriendType) -> true`.

Add test that predicate false returns `REJECTED` without affection change.

- [ ] **Step 7: Run full test task**

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 8: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/gift src/test/java/com/hexagram2021/girlfriends/common/gift
git commit -m "feat(v0.1.0): add gift service"
```
