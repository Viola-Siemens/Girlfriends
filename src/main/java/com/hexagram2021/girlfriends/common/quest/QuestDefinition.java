package com.hexagram2021.girlfriends.common.quest;

import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.resources.Identifier;

/**
 * 委托定义喵~
 *
 * @param questId 委托定义 ID 喵~
 * @param questType 委托类型喵~
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param fixedIndex 固定委托序号喵~
 * @param requiredStage 需要的好感阶段喵~
 * @param objectives 目标组喵~
 * @author liudongyu
 */
public record QuestDefinition(
		String questId,
		QuestType questType,
		Identifier girlfriendTypeId,
		int fixedIndex,
		AffectionStage requiredStage,
		QuestObjectiveGroup objectives
) {
	/**
	 * 创建委托定义并补齐默认目标组喵~
	 */
	public QuestDefinition {
		if(objectives == null) {
			objectives = QuestObjectiveGroup.empty();
		}
	}
}
