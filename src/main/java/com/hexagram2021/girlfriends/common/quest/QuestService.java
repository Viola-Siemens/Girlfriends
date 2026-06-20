package com.hexagram2021.girlfriends.common.quest;

import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * 委托状态与规则服务喵~
 *
 * @author liudongyu
 */
public class QuestService {
	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;
	private final Function<String, @Nullable QuestDefinition> fixedQuestDefinitionGetter;
	private final Function<Identifier, @Nullable QuestDefinition> randomQuestDefinitionGetter;
	private final ToIntFunction<RandomSource> randomDaysSupplier;

	/**
	 * 创建委托服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 */
	public QuestService(GirlfriendsWorldData worldData, RelationshipService relationshipService) {
		this(worldData, relationshipService, _ -> null, _ -> null, QuestService::defaultRandomDays);
	}

	/**
	 * 创建可注入定义来源的委托服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 * @param fixedQuestDefinitionGetter 固定委托定义提供器喵~
	 * @param randomQuestDefinitionGetter 随机委托定义提供器喵~
	 * @param randomDaysSupplier 随机有效天数提供器喵~
	 */
	public QuestService(
			GirlfriendsWorldData worldData,
			RelationshipService relationshipService,
			Function<String, @Nullable QuestDefinition> fixedQuestDefinitionGetter,
			Function<Identifier, @Nullable QuestDefinition> randomQuestDefinitionGetter,
			ToIntFunction<RandomSource> randomDaysSupplier
	) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
		this.fixedQuestDefinitionGetter = fixedQuestDefinitionGetter;
		this.randomQuestDefinitionGetter = randomQuestDefinitionGetter;
		this.randomDaysSupplier = randomDaysSupplier;
	}

	/**
	 * 发布固定委托喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param fixedIndex 固定委托序号喵~
	 * @param currentDay 当前游戏日喵~
	 * @return 是否发布成功喵~
	 */
	public boolean publishFixedQuest(UUID playerUuid, Identifier girlfriendTypeId, int fixedIndex, long currentDay) {
		if(this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest() != null) {
			return false;
		}
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		AffectionStage requiredStage = resolveRequiredStage(fixedIndex);
		if(this.relationshipService.getEffectiveStage(relation).ordinal() < requiredStage.ordinal()) {
			return false;
		}
		if(fixedIndex > 1 && !relation.getCompletedFixedQuests().contains(fixedIndex - 1)) {
			return false;
		}
		if(requiredStage == AffectionStage.HOME_PARTNER && !relation.isHomePartner()) {
			return false;
		}
		QuestDefinition definition = this.resolveFixedDefinition(girlfriendTypeId, fixedIndex, requiredStage);
		QuestInstance instance = new QuestInstance();
		instance.setQuestInstanceId(UUID.randomUUID());
		instance.setCharacterId(girlfriendTypeId);
		instance.setQuestType(QuestType.FIXED);
		instance.setQuestId(definition.questId());
		instance.setFixedIndex(fixedIndex);
		instance.setRequiredStage(requiredStage);
		instance.setState(QuestState.AVAILABLE);
		instance.setCreatedDay(currentDay);
		instance.setExpireDay(null);
		if(!definition.objectives().canAccept(instance)) {
			return false;
		}
		instance.setProgress(definition.objectives().serializeProgress(instance));
		this.worldData.updateCharacter(girlfriendTypeId, state -> state.setCurrentQuest(instance));
		return true;
	}

	/**
	 * 接取当前委托喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否接取成功喵~
	 */
	public boolean acceptCurrentQuest(UUID playerUuid, Identifier girlfriendTypeId) {
		QuestInstance currentQuest = this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest();
		if(currentQuest == null) {
			return false;
		}
		UUID ownerPlayerUuid = currentQuest.getOwnerPlayerUuid();
		if(ownerPlayerUuid != null) {
			return ownerPlayerUuid.equals(playerUuid);
		}
		if(!this.canAcceptQuest(playerUuid, currentQuest)) {
			return false;
		}
		QuestObjectiveGroup objectives = this.resolveObjectives(currentQuest);
		if(objectives == null || !objectives.canAccept(currentQuest)) {
			return false;
		}
		this.worldData.updateCharacter(girlfriendTypeId, state -> {
			QuestInstance quest = state.getCurrentQuest();
			if(quest == null || quest.getOwnerPlayerUuid() != null) {
				return;
			}
			quest.setOwnerPlayerUuid(playerUuid);
			quest.setState(QuestState.ACCEPTED);
			objectives.onAccept(quest);
		});
		QuestInstance updatedQuest = this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest();
		return updatedQuest != null && playerUuid.equals(updatedQuest.getOwnerPlayerUuid());
	}

	/**
	 * 完成当前委托喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否完成成功喵~
	 */
	public boolean completeCurrentQuest(UUID playerUuid, Identifier girlfriendTypeId) {
		QuestInstance currentQuest = this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest();
		if(currentQuest == null || !playerUuid.equals(currentQuest.getOwnerPlayerUuid())) {
			return false;
		}
		QuestObjectiveGroup objectives = this.resolveObjectives(currentQuest);
		if(objectives == null || !objectives.isCompleted(currentQuest)) {
			return false;
		}
		Integer fixedIndex = currentQuest.getFixedIndex();
		QuestType questType = currentQuest.getQuestType();
		this.worldData.updateCharacter(girlfriendTypeId, state -> state.setCurrentQuest(null));
		if(questType == QuestType.FIXED && fixedIndex != null) {
			this.worldData.updateRelation(playerUuid, girlfriendTypeId, relation -> relation.getCompletedFixedQuests().add(fixedIndex));
		}
		return true;
	}

	/**
	 * 刷新随机委托喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param currentDay 当前游戏日喵~
	 * @param randomSource 随机数发生器喵~
	 * @return 是否刷新成功喵~
	 */
	public boolean refreshRandomQuest(Identifier girlfriendTypeId, long currentDay, RandomSource randomSource) {
		QuestInstance currentQuest = this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest();
		if(currentQuest != null) {
			return false;
		}
		QuestDefinition definition = this.resolveRandomDefinition(girlfriendTypeId);
		QuestInstance instance = new QuestInstance();
		instance.setQuestInstanceId(UUID.randomUUID());
		instance.setCharacterId(girlfriendTypeId);
		instance.setQuestType(QuestType.RANDOM);
		instance.setQuestId(definition.questId());
		instance.setFixedIndex(null);
		instance.setRequiredStage(definition.requiredStage());
		instance.setState(QuestState.AVAILABLE);
		instance.setCreatedDay(currentDay);
		instance.setExpireDay(currentDay + Math.max(1, this.randomDaysSupplier.applyAsInt(randomSource)));
		instance.setProgress(definition.objectives().serializeProgress(instance));
		this.worldData.updateCharacter(girlfriendTypeId, state -> state.setCurrentQuest(instance));
		return true;
	}

	/**
	 * 过期随机委托喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param currentDay 当前游戏日喵~
	 * @return 是否执行了过期清理喵~
	 */
	public boolean expireRandomQuest(Identifier girlfriendTypeId, long currentDay) {
		QuestInstance currentQuest = this.worldData.getCharacterState(girlfriendTypeId).getCurrentQuest();
		if(currentQuest == null || currentQuest.getQuestType() != QuestType.RANDOM) {
			return false;
		}
		Long expireDay = currentQuest.getExpireDay();
		if(expireDay == null || expireDay > currentDay) {
			return false;
		}
		this.worldData.updateCharacter(girlfriendTypeId, state -> state.setCurrentQuest(null));
		return true;
	}

	private QuestDefinition resolveFixedDefinition(Identifier girlfriendTypeId, int fixedIndex, AffectionStage requiredStage) {
		String questId = buildFixedQuestId(girlfriendTypeId, fixedIndex);
		QuestDefinition definition = this.fixedQuestDefinitionGetter.apply(questId);
		if(definition != null) {
			return definition;
		}
		return new QuestDefinition(questId, QuestType.FIXED, girlfriendTypeId, fixedIndex, requiredStage, QuestObjectiveGroup.empty());
	}

	private QuestDefinition resolveRandomDefinition(Identifier girlfriendTypeId) {
		QuestDefinition definition = this.randomQuestDefinitionGetter.apply(girlfriendTypeId);
		if(definition != null) {
			return definition;
		}
		return new QuestDefinition(girlfriendTypeId + "/random", QuestType.RANDOM, girlfriendTypeId, 0, AffectionStage.STRANGER, QuestObjectiveGroup.empty());
	}

	private boolean canAcceptQuest(UUID playerUuid, QuestInstance questInstance) {
		if(questInstance.getQuestType() != QuestType.FIXED) {
			return true;
		}
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, questInstance.getCharacterId());
		AffectionStage requiredStage = questInstance.getRequiredStage();
		if(this.relationshipService.getEffectiveStage(relation).ordinal() < requiredStage.ordinal()) {
			return false;
		}
		Integer fixedIndex = questInstance.getFixedIndex();
		if(fixedIndex != null && fixedIndex > 1 && !relation.getCompletedFixedQuests().contains(fixedIndex - 1)) {
			return false;
		}
		return requiredStage != AffectionStage.HOME_PARTNER || relation.isHomePartner();
	}

	@Nullable
	public QuestObjectiveGroup resolveObjectives(QuestInstance questInstance) {
		if(questInstance.getQuestType() == QuestType.FIXED) {
			QuestDefinition definition = this.fixedQuestDefinitionGetter.apply(questInstance.getQuestId());
			if(definition != null) {
				return definition.objectives();
			}
			return this.isFallbackFixedQuest(questInstance) ? QuestObjectiveGroup.empty() : null;
		}
		QuestDefinition definition = this.randomQuestDefinitionGetter.apply(questInstance.getCharacterId());
		if(definition != null && definition.questId().equals(questInstance.getQuestId())) {
			return definition.objectives();
		}
		return this.isFallbackRandomQuest(questInstance) ? QuestObjectiveGroup.empty() : null;
	}

	private boolean isFallbackFixedQuest(QuestInstance questInstance) {
		Integer fixedIndex = questInstance.getFixedIndex();
		return fixedIndex != null && buildFixedQuestId(questInstance.getCharacterId(), fixedIndex).equals(questInstance.getQuestId());
	}

	private boolean isFallbackRandomQuest(QuestInstance questInstance) {
		return (questInstance.getCharacterId() + "/random").equals(questInstance.getQuestId());
	}

	private static String buildFixedQuestId(@Nullable Identifier girlfriendTypeId, int fixedIndex) {
		return girlfriendTypeId + "/fixed_" + fixedIndex;
	}

	private static AffectionStage resolveRequiredStage(int fixedIndex) {
		if(fixedIndex <= 1) {
			return AffectionStage.FAMILIAR;
		}
		if(fixedIndex <= 4) {
			return AffectionStage.TRUST;
		}
		if(fixedIndex <= 7) {
			return AffectionStage.AFFECTION;
		}
		if(fixedIndex <= 9) {
			return AffectionStage.INTIMATE;
		}
		return AffectionStage.HOME_PARTNER;
	}

	private static int defaultRandomDays(RandomSource random) {
		return 5 + random.nextInt(6);
	}
}
