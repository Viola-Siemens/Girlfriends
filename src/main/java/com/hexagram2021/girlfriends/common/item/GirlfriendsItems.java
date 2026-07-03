package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.components.GirlfriendsDataComponentTypes;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * 模组物品注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsItems {
	public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(GirlfriendsMod.MODID);

	public static final DeferredItem<Item> BOUQUET =
			REGISTER.registerSimpleItem("bouquet", () -> new Item.Properties().stacksTo(1));

	public static final DeferredItem<Item> GRAY_RIBBON =
			REGISTER.registerSimpleItem("gray_ribbon", () -> new Item.Properties().stacksTo(16));

	public static final DeferredItem<Item> LIGHT_GRAY_RIBBON =
			REGISTER.registerSimpleItem("light_gray_ribbon", () -> new Item.Properties().stacksTo(16));

	public static final DeferredItem<Item> PINK_RIBBON =
			REGISTER.registerSimpleItem("pink_ribbon", () -> new Item.Properties().stacksTo(16));

	public static final DeferredItem<Item> WHITE_RIBBON =
			REGISTER.registerSimpleItem("white_ribbon", () -> new Item.Properties().stacksTo(16));

	public static final DeferredItem<WateringCanItem> WATERING_CAN =
			REGISTER.registerItem("watering_can", WateringCanItem::new, () -> new Item.Properties().stacksTo(1).component(GirlfriendsDataComponentTypes.WATER_LEVEL, 0));

	public static final List<DeferredItem<?>> ALL_ITEMS = List.of(
			BOUQUET,
			WATERING_CAN,
			GRAY_RIBBON,
			LIGHT_GRAY_RIBBON,
			PINK_RIBBON,
			WHITE_RIBBON
	);

	private GirlfriendsItems() {
	}
}
