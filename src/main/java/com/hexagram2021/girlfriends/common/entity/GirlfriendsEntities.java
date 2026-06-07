package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
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

	private GirlfriendsEntities() {
	}
}
