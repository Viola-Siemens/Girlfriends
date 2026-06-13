package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 物品标签
 *
 * @author liudongyu
 */
public final class GirlfriendsItemTags {
	public static final TagKey<Item> MOMO_PICKS_UP = TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo_picks_up")
	);

	private GirlfriendsItemTags() {
	}
}
