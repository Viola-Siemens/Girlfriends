package com.hexagram2021.girlfriends.common.network;

import com.google.common.collect.Lists;
import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.KnownGiftPreferenceSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.QuestInstance;
import com.hexagram2021.girlfriends.common.quest.QuestObjectiveGroup;
import com.hexagram2021.girlfriends.common.quest.QuestService;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
	private final QuestService questService;

	/**
	 * 创建角色交互摘要服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 * @param questService 委托服务喵~
	 */
	public InteractionSummaryService(GirlfriendsWorldData worldData, RelationshipService relationshipService, QuestService questService) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
		this.questService = questService;
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
		FollowMode followMode = state != null ? state.getFollowMode() : FollowMode.NONE;
		return new InteractionSummary(
				girlfriendTypeId,
				stage,
				computeStageProgress(relation.getAffection(), stage),
				canGiveGift(state),
				canAcceptQuest(state),
				canFollow(relation, state),
				canInviteHome(relation, stage),
				followMode,
				buildKnownGiftPreferences(relation),
				buildQuestContentSummary(state != null ? state.getCurrentQuest() : null),
				needsIntimacyConfirmation(relation)
		);
	}

	/**
	 * 构建角色委托图标摘要喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 委托图标摘要喵~
	 */
	@Nullable
	public QuestIconSummary buildQuestIcon(Identifier girlfriendTypeId) {
		CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
		QuestContentSummary quest = buildQuestContentSummary(state != null ? state.getCurrentQuest() : null);
		if(quest == null) {
			return null;
		}
		return new QuestIconSummary(girlfriendTypeId, quest.questId(), quest.questType(), quest.questState(), quest.titleKey(), quest.objectiveSummaryKeys());
	}

	private static float computeStageProgress(float affection, AffectionStage stage) {
		float min = stage.getMinAffection();
		float max = stage.getMaxAffection();
		if(max <= min) {
			return 1.0F;
		}
		float progress = (affection - min) / (max - min);
		return Mth.clamp(progress, 0.0F, 1.0F);
	}

	@Contract("_,null -> false")
	private static boolean canFollow(PlayerCharacterRelation relation, @Nullable CharacterWorldState state) {
		return state != null && state.isAlive() && relation.isConfirmedIntimacy();
	}

	@Contract("null -> false")
	private static boolean canGiveGift(@Nullable CharacterWorldState state) {
		return state != null && state.isAlive();
	}

	@Contract("null -> false")
	private static boolean canAcceptQuest(@Nullable CharacterWorldState state) {
		if(state == null || !state.isAlive()) {
			return false;
		}
		QuestInstance quest = state.getCurrentQuest();
		return quest != null && quest.getState() == QuestState.AVAILABLE && quest.getOwnerPlayerUuid() == null;
	}

	/**
	 * 判断是否需要确认亲密关系喵~
	 * <p>
	 * 好感度达到 700 且尚未确认亲密关系时需要提示玩家喵~
	 */
	private static boolean needsIntimacyConfirmation(PlayerCharacterRelation relation) {
		return relation.getAffection() >= 700.0F && !relation.isConfirmedIntimacy();
	}

	private static boolean canInviteHome(PlayerCharacterRelation relation, AffectionStage stage) {
		return stage == AffectionStage.HOME_PARTNER;
	}

	private static List<KnownGiftPreferenceSummary> buildKnownGiftPreferences(PlayerCharacterRelation relation) {
		return relation.getKnownGiftPreferences().stream()
				.map(InteractionSummaryService::parseKnownGiftPreference)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(summary -> summary.itemOrTagId().toString()))
				.toList();
	}

	@Nullable
	private static KnownGiftPreferenceSummary parseKnownGiftPreference(String value) {
		try {
			if(value.startsWith("#")) {
				return new KnownGiftPreferenceSummary(Identifier.parse(value.substring(1)), GiftPreferenceLevel.ACCEPTED, true);
			}
			return new KnownGiftPreferenceSummary(Identifier.parse(value), GiftPreferenceLevel.ACCEPTED, false);
		} catch(IdentifierException _) {
			return null;
		}
	}

	@Nullable
	private QuestContentSummary buildQuestContentSummary(@Nullable QuestInstance quest) {
		if(quest == null) {
			return null;
		}
		Identifier questId = parseQuestIdentifierOrNull(quest.getQuestId());
		if(questId == null) {
			return null;
		}
		List<String> objectiveSummaryKeys = Lists.newArrayList();
		for(String key : quest.getProgress().keySet()) {
			if(key.startsWith("objective_")) {
				objectiveSummaryKeys.add(QUEST_KEY_PREFIX + questId.getPath() + "." + key);
			}
		}
		objectiveSummaryKeys.sort(String::compareTo);

		boolean questCompleted = false;
		QuestObjectiveGroup objectives = this.questService.resolveObjectives(quest);
		if(objectives != null) {
			questCompleted = objectives.isCompleted(quest);
		}

		return new QuestContentSummary(
				questId,
				quest.getQuestType(),
				quest.getState(),
				QUEST_KEY_PREFIX + questId.getPath() + ".title",
				QUEST_KEY_PREFIX + questId.getPath() + ".description",
				List.copyOf(objectiveSummaryKeys),
				questCompleted
		);
	}

	@Nullable
	private static Identifier parseQuestIdentifierOrNull(String questId) {
		try {
			return Identifier.parse(questId);
		} catch(IdentifierException _) {
			return null;
		}
	}
}
