package com.hexagram2021.girlfriends.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;

/**
 * 角色模型
 *
 * @author liudongyu
 */
public class GirlfriendModel extends PlayerModel {
	/**
	 * 角色模型构造函数
	 * @param root 模型根节点
	 */
	public GirlfriendModel(ModelPart root) {
		super(root, true);
	}

	/**
	 * 创建角色模型层
	 * @return 角色模型层
	 */
	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64);
	}

	/**
	 * 创建小个子角色模型层
	 * @return 角色模型层
	 */
	public static LayerDefinition createShortBodyLayer() {
		return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(-0.05F, -0.1F, -0.05F), true), 64, 64);
	}
}
