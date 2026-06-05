package com.hexagram2021.girlfriends.common.blessing;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 内置祝福类型注册表喵~
 *
 * @author liudongyu
 */
public final class BlessingTypes {
	public static final DeferredRegister<BlessingType> REGISTER = DeferredRegister.create(
			GirlfriendsRegistries.BLESSING_TYPE,
			GirlfriendsMod.MODID
	);

	public static final Identifier NATURE_PEACE_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "nature_peace");
	public static final Identifier SAILING_AND_FISHING_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "sailing_and_fishing");
	public static final Identifier MINING_EXTRA_DROP_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "mining_extra_drop");
	public static final Identifier MELEE_AND_DEFENSE_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "melee_and_defense");
	public static final Identifier ENDER_PEARL_CONSERVE_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "ender_pearl_conserve");

	public static final DeferredHolder<BlessingType, BlessingType> NATURE_PEACE = REGISTER.register(
			"nature_peace", () -> new BlessingType("girlfriends.blessing_type.nature_peace", "nature_peace")
	);
	public static final DeferredHolder<BlessingType, BlessingType> SAILING_AND_FISHING = REGISTER.register(
			"sailing_and_fishing", () -> new BlessingType("girlfriends.blessing_type.sailing_and_fishing", "sailing_and_fishing")
	);
	public static final DeferredHolder<BlessingType, BlessingType> MINING_EXTRA_DROP = REGISTER.register(
			"mining_extra_drop", () -> new BlessingType("girlfriends.blessing_type.mining_extra_drop", "mining_extra_drop")
	);
	public static final DeferredHolder<BlessingType, BlessingType> MELEE_AND_DEFENSE = REGISTER.register(
			"melee_and_defense", () -> new BlessingType("girlfriends.blessing_type.melee_and_defense", "melee_and_defense")
	);
	public static final DeferredHolder<BlessingType, BlessingType> ENDER_PEARL_CONSERVE = REGISTER.register(
			"ender_pearl_conserve", () -> new BlessingType("girlfriends.blessing_type.ender_pearl_conserve", "ender_pearl_conserve")
	);

	private BlessingTypes() {
	}
}
