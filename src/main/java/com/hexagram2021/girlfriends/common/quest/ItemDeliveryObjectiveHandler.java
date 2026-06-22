package com.hexagram2021.girlfriends.common.quest;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * 物品交付目标处理器，支持列表、单对象、tag 三种写法喵~
 *
 * @param items 目标物品列表，满足任意一个物品交付条件即可喵~
 *
 * @author liudongyu
 */
public record ItemDeliveryObjectiveHandler(List<ItemDeliveryRecord> items) implements QuestObjectiveHandler {
	private static final String KEY_ITEMS = "items";
	private static final String KEY_ITEM_ID = "item_id";
	private static final String KEY_IS_TAG = "is_tag";
	private static final String KEY_REQUIRED_COUNT = "required_count";
	private static final String KEY_DELIVERED_COUNT = "delivered_count";

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = new ListTag();
		for (ItemDeliveryRecord item : this.items) {
			CompoundTag itemTag = new CompoundTag();
			itemTag.putString(KEY_ITEM_ID, item.itemId().toString());
			itemTag.putBoolean(KEY_IS_TAG, item instanceof ItemDeliveryRecord.Tag);
			itemTag.putInt(KEY_REQUIRED_COUNT, item.requiredCount());
			itemTag.putInt(KEY_DELIVERED_COUNT, 0);
			listTag.add(itemTag);
		}
		tag.put(KEY_ITEMS, listTag);
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if (!(event instanceof ItemDeliveryEvent itemDeliveryEvent)) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_ITEMS);
		for (int i = 0; i < listTag.size() && i < this.items.size(); i++) {
			CompoundTag itemTag = listTag.getCompound(i).orElseGet(CompoundTag::new);
			ItemDeliveryRecord itemRecord = this.items.get(i);
			if (itemRecord.matches(itemDeliveryEvent)) {
				int deliveredCount = itemTag.getIntOr(KEY_DELIVERED_COUNT, 0);
				itemTag.putInt(KEY_DELIVERED_COUNT, Math.min(itemTag.getIntOr(KEY_REQUIRED_COUNT, 0), deliveredCount + Math.max(0, itemDeliveryEvent.count())));
			}
		}
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
	 * @param item   交付物品喵~
	 * @param count  交付数量喵~
	 * @param holder 物品的 Holder，用于 tag 匹配；如果不可用则为 null 喵~
	 */
	public record ItemDeliveryEvent(Item item, int count, @Nullable Holder<Item> holder) {
	}

	/**
	 * 物品交付记录，支持单对象、列表、tag 三种写法喵~
	 */
	public sealed

	interface ItemDeliveryRecord {
		/**
		 * 获取用于 NBT 存储的物品 ID 喵~
		 *
		 * @return 物品 ID 喵~
		 */
		Identifier itemId();

		/**
		 * 获取需要的交付数量喵~
		 *
		 * @return 交付数量喵~
		 */
		int requiredCount();

		/**
		 * 判断事件是否匹配此记录喵~
		 *
		 * @param event 物品交付事件喵~
		 * @return 是否匹配喵~
		 */
		boolean matches(ItemDeliveryEvent event);

		/**
		 * 单物品记录喵~
		 *
		 * @param item       交付物品喵~
		 * @param totalCount 交付数量喵~
		 */
		record Single(Item item, int totalCount) implements ItemDeliveryRecord {
			static final Codec<Single> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Single::item),
					Codec.INT.optionalFieldOf("count", 1).forGetter(Single::totalCount)
			).apply(instance, Single::new));

			@Override
			public Identifier itemId() {
				return BuiltInRegistries.ITEM.getKey(this.item);
			}

			@Override
			public int requiredCount() {
				return this.totalCount;
			}

			@Override
			public boolean matches(ItemDeliveryEvent event) {
				return this.item == event.item();
			}

			@Override
			public String toString() {
				return "ItemDeliveryRecord.Single{item=" + BuiltInRegistries.ITEM.getKey(this.item) + ", totalCount=" + this.totalCount + "}";
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (!(o instanceof Single(Item item1, int count))) {
					return false;
				}
				return this.item.equals(item1) && this.totalCount == count;
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.item, this.totalCount);
			}
		}

		/**
		 * 物品 Tag 记录喵~
		 *
		 * @param tag   物品 Tag 喵~
		 * @param count 交付数量喵~
		 */
		record Tag(TagKey<Item> tag, int count) implements ItemDeliveryRecord {
			static final Codec<Tag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(Tag::tag),
					Codec.INT.optionalFieldOf("count", 1).forGetter(Tag::count)
			).apply(instance, Tag::new));

			@Override
			public Identifier itemId() {
				return this.tag.location();
			}

			@Override
			public int requiredCount() {
				return this.count;
			}

			@Override
			public boolean matches(ItemDeliveryEvent event) {
				return event.holder() != null && event.holder().is(this.tag);
			}

			@Override
			public String toString() {
				return "ItemDeliveryRecord.Tag{tag=" + this.tag + ", count=" + this.count + "}";
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (!(o instanceof Tag(TagKey<Item> tag1, int count1))) {
					return false;
				}
				return this.tag.equals(tag1) && this.count == count1;
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.tag, this.count);
			}
		}

		/**
		 * 单条记录的编解码器，同时支持 Single 和 Tag 两种形状喵~
		 */
		Codec<ItemDeliveryRecord> CODEC = Codec.either(Single.CODEC, Tag.CODEC).xmap(
				either -> either.map(s -> s, t -> t),
				rec -> {
					if (rec instanceof Single single) {
						return Either.left(single);
					}
					if (rec instanceof Tag tag) {
						return Either.right(tag);
					}
					throw new IllegalStateException("Unknown ItemDeliveryRecord type: " + rec.getClass());
				}
		);

		/**
		 * 列表编解码器，支持列表、单对象、tag 三种写法喵~
		 */
		Codec<List<ItemDeliveryRecord>> LIST_CODEC = CODEC.listOf()
				.withAlternative(Single.CODEC.xmap(
						List::of,
						_ -> {
							throw new UnsupportedOperationException("This should never be called.");
						}
				))
				.withAlternative(Tag.CODEC.xmap(
						List::of,
						_ -> {
							throw new UnsupportedOperationException("This should never be called.");
						}
				));
	}

	@Override
	public String toString() {
		return "ItemDeliveryObjectiveHandler{items=" + this.items + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ItemDeliveryObjectiveHandler(List<ItemDeliveryRecord> items1))) {
			return false;
		}
		return this.items.equals(items1);
	}

}
