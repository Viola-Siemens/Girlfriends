# Story 9 网络同步与交互摘要 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现交互 GUI 摘要、委托图标摘要、关键关系变化提示和 Serverbound 请求校验边界。

**Architecture:** 客户端只接收摘要 DTO，不读取完整 SavedData。Serverbound handler 只负责反序列化、查找实体和权限校验，然后调用领域服务；领域服务仍是状态变更唯一入口。

**Tech Stack:** Java 25、NeoForge Network API、JUnit Platform、Story 2/3/4/6/7 服务。

---

## Files

- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummary.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/QuestIconSummary.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummaryService.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/clientbound/ClientboundSyncInteractionDataPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/clientbound/ClientboundQuestIconPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundGiveGiftPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundAcceptQuestPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundSetFollowModePacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/package-info.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/network/InteractionSummaryServiceTest.java`
- Test: `src/test/java/com/hexagram2021/girlfriends/common/network/ServerboundPermissionTest.java`

## Steps

- [ ] **Step 1: Write failing interaction summary test**

Create `InteractionSummaryServiceTest`:

```java
@Test
public void summaryDoesNotExposeRawAffectionValue() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	InteractionSummaryService summaryService = new InteractionSummaryService(data, relationshipService);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000016");
	data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).setAffection(150);

	InteractionSummary summary = summaryService.build(playerUuid, GirlfriendTypes.MOMO_ID);

	Assertions.assertEquals(GirlfriendTypes.MOMO_ID, summary.girlfriendTypeId());
	Assertions.assertEquals(AffectionStage.FAMILIAR, summary.stage());
	Assertions.assertTrue(summary.stageProgress() >= 0.0D && summary.stageProgress() <= 1.0D);
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.network.InteractionSummaryServiceTest
```

Expected: compilation fails because network summary classes do not exist.

- [ ] **Step 2: Implement summary DTOs**

Create records:

```java
public record InteractionSummary(ResourceLocation girlfriendTypeId, AffectionStage stage, double stageProgress,
		boolean canGiveGift, boolean canAcceptQuest, boolean canFollow, boolean canInviteHome,
		List<KnownGiftPreferenceSummary> knownGiftPreferences, QuestContentSummary currentQuest) {
}

public record KnownGiftPreferenceSummary(ResourceLocation itemOrTagId, GiftPreferenceLevel level, boolean tag) {
}

public record QuestContentSummary(ResourceLocation questId, QuestType questType, QuestState questState, String titleKey,
		String descriptionKey, List<String> objectiveSummaryKeys) {
}

public record QuestIconSummary(ResourceLocation girlfriendTypeId, ResourceLocation questId, QuestType questType,
		QuestState questState, String titleKey, List<String> objectiveSummaryKeys) {
}
```

Do not include raw affection value in `InteractionSummary`. Known gift preferences may expose only entries already discovered by the current player, and quest content summary may expose only UI-safe title/description/objective localization keys rather than full mutable quest progress NBT.

- [ ] **Step 3: Implement InteractionSummaryService**

Implement:

```java
public InteractionSummary build(UUID playerUuid, ResourceLocation girlfriendTypeId)
```

Rules:

1. Stage comes from `RelationshipService.getEffectiveStage`.
2. Stage progress is calculated inside current numeric stage range.
3. Gift, quest, follow and home booleans call existing service permission seams when available.
4. `knownGiftPreferences` is built from `PlayerCharacterRelation.knownGiftPreferences` and never includes undiscovered preferences.
5. `currentQuest` is built from current quest definition and objective summary keys, not from raw progress NBT.
6. Missing service dependencies default to false or empty summaries, not true.

- [ ] **Step 4: Run summary test**

Run InteractionSummaryService test command. Expected: test passes.

- [ ] **Step 5: Add permission test for serverbound gift seam**

Create `ServerboundPermissionTest` with a pure handler seam:

```java
@Test
public void deniedGiftRequestDoesNotChangeAffection() {
	GirlfriendsWorldData data = new GirlfriendsWorldData();
	RelationshipService relationshipService = new RelationshipService(data);
	GiftService giftService = new GiftService(relationshipService, (playerUuid, girlfriendType) -> false);
	UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000017");

	GiftResult result = giftService.applyGift(playerUuid, GirlfriendTypes.MOMO_ID, GiftPreferenceLevel.FAVORITE);

	Assertions.assertTrue(result.rejected());
	Assertions.assertEquals(0, data.getOrCreateRelation(playerUuid, GirlfriendTypes.MOMO_ID).getAffection());
}
```

Run:

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.network.ServerboundPermissionTest
```

Expected: test passes after Story 3 permission seam exists.

- [ ] **Step 6: Implement packet records after confirming NeoForge API**

Create packet classes using the exact NeoForge 26.1.2 payload API available in the project. Each packet must include:

1. Static `TYPE` or equivalent ID using `girlfriends` namespace.
2. Codec or encode/decode methods required by NeoForge 26.1.2.
3. Handler that enqueues work on server thread.
4. Server-side revalidation before service call.

Do not guess API names. If compilation fails due to network API mismatch, inspect NeoForge examples in dependencies and adjust packet registration before continuing.

- [ ] **Step 7: Implement GirlfriendsNetwork registration**

Create a central registration class and call it from the mod event bus registration point in `GirlfriendsMod`.

The registration must include clientbound summary/icon packets and serverbound gift/quest/follow packets.

- [ ] **Step 8: Run network tests and build**

```bash
./gradlew test --tests com.hexagram2021.girlfriends.common.network.*
./gradlew build
```

Expected: tests and build pass.

- [ ] **Step 9: Commit checkpoint if requested**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network src/test/java/com/hexagram2021/girlfriends/common/network src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java
git commit -m "feat(v0.1.0): add interaction network boundaries"
```
