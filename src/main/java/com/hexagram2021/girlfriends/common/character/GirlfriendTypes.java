package com.hexagram2021.girlfriends.common.character;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * 内置角色类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendTypes {
	public static final DeferredRegister<GirlfriendType> REGISTER = DeferredRegister.create(
			GirlfriendsRegistries.GIRLFRIEND_TYPE,
			GirlfriendsMod.MODID
	);

	public static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	public static final Identifier YUXI_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");
	public static final Identifier MEISHU_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu");
	public static final Identifier WANYING_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying");
	public static final Identifier YOURUO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo");

	public static final DeferredHolder<GirlfriendType, GirlfriendType> MOMO = REGISTER.register("momo", () -> new GirlfriendType(
			"girlfriends.girlfriend_type.momo",
			new DimensionPolicy(Set.of(Level.OVERWORLD.identifier())),
			Identifier.withDefaultNamespace("honeycomb"),
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo_shelter")
	));
	public static final DeferredHolder<GirlfriendType, GirlfriendType> YUXI = REGISTER.register("yuxi", () -> new GirlfriendType(
			"girlfriends.girlfriend_type.yuxi",
			new DimensionPolicy(Set.of(Level.OVERWORLD.identifier())),
			Identifier.withDefaultNamespace("nautilus_shell"),
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi_shelter")
	));
	public static final DeferredHolder<GirlfriendType, GirlfriendType> MEISHU = REGISTER.register("meishu", () -> new GirlfriendType(
			"girlfriends.girlfriend_type.meishu",
			new DimensionPolicy(Set.of(Level.OVERWORLD.identifier())),
			Identifier.withDefaultNamespace("iron_ingot"),
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu_shelter")
	));
	public static final DeferredHolder<GirlfriendType, GirlfriendType> WANYING = REGISTER.register("wanying", () -> new GirlfriendType(
			"girlfriends.girlfriend_type.wanying",
			new DimensionPolicy(Set.of(Level.NETHER.identifier())),
			Identifier.withDefaultNamespace("blaze_rod"),
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying_shelter")
	));
	public static final DeferredHolder<GirlfriendType, GirlfriendType> YOURUO = REGISTER.register("youruo", () -> new GirlfriendType(
			"girlfriends.girlfriend_type.youruo",
			new DimensionPolicy(Set.of(Level.END.identifier())),
			Identifier.withDefaultNamespace("ender_pearl"),
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo_shelter")
	));

	private GirlfriendTypes() {
	}
}
