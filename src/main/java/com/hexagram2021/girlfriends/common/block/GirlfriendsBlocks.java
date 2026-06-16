package com.hexagram2021.girlfriends.common.block;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表
 *
 * @author liudongyu
 */
public final class GirlfriendsBlocks {
	public static final DeferredRegister<Block> REGISTER =
			DeferredRegister.create(Registries.BLOCK, GirlfriendsMod.MODID);

	private GirlfriendsBlocks() {
	}
}
