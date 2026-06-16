package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsItems {
	public static final DeferredRegister<Item> REGISTER =
			DeferredRegister.create(Registries.ITEM, GirlfriendsMod.MODID);

	public static final DeferredHolder<Item, Item> BOUQUET =
			REGISTER.register("bouquet", () -> new Item(new Item.Properties().stacksTo(1)));

	public static final DeferredHolder<Item, WateringCanItem> WATERING_CAN =
			REGISTER.register("watering_can", () -> new WateringCanItem(new Item.Properties().stacksTo(1)));

	private GirlfriendsItems() {
	}
}
