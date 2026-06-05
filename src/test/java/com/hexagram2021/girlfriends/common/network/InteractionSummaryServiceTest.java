package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.QuestInstance;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.quest.QuestType;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionSummaryServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	private static final Identifier QUEST_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo_fixed_1");

	@Test
	void summaryExposesStageAndProgressWithoutRawAffection() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		UUID playerUuid = UUID.randomUUID();
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, MOMO_ID);
		relation.setAffection(650);
		relation.getKnownGiftPreferences().add("minecraft:apple");
		relation.getKnownGiftPreferences().add("#minecraft:flowers");
		relation.getKnownGiftPreferences().add("invalid id");

		InteractionSummaryService service = new InteractionSummaryService(data, new RelationshipService(data));
		InteractionSummary summary = service.build(playerUuid, MOMO_ID);

		assertEquals(AffectionStage.AFFECTION, summary.stage());
		assertTrue(summary.stageProgress() >= 0.0D && summary.stageProgress() <= 1.0D);
		assertEquals(2, summary.knownGiftPreferences().size());
		assertFalse(summary.canGiveGift());
		assertFalse(summary.canAcceptQuest());
		assertFalse(summary.canInviteHome());
	}

	@Test
	void summaryBuildsUiSafeQuestKeysOnly() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		UUID playerUuid = UUID.randomUUID();
		CharacterWorldState state = data.getCharacterState(MOMO_ID);
		QuestInstance quest = new QuestInstance();
		quest.setCharacterId(MOMO_ID);
		quest.setQuestId(QUEST_ID.toString());
		quest.setQuestType(QuestType.FIXED);
		quest.setState(QuestState.ACCEPTED);
		CompoundTag progress = new CompoundTag();
		progress.putInt("objective_collect", 3);
		progress.putInt("raw_counter", 9);
		quest.setProgress(progress);
		state.setCurrentQuest(quest);

		InteractionSummaryService service = new InteractionSummaryService(data, new RelationshipService(data));
		InteractionSummary summary = service.build(playerUuid, MOMO_ID);

		assertNotNull(summary.currentQuest());
		assertEquals(QUEST_ID, summary.currentQuest().questId());
		assertEquals("quest.girlfriends.momo_fixed_1.title", summary.currentQuest().titleKey());
		assertEquals("quest.girlfriends.momo_fixed_1.description", summary.currentQuest().descriptionKey());
		assertEquals(1, summary.currentQuest().objectiveSummaryKeys().size());
		assertEquals("quest.girlfriends.momo_fixed_1.objective_collect", summary.currentQuest().objectiveSummaryKeys().getFirst());
	}

	@Test
	void invalidQuestIdentifierDoesNotCreateQuestSummary() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		UUID playerUuid = UUID.randomUUID();
		CharacterWorldState state = data.getCharacterState(MOMO_ID);
		QuestInstance quest = new QuestInstance();
		quest.setQuestId("invalid id");
		state.setCurrentQuest(quest);

		InteractionSummaryService service = new InteractionSummaryService(data, new RelationshipService(data));

		assertNull(service.build(playerUuid, MOMO_ID).currentQuest());
		assertNull(service.buildQuestIcon(MOMO_ID));
	}
}
