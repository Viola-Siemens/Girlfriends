package com.hexagram2021.girlfriends.common.network;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端交互摘要存储，用于缓存服务端同步的交互状态喵~
 *
 * @author liudongyu
 */
public final class ClientInteractionStore {
	private static final Map<Identifier, InteractionSummary> SUMMARIES = new ConcurrentHashMap<>();
	private static final Map<Identifier, QuestIconSummary> QUEST_ICONS = new ConcurrentHashMap<>();
	private static final Set<Identifier> PENDING_INTERACTIONS = ConcurrentHashMap.newKeySet();

	/**
	 * 标记待处理的交互，用于 mobInteract 客户端侧触发界面打开喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 */
	public static void markPendingInteraction(Identifier girlfriendTypeId) {
		PENDING_INTERACTIONS.add(girlfriendTypeId);
	}

	/**
	 * 原子获取并清除待处理交互标记喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否存在待处理标记喵~
	 */
	public static boolean consumePendingInteraction(Identifier girlfriendTypeId) {
		return PENDING_INTERACTIONS.remove(girlfriendTypeId);
	}

	/**
	 * 存储交互摘要喵~
	 *
	 * @param summary 交互摘要喵~
	 */
	public static void setSummary(InteractionSummary summary) {
		SUMMARIES.put(summary.girlfriendTypeId(), summary);
	}

	/**
	 * 获取已缓存的交互摘要喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 交互摘要，未缓存时返回 null 喵~
	 */
	@Nullable
	public static InteractionSummary getSummary(Identifier girlfriendTypeId) {
		return SUMMARIES.get(girlfriendTypeId);
	}

	/**
	 * 存储委托图标摘要喵~
	 *
	 * @param summary 委托图标摘要喵~
	 */
	public static void setQuestIcon(@Nullable QuestIconSummary summary) {
		if(summary == null) {
			return;
		}
		QUEST_ICONS.put(summary.girlfriendTypeId(), summary);
	}

	/**
	 * 获取已缓存的委托图标摘要喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 委托图标摘要，未缓存时返回 null 喵~
	 */
	@Nullable
	public static QuestIconSummary getQuestIcon(Identifier girlfriendTypeId) {
		return QUEST_ICONS.get(girlfriendTypeId);
	}

	/**
	 * 清除指定角色的全部缓存喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 */
	public static void clear(Identifier girlfriendTypeId) {
		SUMMARIES.remove(girlfriendTypeId);
		QUEST_ICONS.remove(girlfriendTypeId);
	}

	/**
	 * 清除全部缓存喵~
	 */
	public static void clearAll() {
		SUMMARIES.clear();
		QUEST_ICONS.clear();
	}

	private ClientInteractionStore() {
	}
}
