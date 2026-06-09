package com.hexagram2021.girlfriends.client.renderer;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * 角色实体渲染器喵~
 * <p>
 * 统一使用原版 HumanoidModel（参考 PlayerModel），
 * 通过纹理路径区分五位角色喵~
 *
 * @author liudongyu
 */
public class GirlfriendRenderer extends HumanoidMobRenderer<GirlfriendEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier textureLocation;

	/**
	 * 创建渲染器喵~
	 *
	 * @param context            渲染上下文喵~
	 * @param modelLayerLocation 模型层路径喵~
	 * @param characterId        角色 ID（用于纹理路径）喵~
	 */
	public GirlfriendRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayerLocation, String characterId) {
		super(
				context,
				new HumanoidModel<>(context.bakeLayer(modelLayerLocation)),
				0.5f
		);
		this.textureLocation = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID,
				"textures/entity/" + characterId + "/" + characterId + ".png");
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return this.textureLocation;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}
}
