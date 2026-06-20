package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 方块驻留目标处理器喵~
 *
 * @author liudongyu
 */
public class BlockStayObjectiveHandler implements QuestObjectiveHandler {
	private static final String KEY_BLOCK_ID = "block_id";
	private static final String KEY_REQUIRED_TICKS = "required_ticks";
	private static final String KEY_ACCUMULATED_TICKS = "accumulated_ticks";
	private final Identifier blockId;
	private final int requiredTicks;

	/**
	 * 创建方块驻留目标处理器喵~
	 *
	 * @param blockId 目标方块 ID 喵~
	 * @param requiredTicks 需要驻留的 Tick 数喵~
	 */
	public BlockStayObjectiveHandler(Identifier blockId, int requiredTicks) {
		this.blockId = blockId;
		this.requiredTicks = Math.max(1, requiredTicks);
	}

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		tag.putString(KEY_BLOCK_ID, this.blockId.toString());
		tag.putInt(KEY_REQUIRED_TICKS, this.requiredTicks);
		tag.putInt(KEY_ACCUMULATED_TICKS, tag.getInt(KEY_ACCUMULATED_TICKS).orElse(0));
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if(!(event instanceof BlockStayEvent blockStayEvent)) {
			return;
		}
		if(!this.blockId.equals(blockStayEvent.blockId())) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		int accumulatedTicks = tag.getInt(KEY_ACCUMULATED_TICKS).orElse(0);
		tag.putInt(KEY_ACCUMULATED_TICKS, Math.min(this.requiredTicks, accumulatedTicks + Math.max(0, blockStayEvent.ticks())));
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		return this.getOrCreateProgressTag(questInstance).getInt(KEY_ACCUMULATED_TICKS).orElse(0) >= this.requiredTicks;
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
		return "block_stay:" + this.blockId;
	}

	/**
	 * 方块驻留事件喵~
	 *
	 * @param blockId 停留方块 ID 喵~
	 * @param ticks 本次累计 Tick 喵~
	 */
	public record BlockStayEvent(Identifier blockId, int ticks) {
	}
}
