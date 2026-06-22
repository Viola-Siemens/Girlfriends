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
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * 方块驻留目标处理器，支持列表、单对象、tag 三种写法喵~
 *
 * @param blocks 目标方块列表，满足任意一个方块驻留条件即可喵~
 *
 * @author liudongyu
 */
public record BlockStayObjectiveHandler(List<BlockStayRecord> blocks) implements QuestObjectiveHandler {
	private static final String KEY_BLOCKS = "blocks";
	private static final String KEY_BLOCK_ID = "block_id";
	private static final String KEY_IS_TAG = "is_tag";
	private static final String KEY_REQUIRED_TICKS = "required_ticks";
	private static final String KEY_ACCUMULATED_TICKS = "accumulated_ticks";

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = new ListTag();
		for(BlockStayRecord block : this.blocks) {
			CompoundTag itemTag = new CompoundTag();
			itemTag.putString(KEY_BLOCK_ID, block.blockId().toString());
			itemTag.putBoolean(KEY_IS_TAG, block instanceof BlockStayRecord.Tag);
			itemTag.putInt(KEY_REQUIRED_TICKS, block.requiredTicks());
			itemTag.putInt(KEY_ACCUMULATED_TICKS, 0);
			listTag.add(itemTag);
		}
		tag.put(KEY_BLOCKS, listTag);
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if(!(event instanceof BlockStayEvent blockStayEvent)) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_BLOCKS);
		for(int i = 0; i < listTag.size() && i < this.blocks.size(); i++) {
			CompoundTag itemTag = listTag.getCompound(i).orElseGet(CompoundTag::new);
			BlockStayRecord blockRecord = this.blocks.get(i);
			if(blockRecord.matches(blockStayEvent)) {
				int requiredTicks = itemTag.getIntOr(KEY_REQUIRED_TICKS, 1);
				int accumulatedTicks = itemTag.getIntOr(KEY_ACCUMULATED_TICKS, 0);
				itemTag.putInt(KEY_ACCUMULATED_TICKS, Math.min(requiredTicks, accumulatedTicks + Math.max(0, blockStayEvent.ticks())));
			}
		}
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_BLOCKS);
		return listTag.compoundStream().anyMatch(
				itemTag -> itemTag.getIntOr(KEY_ACCUMULATED_TICKS, 0) >= itemTag.getIntOr(KEY_REQUIRED_TICKS, 1)
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
		return "block_stay:" + this.blocks;
	}

	/**
	 * 方块驻留事件喵~
	 *
	 * @param block  停留方块喵~
	 * @param ticks  本次累计 Tick 数喵~
	 * @param holder 方块的 Holder，用于 tag 匹配；如果不可用则为 null 喵~
	 */
	public record BlockStayEvent(Block block, int ticks, @Nullable Holder<Block> holder) {
	}

	/**
	 * 方块驻留记录，支持单对象、列表、tag 三种写法喵~
	 */
	public sealed interface BlockStayRecord {
		/**
		 * 获取用于 NBT 存储的方块 ID 喵~
		 *
		 * @return 方块 ID 喵~
		 */
		Identifier blockId();

		/**
		 * 获取需要的驻留 Tick 数喵~
		 *
		 * @return 驻留 Tick 数喵~
		 */
		int requiredTicks();

		/**
		 * 判断事件是否匹配此记录喵~
		 *
		 * @param event 方块驻留事件喵~
		 * @return 是否匹配喵~
		 */
		boolean matches(BlockStayEvent event);

		/**
		 * 单方块记录喵~
		 *
		 * @param block         目标方块喵~
		 * @param requiredTicks 需要驻留的 Tick 数喵~
		 */
		record Single(Block block, int requiredTicks) implements BlockStayRecord {
			static final Codec<Single> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(Single::block),
					Codec.INT.optionalFieldOf("required_ticks", 20).forGetter(Single::requiredTicks)
			).apply(instance, Single::new));

			@Override
			public Identifier blockId() {
				return BuiltInRegistries.BLOCK.getKey(this.block);
			}

			@Override
			public boolean matches(BlockStayEvent event) {
				return this.block == event.block();
			}

			@Override
			public String toString() {
				return "BlockStayRecord.Single{block=" + BuiltInRegistries.BLOCK.getKey(this.block) + ", requiredTicks=" + this.requiredTicks + "}";
			}

			@Override
			public boolean equals(Object o) {
				if(this == o) {
					return true;
				}
				if(!(o instanceof Single(Block block1, int ticks))) {
					return false;
				}
				return this.block.equals(block1) && this.requiredTicks == ticks;
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.block, this.requiredTicks);
			}
		}

		/**
		 * 方块 Tag 记录喵~
		 *
		 * @param tag           方块 Tag 喵~
		 * @param requiredTicks 需要驻留的 Tick 数喵~
		 */
		record Tag(TagKey<Block> tag, int requiredTicks) implements BlockStayRecord {
			static final Codec<Tag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(Tag::tag),
					Codec.INT.optionalFieldOf("required_ticks", 20).forGetter(Tag::requiredTicks)
			).apply(instance, Tag::new));

			@Override
			public Identifier blockId() {
				return this.tag.location();
			}

			@Override
			public boolean matches(BlockStayEvent event) {
				return event.holder() != null && event.holder().is(this.tag);
			}

			@Override
			public String toString() {
				return "BlockStayRecord.Tag{tag=" + this.tag + ", requiredTicks=" + this.requiredTicks + "}";
			}

			@Override
			public boolean equals(Object o) {
				if(this == o) {
					return true;
				}
				if(!(o instanceof Tag(TagKey<Block> tag1, int ticks))) {
					return false;
				}
				return this.tag.equals(tag1) && this.requiredTicks == ticks;
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.tag, this.requiredTicks);
			}
		}

		/**
		 * 单条记录的编解码器，同时支持 Single 和 Tag 两种形状喵~
		 */
		Codec<BlockStayRecord> CODEC = Codec.either(Single.CODEC, Tag.CODEC).xmap(
				either -> either.map(s -> s, t -> t),
				rec -> {
					if(rec instanceof Single single) {
						return Either.left(single);
					}
					if(rec instanceof Tag tag) {
						return Either.right(tag);
					}
					throw new IllegalStateException("Unknown BlockStayRecord type: " + rec.getClass());
				}
		);

		/**
		 * 列表编解码器，支持列表、单对象、tag 三种写法喵~
		 */
		Codec<List<BlockStayRecord>> LIST_CODEC = CODEC.listOf()
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
}
