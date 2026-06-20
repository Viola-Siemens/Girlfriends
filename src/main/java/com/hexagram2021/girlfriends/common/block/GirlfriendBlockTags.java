package com.hexagram2021.girlfriends.common.block;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 方块标签
 *
 * @author liudongyu
 */
public final class GirlfriendBlockTags {
	public static final TagKey<Block> GRASS_TO_WEED = TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "grass_to_weed")
	);

	private GirlfriendBlockTags() {
	}
}
