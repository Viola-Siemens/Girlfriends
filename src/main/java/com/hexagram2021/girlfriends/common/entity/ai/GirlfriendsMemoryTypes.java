package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

/**
 * 角色 AI 记忆模块类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsMemoryTypes {
	public static final DeferredRegister<MemoryModuleType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, GirlfriendsMod.MODID);

	/** 当前日程时段 (Integer) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> CURRENT_SCHEDULE =
			REGISTER.register("current_schedule", () -> new MemoryModuleType<>(Optional.empty()));

	/** 庇护所位置 (GlobalPos) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> MEETING_POINT =
			REGISTER.register("meeting_point", () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));

	/** 移动目标 (BlockPos) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> WALK_TARGET =
			REGISTER.register("walk_target", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	/** 最近可见玩家 (Player) 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Player>> NEAREST_VISIBLE_PLAYER =
			REGISTER.register("nearest_visible_player", () -> new MemoryModuleType<>(Optional.empty()));

	/** 附近花朵位置列表 (List<BlockPos>) — 沫沫专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<BlockPos>>> NEARBY_FLOWERS =
			REGISTER.register("nearby_flowers", () -> new MemoryModuleType<>(Optional.empty()));

	/** 附近水域位置 (GlobalPos) — 渔溪专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> NEAREST_WATER =
			REGISTER.register("nearest_water", () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));

	/** 附近可挖掘矿石 (List<BlockPos>) — 梅疏专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<BlockPos>>> NEARBY_ORES =
			REGISTER.register("nearby_ores", () -> new MemoryModuleType<>(Optional.empty()));

	/** 附近敌对生物 (LivingEntity) — 晚萤专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<LivingEntity>> NEAREST_HOSTILE =
			REGISTER.register("nearest_hostile", () -> new MemoryModuleType<>(Optional.empty()));

	/** 末影珍珠瞬移目标 (BlockPos) — 幽若专属 喵~ */
	public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> PEARL_TARGET =
			REGISTER.register("pearl_target", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

	private GirlfriendsMemoryTypes() {
	}
}
