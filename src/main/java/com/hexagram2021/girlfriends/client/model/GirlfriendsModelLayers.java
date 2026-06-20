package com.hexagram2021.girlfriends.client.model;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/**
 * 角色实体模型层注册喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsModelLayers {
	public static final ModelLayerLocation MOMO =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo"), "main");
	public static final ModelLayerLocation YUXI =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi"), "main");
	public static final ModelLayerLocation MEISHU =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu"), "main");
	public static final ModelLayerLocation WANYING =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying"), "main");
	public static final ModelLayerLocation YOURUO =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "youruo"), "main");

	private GirlfriendsModelLayers() {
	}
}
