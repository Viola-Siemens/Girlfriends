# Story 5 多人绑定与竞争 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现爱慕绑定、其他玩家推进限制、动摇期、绑定转移和亲密锁定。

**Architecture:** `BindingService` 是绑定权限的唯一判断入口，礼物、委托和网络层都通过它判断某玩家是否可以推进某角色。绑定状态保存在 `CharacterBindingState` 中。

**Tech Stack:** Java 25、JUnit Platform、Story 1/2/4 模型与服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/binding/BindingService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/binding/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/binding/CharacterBindingState.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/binding/BindingServiceTest.java`

## Steps

- [ ] **Step 1: Write failing binding establishment test**

Create `BindingServiceTest`:

```java
@Test
public void affectionStageCreatesBindingWhenNoneExists() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	BindingService bindingService = new BindingService(data, relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000009");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setAffection(500);

	bindingService.updateBindingAfterAffectionChange(playerUuid, GirlfriendTypes.MOMO_ID, 10L);

	Assertions.assertEquals(playerUuid, data.getCharacterState(GirlfriendTypes.MOMO_ID).getBinding().getBoundPlayerUuid());
	Assertions.assertTrue(bindingService.canReceiveGift(playerUuid, GirlfriendTypes.MOMO_ID));
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.binding.BindingServiceTest
```

Expected: compilation fails because `BindingService` does not exist.

- [ ] **Step 2: Implement BindingService basic permission methods**

Implement:

```java
public void updateBindingAfterAffectionChange(UUID playerUuid, ResourceLocation girlfriendTypeId, long currentDay)
public boolean canReceiveGift(UUID playerUuid, ResourceLocation girlfriendTypeId)
public boolean canReceiveFixedQuest(UUID playerUuid, ResourceLocation girlfriendTypeId)
public boolean canReceiveRandomQuest(UUID playerUuid, ResourceLocation girlfriendTypeId)
```

Rules:

1. No binding means everyone can receive gifts and quests.
2. Bound player can receive gifts and fixed quests.
3. Non-bound player cannot receive gifts or fixed quests after love binding.
4. After intimacy lock, only bound player can receive random quests.

- [ ] **Step 3: Run binding establishment test**

Run BindingService test command. Expected: test passes.

- [ ] **Step 4: Add wavering transfer test**

Add test:

```java
@Test
public void challengerTransfersBindingAfterThreeDays() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	BindingService bindingService = new BindingService(data, relationshipService);
	UUID first = UUID.fromString("00000000-0000-0000-0000-000000000010");
	UUID second = UUID.fromString("00000000-0000-0000-0000-000000000011");
	data.getOrCreateRelation(first, GirlfriendTypes.YUXI_ID).setAffection(500);
	data.getOrCreateRelation(second, GirlfriendTypes.YUXI_ID).setAffection(501);

	bindingService.updateBindingAfterAffectionChange(first, GirlfriendTypes.YUXI_ID, 1L);
	bindingService.updateBindingAfterAffectionChange(second, GirlfriendTypes.YUXI_ID, 2L);
	bindingService.checkWavering(GirlfriendTypes.YUXI_ID, 5L);

	Assertions.assertEquals(second, data.getCharacterState(GirlfriendTypes.YUXI_ID).getBinding().getBoundPlayerUuid());
}
```

- [ ] **Step 5: Implement wavering logic**

Implement:

```java
public void checkWavering(ResourceLocation girlfriendTypeId, long currentDay)
```

Rules:

1. Challenger starts when a non-bound relation has affection greater than bound relation.
2. `waveringStartDay` is current day when challenger starts.
3. After `currentDay - waveringStartDay >= 3`, transfer if challenger is still greater.
4. Clear challenger if no longer ahead.
5. If another challenger is higher, replace challenger and reset start day.

- [ ] **Step 6: Add intimacy lock test**

Add test that `confirmIntimacy(playerUuid, girlfriendType)` sets bound player and locked flag, then non-bound player cannot receive random quest.

- [ ] **Step 7: Implement intimacy confirmation**

Implement:

```java
public void confirmIntimacy(UUID playerUuid, ResourceLocation girlfriendTypeId)
```

Set relation `confirmedIntimacy = true`, binding bound UUID to player, `lockedByIntimacy = true`, and clear challenger.

- [ ] **Step 8: Run BindingService tests and full tests**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.binding.BindingServiceTest
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 9: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/binding src/test/java/com/hexagram2021/girlfriends/common/binding
git commit -m "feat(v0.1.0): add binding competition rules"
```
