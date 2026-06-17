package com.hexagram2021.girlfriends.common.components;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.hexagram2021.girlfriends.GirlfriendsMod.MODID;

/**
 * 物品数据组件
 *
 * @author liudongyu
 */
public final class GirlfriendsDataComponentTypes {
	public static final DeferredRegister<DataComponentType<?>> REGISTER =
			DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATER_LEVEL =
			REGISTER.register(
					"water_level",
					() -> DataComponentType.<Integer>builder()
							.persistent(Codec.INT)
							.networkSynchronized(ByteBufCodecs.INT)
							.build()
			);

	private GirlfriendsDataComponentTypes() {
	}
}
