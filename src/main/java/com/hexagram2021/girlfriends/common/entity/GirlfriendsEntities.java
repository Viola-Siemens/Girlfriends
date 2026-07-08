package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.GirlfriendFishingHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 角色实体类型注册表喵~
 * <p>
 * 各角色实体在对应 Story 中添加注册喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsEntities {
	public static final DeferredRegister<EntityType<?>> REGISTER =
			DeferredRegister.create(Registries.ENTITY_TYPE, GirlfriendsMod.MODID);

	/** 沫沫实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<MomoEntity>> MOMO =
			REGISTER.register("momo", () -> EntityType.Builder
					.of(MomoEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo"))));

	/** 渔溪实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<YuxiEntity>> YUXI =
			REGISTER.register("yuxi", () -> EntityType.Builder
					.of(YuxiEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi"))));

	/** 梅疏实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<MeishuEntity>> MEISHU =
			REGISTER.register("meishu", () -> EntityType.Builder
					.of(MeishuEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu"))));

	/** 渔溪钓竿浮标实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<GirlfriendFishingHook>> GIRLFRIEND_FISHING_HOOK =
			REGISTER.register("girlfriend_fishing_hook", () -> EntityType.Builder
					.of(GirlfriendFishingHook::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.build(ResourceKey.create(Registries.ENTITY_TYPE,
							Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriend_fishing_hook"))));

	/** 晚萤实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<WanyingEntity>> WANYING =
			REGISTER.register("wanying", () -> EntityType.Builder
					.of(WanyingEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying"))));

	/** 幽若实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<YouruoEntity>> YOURUO =
			REGISTER.register("youruo", () -> EntityType.Builder
					.of(YouruoEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo"))));

	private GirlfriendsEntities() {
	}
}
