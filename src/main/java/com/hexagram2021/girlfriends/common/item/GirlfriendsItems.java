package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsItems {
	public static final DeferredRegister.Items REGISTER =
			DeferredRegister.createItems(GirlfriendsMod.MODID);

	public static final DeferredItem<Item> BOUQUET =
			REGISTER.registerSimpleItem("bouquet", () -> new Item.Properties().stacksTo(1));

	public static final DeferredItem<WateringCanItem> WATERING_CAN =
			REGISTER.registerItem("watering_can", WateringCanItem::new, () -> new Item.Properties().stacksTo(1));

	private GirlfriendsItems() {
	}
}
