package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;

/**
 * 战斗目标处理器喵~
 *
 * @author liudongyu
 */
public class FightObjectiveHandler implements QuestObjectiveHandler {
	private static final String PROGRESS_KEY = "fight";

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		getOrCreateProgressTag(questInstance);
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		// TODO: 根据 GDD 实现战斗事件处理逻辑喵~
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		// TODO: 根据 GDD 实现战斗完成判定喵~
		return false;
	}

	@Override
	public CompoundTag serializeProgress(QuestInstance questInstance) {
		return getOrCreateProgressTag(questInstance).copy();
	}

	@Override
	public void deserializeProgress(QuestInstance questInstance, CompoundTag tag) {
		questInstance.getProgress().put(PROGRESS_KEY, tag.copy());
	}

	private CompoundTag getOrCreateProgressTag(QuestInstance questInstance) {
		return questInstance.getProgress().getCompound(PROGRESS_KEY).orElseGet(() -> {
			CompoundTag tag = new CompoundTag();
			questInstance.getProgress().put(PROGRESS_KEY, tag);
			return tag;
		});
	}
}
