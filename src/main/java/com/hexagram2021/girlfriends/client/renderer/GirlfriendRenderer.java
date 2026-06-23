package com.hexagram2021.girlfriends.client.renderer;

import com.google.common.collect.Maps;
import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 角色实体渲染器喵~
 * <p>
 * 统一使用原版 HumanoidModel（参考 PlayerModel），
 * 通过纹理路径区分五位角色。
 * 在满足条件时，角色头顶渲染交互图标（亲密确认 / 固定委托 / 随机委托）喵~
 *
 * @author liudongyu
 */
public class GirlfriendRenderer extends HumanoidMobRenderer<GirlfriendEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier ICON_QUESTION =
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "textures/gui/sprites/icon/question.png");
	private static final Identifier ICON_STORY =
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "textures/gui/sprites/icon/story.png");
	private static final Identifier ICON_RANDOM =
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "textures/gui/sprites/icon/random.png");

	private static final int FULL_BRIGHT = 0xffffff;

	private final Identifier textureLocation;

	/**
	 * 帧内图标缓存喵~
	 * <p>
	 * {@link #extractRenderState} 计算图标类型并存入，
	 * {@link #submitNameDisplay} 读取并清除。
	 * 使用 IdentityHashMap 因为 RenderState 对象每帧重新创建，
	 * 且 extractRenderState → submitNameDisplay 在同一渲染帧内串行调用喵~
	 */
	private final Map<HumanoidRenderState, IconType> iconCache = Maps.newIdentityHashMap();

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

	@Override
	public void extractRenderState(GirlfriendEntity entity, HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		// 从客户端缓存读取交互摘要，计算图标类型喵~
		InteractionSummary summary = ClientInteractionStore.getSummary(entity.getGirlfriendTypeId());
		IconType icon = computeIconType(summary);
		this.iconCache.put(state, icon);
	}

	@Override
	protected void submitNameDisplay(HumanoidRenderState state, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		super.submitNameDisplay(state, poseStack, submitNodeCollector, camera);

		IconType icon = this.iconCache.remove(state);
		if (icon == null) {
			return;
		}

		Identifier texture = icon.getTexture();
		RenderType renderType = RenderTypes.itemCutout(texture);

		poseStack.pushPose();
		// 定位到名称标签附着点上方喵~
		if (state.nameTagAttachment != null) {
			poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y, state.nameTagAttachment.z);
		}
		// 名称标签上方偏移喵~
		poseStack.translate(0.0F, 2.5F, 0.0F);
		// 面向摄像机喵~
		poseStack.mulPose(camera.orientation);
		// 缩放到名称标签坐标空间喵~
		float scale = 0.05F;
		poseStack.scale(scale, scale, scale);

		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
			Matrix4f matrix = pose.pose();
			float halfSize = 5.0F;
			int white = -1;

			Vector3f v0 = matrix.transformPosition(new Vector3f(-halfSize, -halfSize, 0.0F), new Vector3f());
			Vector3f v1 = matrix.transformPosition(new Vector3f(halfSize, -halfSize, 0.0F), new Vector3f());
			Vector3f v2 = matrix.transformPosition(new Vector3f(halfSize, halfSize, 0.0F), new Vector3f());
			Vector3f v3 = matrix.transformPosition(new Vector3f(-halfSize, halfSize, 0.0F), new Vector3f());

			buffer.addVertex(v0.x, v0.y, v0.z, white, 0.0F, 1.0F, 0, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
			buffer.addVertex(v1.x, v1.y, v1.z, white, 1.0F, 1.0F, 0, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
			buffer.addVertex(v2.x, v2.y, v2.z, white, 1.0F, 0.0F, 0, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
			buffer.addVertex(v3.x, v3.y, v3.z, white, 0.0F, 0.0F, 0, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
		});

		poseStack.popPose();
	}

	/**
	 * 根据交互摘要计算应显示的图标类型喵~
	 * <p>
	 * 优先级：亲密确认 > 固定委托 > 随机委托喵~
	 *
	 * @param summary 交互摘要，可能为 null 喵~
	 * @return 图标类型，null 表示不显示喵~
	 */
	@Nullable
	private static IconType computeIconType(@Nullable InteractionSummary summary) {
		if (summary == null) {
			return null;
		}
		// 优先级：亲密确认 > 固定委托 > 随机委托喵~
		if (summary.needsIntimacyConfirmation()) {
			return IconType.QUESTION;
		}
		if (summary.canAcceptQuest() && summary.currentQuest() != null) {
			return switch (summary.currentQuest().questType()) {
				case FIXED -> IconType.STORY;
				case RANDOM -> IconType.RANDOM;
			};
		}
		return null;
	}

	/**
	 * 图标类型枚举喵~
	 */
	private enum IconType {
		QUESTION(ICON_QUESTION),
		STORY(ICON_STORY),
		RANDOM(ICON_RANDOM);

		private final Identifier texture;

		IconType(Identifier texture) {
			this.texture = texture;
		}

		Identifier getTexture() {
			return this.texture;
		}
	}
}
