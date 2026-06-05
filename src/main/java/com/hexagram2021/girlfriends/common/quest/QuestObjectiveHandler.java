package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;

/**
 * 委托目标处理器喵~
 *
 * @author liudongyu
 */
public interface QuestObjectiveHandler {
	/**
	 * 判断委托是否可接取喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 是否可接取喵~
	 */
	boolean canAccept(QuestInstance questInstance);

	/**
	 * 在接取时初始化目标进度喵~
	 *
	 * @param questInstance 委托实例喵~
	 */
	void onAccept(QuestInstance questInstance);

	/**
	 * 处理目标相关事件喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @param event 事件对象喵~
	 */
	void onEvent(QuestInstance questInstance, Object event);

	/**
	 * 判断目标是否完成喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 是否完成喵~
	 */
	boolean isCompleted(QuestInstance questInstance);

	/**
	 * 序列化目标进度喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 目标进度数据喵~
	 */
	CompoundTag serializeProgress(QuestInstance questInstance);

	/**
	 * 反序列化目标进度喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @param tag 目标进度数据喵~
	 */
	void deserializeProgress(QuestInstance questInstance, CompoundTag tag);
}
