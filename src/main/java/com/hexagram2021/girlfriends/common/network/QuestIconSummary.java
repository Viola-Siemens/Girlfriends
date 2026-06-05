package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.quest.QuestType;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 角色委托图标摘要喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param questId 委托 ID 喵~
 * @param questType 委托类型喵~
 * @param questState 委托状态喵~
 * @param titleKey 标题本地化键喵~
 * @param objectiveSummaryKeys 目标摘要本地化键列表喵~
 * @author liudongyu
 */
public record QuestIconSummary(
		Identifier girlfriendTypeId,
		Identifier questId,
		QuestType questType,
		QuestState questState,
		String titleKey,
		List<String> objectiveSummaryKeys
) {
}
