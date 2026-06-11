package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * 实体标签
 *
 * @author liudongyu
 */
public final class GirlfriendEntityTags {
	public static final TagKey<EntityType<?>> GIRLFRIENDS = TagKey.create(
			Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriends")
	);

	private GirlfriendEntityTags() {
	}
}
