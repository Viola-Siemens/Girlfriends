package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;

/**
 * 结构到访目标处理器喵~
 *
 * @author liudongyu
 */
public class StructureVisitObjectiveHandler implements QuestObjectiveHandler {
	private static final String KEY_STRUCTURE_ID = "structure_id";
	private static final String KEY_VISITED = "visited";
	private final String structureId;

	/**
	 * 创建结构到访目标处理器喵~
	 *
	 * @param structureId 目标结构 ID 喵~
	 */
	public StructureVisitObjectiveHandler(String structureId) {
		this.structureId = structureId;
	}

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		tag.putString(KEY_STRUCTURE_ID, this.structureId);
		tag.putBoolean(KEY_VISITED, tag.getBoolean(KEY_VISITED).orElse(false));
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if(!(event instanceof StructureVisitEvent structureVisitEvent)) {
			return;
		}
		if(!this.structureId.equals(structureVisitEvent.structureId())) {
			return;
		}
		this.getOrCreateProgressTag(questInstance).putBoolean(KEY_VISITED, true);
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		return this.getOrCreateProgressTag(questInstance).getBoolean(KEY_VISITED).orElse(false);
	}

	@Override
	public CompoundTag serializeProgress(QuestInstance questInstance) {
		return this.getOrCreateProgressTag(questInstance).copy();
	}

	@Override
	public void deserializeProgress(QuestInstance questInstance, CompoundTag tag) {
		questInstance.getProgress().put(this.getProgressKey(), tag.copy());
	}

	private CompoundTag getOrCreateProgressTag(QuestInstance questInstance) {
		return questInstance.getProgress().getCompound(this.getProgressKey()).orElseGet(() -> {
			CompoundTag tag = new CompoundTag();
			questInstance.getProgress().put(this.getProgressKey(), tag);
			return tag;
		});
	}

	private String getProgressKey() {
		return "structure_visit:" + this.structureId;
	}

	/**
	 * 结构到访事件喵~
	 *
	 * @param structureId 到访结构 ID 喵~
	 */
	public record StructureVisitEvent(String structureId) {
	}
}
