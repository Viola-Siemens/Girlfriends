package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsItems {
	public static final DeferredRegister<Item> REGISTER =
			DeferredRegister.create(Registries.ITEM, GirlfriendsMod.MODID);

	private GirlfriendsItems() {
	}
}
