package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.KnownGiftPreferenceSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.QuestInstance;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 角色交互摘要服务喵~
 *
 * @author liudongyu
 */
public class InteractionSummaryService {
	private static final String QUEST_KEY_PREFIX = "quest." + GirlfriendsMod.MODID + ".";
	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;

	/**
	 * 创建角色交互摘要服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 */
	public InteractionSummaryService(GirlfriendsWorldData worldData, RelationshipService relationshipService) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
	}

	/**
	 * 构建当前玩家与角色的交互摘要喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 交互摘要喵~
	 */
	public InteractionSummary build(UUID playerUuid, Identifier girlfriendTypeId) {
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		AffectionStage stage = this.relationshipService.getEffectiveStage(relation);
		CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
		return new InteractionSummary(
				girlfriendTypeId,
				stage,
				computeStageProgress(relation.getAffection(), stage),
				canGiveGift(state),
				canAcceptQuest(state),
				canFollow(relation, state),
				canInviteHome(relation, stage),
				buildKnownGiftPreferences(relation),
				buildQuestContentSummary(state)
		);
	}

	/**
	 * 构建角色委托图标摘要喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 委托图标摘要喵~
	 */
	public QuestIconSummary buildQuestIcon(Identifier girlfriendTypeId) {
		CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
		QuestContentSummary quest = buildQuestContentSummary(state);
		if(quest == null) {
			return null;
		}
		return new QuestIconSummary(girlfriendTypeId, quest.questId(), quest.questType(), quest.questState(), quest.titleKey(), quest.objectiveSummaryKeys());
	}

	private static double computeStageProgress(int affection, AffectionStage stage) {
		int min = stage.getMinAffection();
		int max = stage.getMaxAffection();
		if(max <= min) {
			return 1.0D;
		}
		double progress = (double)(affection - min) / (double)(max - min);
		return Math.max(0.0D, Math.min(1.0D, progress));
	}

	private static boolean canFollow(PlayerCharacterRelation relation, CharacterWorldState state) {
		return state != null && state.isAlive() && relation.isConfirmedIntimacy();
	}

	private static boolean canGiveGift(CharacterWorldState state) {
		return state != null && state.isAlive();
	}

	private static boolean canAcceptQuest(CharacterWorldState state) {
		if(state == null || !state.isAlive()) {
			return false;
		}
		QuestInstance quest = state.getCurrentQuest();
		return quest != null && quest.getState() == QuestState.AVAILABLE && quest.getOwnerPlayerUuid() == null;
	}

	private static boolean canInviteHome(PlayerCharacterRelation relation, AffectionStage stage) {
		return stage == AffectionStage.HOME_PARTNER;
	}

	private static List<KnownGiftPreferenceSummary> buildKnownGiftPreferences(PlayerCharacterRelation relation) {
		return relation.getKnownGiftPreferences().stream()
				.map(InteractionSummaryService::parseKnownGiftPreference)
				.filter(summary -> summary != null)
				.sorted(Comparator.comparing(summary -> summary.itemOrTagId().toString()))
				.toList();
	}

	private static KnownGiftPreferenceSummary parseKnownGiftPreference(String value) {
		try {
			if(value.startsWith("#")) {
				return new KnownGiftPreferenceSummary(Identifier.parse(value.substring(1)), GiftPreferenceLevel.ACCEPTED, true);
			}
			return new KnownGiftPreferenceSummary(Identifier.parse(value), GiftPreferenceLevel.ACCEPTED, false);
		} catch(IdentifierException ignored) {
			return null;
		}
	}

	private static QuestContentSummary buildQuestContentSummary(CharacterWorldState state) {
		if(state == null || state.getCurrentQuest() == null) {
			return null;
		}
		QuestInstance quest = state.getCurrentQuest();
		Identifier questId = parseQuestIdentifierOrNull(quest.getQuestId());
		if(questId == null) {
			return null;
		}
		List<String> objectiveSummaryKeys = new ArrayList<>();
		for(String key : quest.getProgress().keySet()) {
			if(key.startsWith("objective_")) {
				objectiveSummaryKeys.add(QUEST_KEY_PREFIX + questId.getPath() + "." + key);
			}
		}
		objectiveSummaryKeys.sort(String::compareTo);
		return new QuestContentSummary(
				questId,
				quest.getQuestType(),
				quest.getState(),
				QUEST_KEY_PREFIX + questId.getPath() + ".title",
				QUEST_KEY_PREFIX + questId.getPath() + ".description",
				List.copyOf(objectiveSummaryKeys)
		);
	}

	private static Identifier parseQuestIdentifierOrNull(String questId) {
		try {
			return Identifier.parse(questId);
		} catch(IdentifierException ignored) {
			return null;
		}
	}
}
