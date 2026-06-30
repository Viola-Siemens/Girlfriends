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
	public static final TagKey<Item> YUXI_PICKS_UP = TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi_picks_up")
	);
	public static final TagKey<Item> MEISHU_PICKS_UP = TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu_picks_up")
	);
	public static final TagKey<Item> WANYING_PICKS_UP = TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying_picks_up")
	);
	public static final TagKey<Item> YOURUO_PICKS_UP = TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo_picks_up")
	);

	private GirlfriendsItemTags() {
	}
}
