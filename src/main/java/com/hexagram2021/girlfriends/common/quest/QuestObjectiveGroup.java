package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * 委托目标组喵~
 *
 * @author liudongyu
 */
public record QuestObjectiveGroup(List<QuestObjectiveHandler> objectives) {
	private static final String OBJECTIVE_PREFIX = "objective_";

	/**
	 * 创建委托目标组喵~
	 *
	 * @param objectives 目标处理器列表喵~
	 */
	public QuestObjectiveGroup(List<QuestObjectiveHandler> objectives) {
		this.objectives = List.copyOf(objectives);
	}

	/**
	 * 创建空目标组喵~
	 *
	 * @return 空目标组喵~
	 */
	public static QuestObjectiveGroup empty() {
		return new QuestObjectiveGroup(List.of());
	}

	/**
	 * 获取全部目标处理器喵~
	 *
	 * @return 目标处理器列表喵~
	 */
	@Override
	public List<QuestObjectiveHandler> objectives() {
		return this.objectives;
	}

	/**
	 * 判断委托是否可接取喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 是否可接取喵~
	 */
	public boolean canAccept(QuestInstance questInstance) {
		return this.objectives.stream().allMatch(objective -> objective.canAccept(questInstance));
	}

	/**
	 * 在接取时初始化全部目标喵~
	 *
	 * @param questInstance 委托实例喵~
	 */
	public void onAccept(QuestInstance questInstance) {
		for (QuestObjectiveHandler objective : this.objectives) {
			objective.onAccept(questInstance);
		}
	}

	/**
	 * 广播事件到全部目标喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @param event         事件对象喵~
	 */
	public void onEvent(QuestInstance questInstance, Object event) {
		for (QuestObjectiveHandler objective : this.objectives) {
			objective.onEvent(questInstance, event);
		}
	}

	/**
	 * 判断全部目标是否完成喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 是否全部完成喵~
	 */
	public boolean isCompleted(QuestInstance questInstance) {
		return this.objectives.stream().allMatch(objective -> objective.isCompleted(questInstance));
	}

	/**
	 * 序列化目标组进度喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @return 序列化结果喵~
	 */
	public CompoundTag serializeProgress(QuestInstance questInstance) {
		CompoundTag tag = new CompoundTag();
		for (int index = 0; index < this.objectives.size(); index++) {
			tag.put(OBJECTIVE_PREFIX + index, this.objectives.get(index).serializeProgress(questInstance));
		}
		return tag;
	}

	/**
	 * 反序列化目标组进度喵~
	 *
	 * @param questInstance 委托实例喵~
	 * @param tag           序列化数据喵~
	 */
	public void deserializeProgress(QuestInstance questInstance, CompoundTag tag) {
		for (int index = 0; index < this.objectives.size(); index++) {
			CompoundTag objectiveTag = tag.getCompound(OBJECTIVE_PREFIX + index).orElseGet(CompoundTag::new);
			this.objectives.get(index).deserializeProgress(questInstance, objectiveTag);
		}
	}
}
