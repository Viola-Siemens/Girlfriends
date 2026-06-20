package com.hexagram2021.girlfriends.common.quest;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.include.com.google.common.base.Objects;

import java.util.List;

/**
 * 物品交付目标处理器喵~
 *
 * @author liudongyu
 */
public class ItemDeliveryObjectiveHandler implements QuestObjectiveHandler {
	private static final String KEY_ITEMS = "items";
	private static final String KEY_ITEM_ID = "item";
	private static final String KEY_REQUIRED_COUNT = "required_count";
	private static final String KEY_DELIVERED_COUNT = "delivered_count";
	private final List<ItemDeliveryRecord> items;

	/**
	 * 创建物品交付目标处理器喵~
	 *
	 * @param items 目标物品，满足任意一个物品交付条件即可
	 */
	public ItemDeliveryObjectiveHandler(List<ItemDeliveryRecord> items) {
		this.items = items;
	}

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = new ListTag();
		for(ItemDeliveryRecord item: this.items) {
			CompoundTag itemTag = new CompoundTag();
			itemTag.putString(KEY_ITEM_ID, BuiltInRegistries.ITEM.getKey(item.item()).toString());
			itemTag.putInt(KEY_REQUIRED_COUNT, item.totalCount());
			itemTag.putInt(KEY_DELIVERED_COUNT, 0);
			listTag.add(itemTag);
		}
		tag.put(KEY_ITEMS, listTag);
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if(!(event instanceof ItemDeliveryEvent(Item item, int count))) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_ITEMS);
		listTag.compoundStream().forEach(itemTag -> {
			if(itemTag.getStringOr(KEY_ITEM_ID, "").equals(BuiltInRegistries.ITEM.getKey(item).toString())) {
				int deliveredCount = itemTag.getIntOr(KEY_DELIVERED_COUNT, 0);
				itemTag.putInt(KEY_DELIVERED_COUNT, Math.min(itemTag.getIntOr(KEY_REQUIRED_COUNT, 0), deliveredCount + Math.max(0, count)));
			}
		});
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_ITEMS);
		return listTag.compoundStream().anyMatch(
				itemTag -> itemTag.getIntOr(KEY_DELIVERED_COUNT, 0) >= itemTag.getIntOr(KEY_REQUIRED_COUNT, 0)
		);
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
		return "item_delivery:" + this.items;
	}

	/**
	 * 物品交付事件喵~
	 *
	 * @param item 交付物品喵~
	 * @param count 交付数量喵~
	 */
	public record ItemDeliveryEvent(Item item, int count) {
	}

	/**
	 * 物品交付记录喵~
	 *
	 * @param item       交付物品
	 * @param totalCount 交付数量
	 *
	 * @author liudongyu
	 */
	public record ItemDeliveryRecord(Item item, int totalCount) {
		public static final Codec<ItemDeliveryRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemDeliveryRecord::item),
				Codec.INT.optionalFieldOf("count", 1).forGetter(ItemDeliveryRecord::totalCount)
		).apply(instance, ItemDeliveryRecord::new));

		public static final Codec<List<ItemDeliveryRecord>> LIST_CODEC = CODEC.listOf().withAlternative(CODEC.xmap(
				List::of,
				List::getFirst
		)).withAlternative(TagItemDeliveryRecord.CODEC.xmap(
				tagRecord -> {
					ImmutableList.Builder<ItemDeliveryRecord> builder = ImmutableList.builder();
					BuiltInRegistries.ITEM.getTagOrEmpty(tagRecord.tag()).forEach(
							itemHolder -> builder.add(new ItemDeliveryRecord(itemHolder.value(), tagRecord.count()))
					);
					return builder.build();
				},
				_ -> {
					throw new UnsupportedOperationException("This should never be called.");
				}
		));

		@Override
		public String toString() {
			return "ItemDeliveryRecord{item=" + BuiltInRegistries.ITEM.getKey(this.item) + ", totalCount=" + this.totalCount + "}";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ItemDeliveryRecord(Item item1, int count))) {
				return false;
			}
			return this.item.equals(item1) && this.totalCount == count;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.item, this.totalCount);
		}

		private record TagItemDeliveryRecord(TagKey<Item> tag, int count) {
			private static final Codec<TagItemDeliveryRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					TagKey.codec(BuiltInRegistries.ITEM.key()).fieldOf("tag").forGetter(TagItemDeliveryRecord::tag),
					Codec.INT.optionalFieldOf("count", 1).forGetter(TagItemDeliveryRecord::count)
			).apply(instance, TagItemDeliveryRecord::new));
		}
	}
}
