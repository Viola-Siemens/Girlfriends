package com.hexagram2021.girlfriends.common.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 物品交付目标处理器喵~
 *
 * @author liudongyu
 */
public class ItemDeliveryObjectiveHandler implements QuestObjectiveHandler {
	private static final String KEY_ITEM_ID = "item_id";
	private static final String KEY_REQUIRED_COUNT = "required_count";
	private static final String KEY_DELIVERED_COUNT = "delivered_count";
	private final Identifier itemId;
	private final int requiredCount;

	/**
	 * 创建物品交付目标处理器喵~
	 *
	 * @param itemId 目标物品 ID 喵~
	 * @param requiredCount 需要交付的数量喵~
	 */
	public ItemDeliveryObjectiveHandler(Identifier itemId, int requiredCount) {
		this.itemId = itemId;
		this.requiredCount = Math.max(1, requiredCount);
	}

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		tag.putString(KEY_ITEM_ID, this.itemId.toString());
		tag.putInt(KEY_REQUIRED_COUNT, this.requiredCount);
		tag.putInt(KEY_DELIVERED_COUNT, tag.getInt(KEY_DELIVERED_COUNT).orElse(0));
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if(!(event instanceof ItemDeliveryEvent itemDeliveryEvent)) {
			return;
		}
		if(!this.itemId.equals(itemDeliveryEvent.itemId())) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		int deliveredCount = tag.getInt(KEY_DELIVERED_COUNT).orElse(0);
		tag.putInt(KEY_DELIVERED_COUNT, Math.min(this.requiredCount, deliveredCount + Math.max(0, itemDeliveryEvent.count())));
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		return tag.getInt(KEY_DELIVERED_COUNT).orElse(0) >= this.requiredCount;
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
		return "item_delivery:" + this.itemId;
	}

	/**
	 * 物品交付事件喵~
	 *
	 * @param itemId 交付物品 ID 喵~
	 * @param count 交付数量喵~
	 */
	public record ItemDeliveryEvent(Identifier itemId, int count) {
	}
}
