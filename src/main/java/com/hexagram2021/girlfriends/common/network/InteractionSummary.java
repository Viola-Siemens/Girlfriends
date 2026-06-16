package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.quest.QuestType;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 角色交互界面摘要喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param stage 展示用好感阶段喵~
 * @param stageProgress 当前阶段进度喵~
 * @param canGiveGift 是否可赠礼喵~
 * @param canAcceptQuest 是否可接取委托喵~
 * @param canFollow 是否可跟随喵~
 * @param canInviteHome 是否可邀请回家喵~
 * @param followMode 当前跟随模式喵~
 * @param knownGiftPreferences 已发现礼物偏好喵~
 * @param currentQuest 当前委托摘要喵~
 * @param needsIntimacyConfirmation 是否需要确认亲密关系喵~
 * @author liudongyu
 */
public record InteractionSummary(
		Identifier girlfriendTypeId,
		AffectionStage stage,
		double stageProgress,
		boolean canGiveGift,
		boolean canAcceptQuest,
		boolean canFollow,
		boolean canInviteHome,
		FollowMode followMode,
		List<KnownGiftPreferenceSummary> knownGiftPreferences,
		@Nullable QuestContentSummary currentQuest,
		boolean needsIntimacyConfirmation
) {
	/**
	 * 已发现礼物偏好摘要喵~
	 *
	 * @param itemOrTagId 物品或标签 ID 喵~
	 * @param level 偏好等级喵~
	 * @param tag 是否标签偏好喵~
	 */
	public record KnownGiftPreferenceSummary(Identifier itemOrTagId, GiftPreferenceLevel level, boolean tag) {
	}

	/**
	 * 当前委托内容摘要喵~
	 *
	 * @param questId 委托 ID 喵~
	 * @param questType 委托类型喵~
	 * @param questState 委托状态喵~
	 * @param titleKey 标题本地化键喵~
	 * @param descriptionKey 描述本地化键喵~
	 * @param objectiveSummaryKeys 目标摘要本地化键列表喵~
	 * @param questCompleted 委托目标是否已完成喵~
	 */
	public record QuestContentSummary(
			Identifier questId,
			QuestType questType,
			QuestState questState,
			String titleKey,
			String descriptionKey,
			List<String> objectiveSummaryKeys,
			boolean questCompleted
	) {
	}
}
