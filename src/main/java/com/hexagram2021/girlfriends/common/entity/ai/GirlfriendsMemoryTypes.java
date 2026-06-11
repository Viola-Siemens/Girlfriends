package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * 角色 AI 记忆模块类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsMemoryTypes {
	public static final DeferredRegister<MemoryModuleType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, GirlfriendsMod.MODID);

	/** 庇护所位置 (GlobalPos) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> SHELTER_POINT =
			REGISTER.register("shelter_point", () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));

	/** 附近花朵位置列表 (List<BlockPos>) — 沫沫专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEARBY_FLOWERS =
			REGISTER.register("nearby_flowers", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	/** 附近蜂箱位置 (BlockPos) — 沫沫专属 */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEAREST_BEEHIVE =
			REGISTER.register("nearest_beehive", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	/** 附近水域位置 (GlobalPos) — 渔溪专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEAREST_WATER =
			REGISTER.register("nearest_water", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	/** 附近可挖掘矿石 (List<BlockPos>) — 梅疏专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEARBY_ORES =
			REGISTER.register("nearby_ores", () -> new MemoryModuleType<>(Optional.empty()));

	/** 末影珍珠瞬移目标 (BlockPos) — 幽若专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> PEARL_TARGET =
			REGISTER.register("pearl_target", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	private GirlfriendsMemoryTypes() {
	}
}
