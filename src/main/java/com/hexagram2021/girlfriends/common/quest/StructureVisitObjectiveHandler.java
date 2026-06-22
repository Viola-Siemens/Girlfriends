package com.hexagram2021.girlfriends.common.quest;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 结构到访目标处理器，支持列表、单对象、tag 三种写法喵~
 *
 * @param structures 目标结构列表，满足任意一个结构到访条件即可喵~
 *
 * @author liudongyu
 */
public record StructureVisitObjectiveHandler(List<StructureVisitRecord> structures) implements QuestObjectiveHandler {
	private static final String KEY_STRUCTURES = "structures";
	private static final String KEY_STRUCTURE_ID = "structure_id";
	private static final String KEY_IS_TAG = "is_tag";
	private static final String KEY_VISITED = "visited";

	@Override
	public boolean canAccept(QuestInstance questInstance) {
		return true;
	}

	@Override
	public void onAccept(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = new ListTag();
		for (StructureVisitRecord structure : this.structures) {
			CompoundTag itemTag = new CompoundTag();
			itemTag.putString(KEY_STRUCTURE_ID, structure.structureId().toString());
			itemTag.putBoolean(KEY_IS_TAG, structure instanceof StructureVisitRecord.Tag);
			itemTag.putBoolean(KEY_VISITED, false);
			listTag.add(itemTag);
		}
		tag.put(KEY_STRUCTURES, listTag);
	}

	@Override
	public void onEvent(QuestInstance questInstance, Object event) {
		if (!(event instanceof StructureVisitEvent structureVisitEvent)) {
			return;
		}
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_STRUCTURES);
		for (int i = 0; i < listTag.size() && i < this.structures.size(); i++) {
			CompoundTag itemTag = listTag.getCompound(i).orElseGet(CompoundTag::new);
			StructureVisitRecord structureRecord = this.structures.get(i);
			if (structureRecord.matches(structureVisitEvent)) {
				itemTag.putBoolean(KEY_VISITED, true);
			}
		}
	}

	@Override
	public boolean isCompleted(QuestInstance questInstance) {
		CompoundTag tag = this.getOrCreateProgressTag(questInstance);
		ListTag listTag = tag.getListOrEmpty(KEY_STRUCTURES);
		return listTag.compoundStream().anyMatch(
				itemTag -> itemTag.getBoolean(KEY_VISITED).orElse(false)
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
		return "structure_visit:" + this.structures;
	}

	/**
	 * 结构到访事件喵~
	 *
	 * @param structureId 到访结构的注册表 ID 喵~
	 * @param holder      结构的 Holder，用于 tag 匹配；如果不可用则为 null 喵~
	 */
	public record StructureVisitEvent(Identifier structureId, @Nullable Holder<Structure> holder) {
	}

	/**
	 * 结构到访记录，支持单对象、列表、tag 三种写法喵~
	 */
	public sealed

	interface StructureVisitRecord {
		/**
		 * 获取用于 NBT 存储的结构 ID 喵~
		 *
		 * @return 结构 ID 喵~
		 */
		Identifier structureId();

		/**
		 * 判断事件是否匹配此记录喵~
		 *
		 * @param event 结构到访事件喵~
		 * @return 是否匹配喵~
		 */
		boolean matches(StructureVisitEvent event);

		/**
		 * 单结构记录喵~
		 *
		 * @param structureId 结构 ID 喵~
		 */
		record Single(Identifier structureId) implements StructureVisitRecord {
			static final Codec<Single> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Identifier.CODEC.fieldOf("structure").forGetter(Single::structureId)
			).apply(instance, Single::new));

			@Override
			public boolean matches(StructureVisitEvent event) {
				return this.structureId.equals(event.structureId());
			}
		}

		/**
		 * 结构 Tag 记录喵~
		 *
		 * @param tag 结构 Tag 喵~
		 */
		record Tag(TagKey<Structure> tag) implements StructureVisitRecord {
			static final Codec<Tag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					TagKey.codec(Registries.STRUCTURE).fieldOf("tag").forGetter(Tag::tag)
			).apply(instance, Tag::new));

			@Override
			public Identifier structureId() {
				return this.tag.location();
			}

			@Override
			public boolean matches(StructureVisitEvent event) {
				return event.holder() != null && event.holder().is(this.tag);
			}

			@Override
			public String toString() {
				return "StructureVisitRecord.Tag{tag=" + this.tag + "}";
			}
		}

		/**
		 * 单条记录的编解码器，同时支持 Single 和 Tag 两种形状喵~
		 */
		Codec<StructureVisitRecord> CODEC = Codec.either(Single.CODEC, Tag.CODEC).xmap(
				either -> either.map(s -> s, t -> t),
				rec -> {
					if (rec instanceof Single single) {
						return Either.left(single);
					}
					if (rec instanceof Tag tag) {
						return Either.right(tag);
					}
					throw new IllegalStateException("Unknown StructureVisitRecord type: " + rec.getClass());
				}
		);

		/**
		 * 列表编解码器，支持列表、单对象、tag 三种写法喵~
		 */
		Codec<List<StructureVisitRecord>> LIST_CODEC = CODEC.listOf()
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
		return "StructureVisitObjectiveHandler{structures=" + this.structures + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StructureVisitObjectiveHandler(List<StructureVisitRecord> structures1))) {
			return false;
		}
		return this.structures.equals(structures1);
	}

}
