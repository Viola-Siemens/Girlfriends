package com.hexagram2021.girlfriends.common.creativemodetab;

import com.hexagram2021.girlfriends.common.item.GirlfriendsItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.hexagram2021.girlfriends.GirlfriendsMod.MODID;

/**
 * 创造模式物品栏注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsCreativeModeTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTER =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
			REGISTER.register(
					"water_level",
					() -> CreativeModeTab.builder()
							.icon(() -> new ItemStack(GirlfriendsItems.BOUQUET.get()))
							.displayItems(GirlfriendsItems.ALL_ITEMS)
							.withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
							.build()
			);

	private GirlfriendsCreativeModeTabs() {
	}
}
