package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
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
			DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, GirlfriendsMod.MODID);

	/** 沫沫实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<MomoEntity>> MOMO =
			REGISTER.register("momo", () -> EntityType.Builder
					.of(MomoEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(48)
					.updateInterval(3)
					.build(ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo"))));

	private GirlfriendsEntities() {
	}
}
