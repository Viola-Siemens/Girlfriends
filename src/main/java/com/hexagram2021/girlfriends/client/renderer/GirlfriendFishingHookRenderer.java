package com.hexagram2021.girlfriends.client.renderer;

import com.hexagram2021.girlfriends.common.entity.GirlfriendFishingHook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 自定义浮标渲染器 — 参照原版 FishingHookRenderer 实现喵~
 *
 * @author liudongyu
 */
@OnlyIn(Dist.CLIENT)
public class GirlfriendFishingHookRenderer extends EntityRenderer<GirlfriendFishingHook, FishingHookRenderState> {
	private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");

	public GirlfriendFishingHookRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public FishingHookRenderState createRenderState() {
		return new FishingHookRenderState();
	}

	@Override
	public void extractRenderState(GirlfriendFishingHook entity, FishingHookRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		LivingEntity owner = entity.getLivingOwner();
		if(owner == null) {
			state.lineOriginOffset = Vec3.ZERO;
		} else {
			float yRot = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * (float) (Math.PI / 180.0);
			double sin = Mth.sin(yRot);
			double cos = Mth.cos(yRot);
			Vec3 handPos = owner.getEyePosition(partialTicks)
					.add(-cos * 0.35 - sin * 0.8, -0.45, -sin * 0.35 + cos * 0.8);
			Vec3 hookPos = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);
			state.lineOriginOffset = handPos.subtract(hookPos);
		}
	}

	@Override
	public void submit(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.pushPose();
		poseStack.scale(0.5F, 0.5F, 0.5F);
		poseStack.mulPose(camera.orientation);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutCull(TEXTURE_LOCATION), (pose, buffer) -> {
			this.vertex(buffer, pose, 0.0F, 0, 0, 1);
			this.vertex(buffer, pose, 1.0F, 0, 1, 1);
			this.vertex(buffer, pose, 1.0F, 1, 1, 0);
			this.vertex(buffer, pose, 0.0F, 1, 0, 0);
		});
		poseStack.popPose();

		float xa = (float) state.lineOriginOffset.x;
		float ya = (float) state.lineOriginOffset.y;
		float za = (float) state.lineOriginOffset.z;
		if(state.lineOriginOffset.equals(Vec3.ZERO)) {
			poseStack.popPose();
			super.submit(state, poseStack, submitNodeCollector, camera);
			return;
		}
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
			for(int i = 0; i < 16; i++) {
				float a0 = (float) i / 16.0F;
				float a1 = (float) (i + 1) / 16.0F;
				this.lineVertex(xa, ya, za, buffer, pose, a0, a1);
				this.lineVertex(xa, ya, za, buffer, pose, a1, a0);
			}
		});
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	private void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, int y, int u, int v) {
		buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	private void lineVertex(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float aa, float nexta) {
		float x = xa * aa;
		float y = ya * (aa * aa + aa) * 0.5F + 0.25F;
		float z = za * aa;
		float nx = xa * nexta - x;
		float ny = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - y;
		float nz = za * nexta - z;
		float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
		nx /= length;
		ny /= length;
		nz /= length;
		buffer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx, ny, nz);
	}
}
