# Story 4 委托槽与委托框架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现每个角色唯一委托槽、固定委托发布条件、随机委托刷新与过期、通用委托目标接口。

**Architecture:** `QuestService` 只管理委托状态和规则，不承载具体角色剧情目标。具体目标通过 `QuestObjectiveHandler` 在后续 GDD 补齐后挂接。

**Tech Stack:** Java 25、JUnit Platform、Story 1/2 领域模型与关系服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestDefinition.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/FixedQuestDefinitionManager.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/RandomQuestTemplateManager.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestObjectiveHandler.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestObjectiveGroup.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/ItemDeliveryObjectiveHandler.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/StructureVisitObjectiveHandler.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/BlockStayObjectiveHandler.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/quest/package-info.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestInstance.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/quest/QuestServiceTest.java`

## Steps

- [ ] **Step 1: Write failing single-slot test**

Create `QuestServiceTest`:

```java
@Test
public void characterOnlyHasOneQuestSlot() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	QuestService service = new QuestService(data, relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000008");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setAffection(150);

	boolean first = service.publishFixedQuest(playerUuid, GirlfriendTypes.MOMO_ID, 1, 10L);
	boolean second = service.publishFixedQuest(playerUuid, GirlfriendTypes.MOMO_ID, 1, 10L);

	Assertions.assertTrue(first);
	Assertions.assertFalse(second);
	Assertions.assertNotNull(data.getCharacterState(GirlfriendTypes.MOMO_ID).getCurrentQuest());
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.quest.QuestServiceTest
```

Expected: compilation fails because `QuestService` does not exist.

- [ ] **Step 2: Implement QuestDefinition and objective interface**

Create `QuestDefinition` record with objective composition:

```java
public record QuestDefinition(String questId, QuestType questType, ResourceLocation girlfriendTypeId, int fixedIndex,
		AffectionStage requiredStage, QuestObjectiveGroup objectives) {
}
```

Create `QuestObjectiveHandler` with methods:

```java
boolean canAccept(QuestInstance questInstance);
void onAccept(QuestInstance questInstance);
void onEvent(QuestInstance questInstance, Object event);
boolean isCompleted(QuestInstance questInstance);
CompoundTag serializeProgress(QuestInstance questInstance);
void deserializeProgress(QuestInstance questInstance, CompoundTag tag);
```

Create `QuestObjectiveGroup` as a composition container, not inheritance. Java does not support class multi-inheritance, so a quest that requires item delivery, structure arrival, and staying near a block for a duration must hold multiple objective handlers and require all of them to complete:

```java
public class QuestObjectiveGroup {
	private final List<QuestObjectiveHandler> objectives;

	public boolean isCompleted(QuestInstance questInstance) {
		return this.objectives.stream().allMatch(objective -> objective.isCompleted(questInstance));
	}
}
```

Implement the first three objective handler types as generic framework classes: `ItemDeliveryObjectiveHandler`, `StructureVisitObjectiveHandler`, and `BlockStayObjectiveHandler`.

Create `FixedQuestDefinitionManager` and `RandomQuestTemplateManager`, both extending `SimplePreparableReloadListener`. They load quest JSON from:

```text
data/<namespace>/girlfriends/fixed_quests/<path>.json
data/<namespace>/girlfriends/random_quest_templates/<path>.json
```

The Managers parse objective arrays into `QuestObjectiveGroup`, then publish immutable maps keyed by quest ID.

- [ ] **Step 3: Implement QuestService fixed publication**

Implement:

```java
public boolean publishFixedQuest(UUID playerUuid, ResourceLocation girlfriendTypeId, int fixedIndex, long currentDay)
public boolean acceptCurrentQuest(UUID playerUuid, ResourceLocation girlfriendTypeId)
public boolean completeCurrentQuest(UUID playerUuid, ResourceLocation girlfriendTypeId)
```

Rules:

1. Return false when current slot is not null.
2. Fixed quest required stage is inferred from index: 1 familiar, 2-4 trust, 5-7 affection, 8-9 intimate, 10 home partner prerequisite.
3. Check previous fixed quest completion before publishing index greater than 1.
4. `acceptCurrentQuest` must atomically verify `ownerPlayerUuid == null` before setting owner UUID.
5. If `ownerPlayerUuid` already exists and differs from the requester, return false and leave the quest unchanged.
6. If `ownerPlayerUuid` equals requester, return true idempotently.
7. `completeCurrentQuest` must verify requester equals `ownerPlayerUuid`; helpers can contribute progress but cannot complete or claim rewards.
8. Completing fixed quest records fixed index in relation.

- [ ] **Step 4: Run single-slot test**

Run the QuestService test command. Expected: test passes.

- [ ] **Step 5: Add owner race test**

Add a test where two players try to accept the same published quest:

```java
@Test
public void secondPlayerCannotStealAcceptedQuest() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	QuestService service = new QuestService(data, relationshipService);
	UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000018");
	UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000019");
	data.getOrCreateRelation(firstPlayer, GirlfriendTypes.MOMO_ID).setAffection(150);
	data.getOrCreateRelation(secondPlayer, GirlfriendTypes.MOMO_ID).setAffection(150);
	service.publishFixedQuest(firstPlayer, GirlfriendTypes.MOMO_ID, 1, 10L);

	Assertions.assertTrue(service.acceptCurrentQuest(firstPlayer, GirlfriendTypes.MOMO_ID));
	Assertions.assertFalse(service.acceptCurrentQuest(secondPlayer, GirlfriendTypes.MOMO_ID));
	Assertions.assertEquals(firstPlayer, data.getCharacterState(GirlfriendTypes.MOMO_ID).getCurrentQuest().getOwnerPlayerUuid());
}
```

Run the QuestService test command. Expected: test passes.

- [ ] **Step 6: Add random refresh and expiration tests**

Add tests for:

1. `refreshRandomQuest(GirlfriendTypes.MOMO_ID, 100L)` creates a random quest when slot empty.
2. `expireRandomQuest(GirlfriendTypes.MOMO_ID, 111L)` clears the slot when `expireDay <= currentDay`.
3. Random quest does not replace accepted fixed quest.

- [ ] **Step 7: Implement random quest methods**

Implement:

```java
public boolean refreshRandomQuest(ResourceLocation girlfriendTypeId, long currentDay)
public boolean expireRandomQuest(ResourceLocation girlfriendTypeId, long currentDay)
```

Random expiration day can be deterministic for tests by adding constructor-injected `IntSupplier randomDaysSupplier`, defaulting to 5..10.

- [ ] **Step 8: Run QuestService tests**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.quest.QuestServiceTest
```

Expected: tests pass.

- [ ] **Step 9: Run full test task**

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 10: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/quest src/test/java/com/hexagram2021/girlfriends/common/quest
git commit -m "feat(v0.1.0): add quest slot framework"
```
