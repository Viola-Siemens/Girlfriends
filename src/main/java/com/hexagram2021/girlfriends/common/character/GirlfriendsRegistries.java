package com.hexagram2021.girlfriends.common.character;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Girlfriends 自定义注册表定义喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsRegistries {
	public static final ResourceKey<Registry<GirlfriendType>> GIRLFRIEND_TYPE = ResourceKey.createRegistryKey(
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriend_type")
	);

	public static final Registry<GirlfriendType> GIRLFRIEND_TYPE_REGISTRY = new RegistryBuilder<>(GIRLFRIEND_TYPE)
			.sync(true)
			.create();

	private GirlfriendsRegistries() {
	}
}
