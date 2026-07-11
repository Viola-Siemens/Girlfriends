# YuxiEntity 钓鱼功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 YuxiEntity 实现基于原版 FishingHook 机制的钓鱼 AI Behavior，使渔溪能在手持钓竿时在水域旁抛竿垂钓并产出战利品。

**Architecture:** 新建 `GirlfriendFishingHook`（Projectile 子类）作为浮标实体，新建 `FishNearbyWater`（Behavior 子类）管理抛竿→等待→收竿→冷却生命周期，通过 `BuiltInLootTables.FISHING` 战利品表产出物品到渔溪背包。

**Tech Stack:** Java 25, NeoForge 26.1.2, Minecraft 26.1.2, Mojang official mappings

## Global Constraints

- Java 25 toolchain, Mojang official mappings
- 物理端隔离：`common/` 包代码不得 import `net.minecraft.client.*` 或 `com.hexagram2021.girlfriends.client.*`
- 缩进使用 Tab，大括号 K&R 风格
- 每行不超过 150 字符
- 提交信息格式：`feat(REQ-7): subject`
- 服务端权威：所有判定在服务端执行
- 跟随模式不钓鱼（仅 dayWork activity）

---

## 文件结构

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `common/entity/GirlfriendsEntities.java` | 注册 `GIRLFRIEND_FISHING_HOOK` EntityType |
| 创建 | `common/entity/GirlfriendFishingHook.java` | 浮标 Projectile 实体：状态机、粒子、战利品 |
| 创建 | `common/entity/ai/behavior/FishNearbyWater.java` | 钓鱼 Behavior：生命周期管理 |
| 修改 | `common/entity/YuxiEntity.java` | dayWork 注册 FishNearbyWater |
| 创建 | `client/renderer/GirlfriendFishingHookRenderer.java` | 客户端浮标渲染 |
| 修改 | `client/GirlfriendsModClient.java` | 注册渲染器 |

依赖链：Task 1 → Task 2 → Task 4,5,6（Task 3 和 Task 2 可并行）

---

### Task 1: 注册 GIRLFRIEND_FISHING_HOOK EntityType

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendsEntities.java`

**Interfaces:**
- Produces: `GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK` — `DeferredHolder<EntityType<?>, EntityType<GirlfriendFishingHook>>`

- [ ] **Step 1: 在 GirlfriendsEntities 中添加 EntityType 注册**

在 `MEISHU` 注册之后、`WANYING` 注册之前插入以下代码：

```java
	/** 渔溪钓竿浮标实体类型喵~ */
	public static final DeferredHolder<EntityType<?>, EntityType<GirlfriendFishingHook>> GIRLFRIEND_FISHING_HOOK =
			REGISTER.register("girlfriend_fishing_hook", () -> EntityType.Builder
					.of(GirlfriendFishingHook::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.build(ResourceKey.create(Registries.ENTITY_TYPE,
							Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "girlfriend_fishing_hook"))));
```

同时在文件头部添加 import：
```java
import com.hexagram2021.girlfriends.common.entity.GirlfriendFishingHook;
```
（文件其余 import 已存在，无需额外添加）

- [ ] **Step 2: 验证编译**

```bash
./gradlew classes
```

预期：编译失败（`GirlfriendFishingHook` 类尚未创建），这是预期的。后续 Task 2 创建后会通过。

- [ ] **Step 3: 提交（与 Task 2 一起提交）**

---

### Task 2: 创建 GirlfriendFishingHook 实体类

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendFishingHook.java`

**Interfaces:**
- Produces: `GirlfriendFishingHook extends Projectile` — 状态机 `FLYING→BOBBING→BITING`，`isBiting(): boolean`，`retrieve(ItemStack): void`，`setupFishing(LivingEntity): void`

- [ ] **Step 1: 创建 GirlfriendFishingHook.java**

```java
package com.hexagram2021.girlfriends.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 自定义浮标实体 — 供角色实体（非 Player）钓鱼使用喵~
 * <p>
 * 参照原版 {@link net.minecraft.world.entity.projectile.FishingHook} 实现，
 * 但不依赖 Player owner，适配 {@link GirlfriendEntity} 喵~
 *
 * @author liudongyu
 */
public class GirlfriendFishingHook extends Projectile {
	private enum State { FLYING, BOBBING }

	private static final EntityDataAccessor<Boolean> DATA_BITING =
			SynchedEntityData.defineId(GirlfriendFishingHook.class, EntityDataSerializers.BOOLEAN);

	private final RandomSource syncronizedRandom = RandomSource.create();
	private State state = State.FLYING;
	private int timeUntilLured;
	private int timeUntilHooked;
	private int nibble;
	private int outOfWaterTime;
	private int life;

	/**
	 * 供 EntityType.Builder 反射调用的构造器喵~
	 *
	 * @param type  实体类型喵~
	 * @param level 世界喵~
	 */
	public GirlfriendFishingHook(EntityType<? extends GirlfriendFishingHook> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_BITING, false);
	}

	/**
	 * 设置浮标 owner 并计算抛射初速度喵~
	 * <p>
	 * 在 {@link net.minecraft.world.level.Level#addFreshEntity(Entity)} 之前调用喵~
	 *
	 * @param owner 钓鱼的角色实体喵~
	 */
	public void setupFishing(LivingEntity owner) {
		this.setOwner(owner);
		this.setPos(owner.getEyePosition());

		float yRot = owner.getYRot();
		float xRot = owner.getXRot();
		float yRotRad = -yRot * (float) (Math.PI / 180.0) - (float) Math.PI;
		float ySin = Mth.sin(yRotRad);
		float yCos = Mth.cos(yRotRad);
		float xCos = -Mth.cos(-xRot * (float) (Math.PI / 180.0));
		float xSin = Mth.sin(-xRot * (float) (Math.PI / 180.0));
		this.snapTo(this.getX() - ySin * 0.3, this.getY(), this.getZ() - yCos * 0.3, yRot, xRot);
		Vec3 direction = new Vec3(-ySin, Mth.clamp(-(xSin / xCos), -5.0F, 5.0F), -yCos);
		double length = direction.length();
		direction = direction.multiply(
				0.6 / length + this.random.triangle(0.5, 0.0103365),
				0.6 / length + this.random.triangle(0.5, 0.0103365),
				0.6 / length + this.random.triangle(0.5, 0.0103365)
		);
		this.setDeltaMovement(direction);
		this.setYRot((float) (Mth.atan2(direction.x, direction.z) * 180.0F / (float) Math.PI));
		this.setXRot((float) (Mth.atan2(direction.y, direction.horizontalDistance()) * 180.0F / (float) Math.PI));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	/**
	 * 检查是否正在咬钩喵~
	 *
	 * @return nibble > 0 时返回 true 喵~
	 */
	public boolean isBiting() {
		return this.nibble > 0;
	}

	/**
	 * 获取浮标当前 owner 喵~
	 *
	 * @return owner，若不为 LivingEntity 则返回 null 喵~
	 */
	public @Nullable LivingEntity getLivingOwner() {
		return this.getOwner() instanceof LivingEntity le ? le : null;
	}

	@Override
	public void tick() {
		this.syncronizedRandom.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level().getGameTime());
		super.tick();

		LivingEntity owner = this.getLivingOwner();
		if(owner == null || !this.shouldKeepFishing(owner)) {
			this.discard();
			return;
		}

		if(this.onGround()) {
			this.life++;
			if(this.life >= 1200) {
				this.discard();
				return;
			}
		} else {
			this.life = 0;
		}

		if(this.level().isClientSide()) {
			return;
		}

		BlockPos blockPos = this.blockPosition();
		FluidState fluidState = this.level().getFluidState(blockPos);

		if(this.state == State.FLYING) {
			if(fluidState.is(FluidTags.WATER)) {
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.2, 0.3));
				this.state = State.BOBBING;
				this.timeUntilLured = Mth.nextInt(this.random, 100, 600);
			} else {
				this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
			}
		} else {
			// BOBBING state
			float liquidHeight = fluidState.is(FluidTags.WATER) ?
					fluidState.getHeight(this.level(), blockPos) : 0.0F;
			boolean inWater = liquidHeight > 0.0F;

			Vec3 movement = this.getDeltaMovement();
			double force = this.getY() + movement.y - blockPos.getY() - liquidHeight;
			if(Math.abs(force) < 0.01) {
				force += Math.signum(force) * 0.1;
			}
			this.setDeltaMovement(movement.x * 0.9, movement.y - force * this.random.nextFloat() * 0.2, movement.z * 0.9);

			if(inWater) {
				this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
				this.catchingFish(blockPos);
			} else {
				this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
			}
		}

		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
		this.reapplyPosition();
	}

	/**
	 * 核心钓鱼逻辑 — 参照原版 catchingFish 实现喵~
	 */
	@SuppressWarnings("resource")
	private void catchingFish(BlockPos blockPos) {
		ServerLevel serverLevel = (ServerLevel) this.level();
		int fishingSpeed = 1;
		BlockPos above = blockPos.above();
		if(this.random.nextFloat() < 0.25F && this.level().isRainingAt(above)) {
			fishingSpeed++;
		}
		if(this.random.nextFloat() < 0.5F && !this.level().canSeeSky(above)) {
			fishingSpeed--;
		}

		if(this.nibble > 0) {
			this.nibble--;
			if(this.nibble <= 0) {
				this.timeUntilLured = 0;
				this.timeUntilHooked = 0;
				this.entityData.set(DATA_BITING, false);
			}
		} else if(this.timeUntilHooked > 0) {
			this.timeUntilHooked -= fishingSpeed;
			if(this.timeUntilHooked > 0) {
				float angle = Mth.nextFloat(this.random, 0.0F, 360.0F) * (float) (Math.PI / 180.0);
				float angleSin = Mth.sin(angle);
				float angleCos = Mth.cos(angle);
				double fishX = this.getX() + angleSin * this.timeUntilHooked * 0.1F;
				double fishY = Mth.floor(this.getY()) + 1.0;
				double fishZ = this.getZ() + angleCos * this.timeUntilHooked * 0.1F;
				BlockState splashBlockState = serverLevel.getBlockState(BlockPos.containing(fishX, fishY - 1.0, fishZ));
				if(splashBlockState.is(Blocks.WATER)) {
					if(this.random.nextFloat() < 0.15F) {
						serverLevel.sendParticles(ParticleTypes.BUBBLE, fishX, fishY - 0.1F, fishZ, 1, angleSin, 0.1, angleCos, 0.0);
					}
					float pdx = angleSin * 0.04F;
					float pdz = angleCos * 0.04F;
					serverLevel.sendParticles(ParticleTypes.FISHING, fishX, fishY, fishZ, 0, pdz, 0.01, -pdx, 1.0);
					serverLevel.sendParticles(ParticleTypes.FISHING, fishX, fishY, fishZ, 0, -pdz, 0.01, pdx, 1.0);
				}
			} else {
				// 鱼上钩
				this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
				double y = this.getY() + 0.5;
				serverLevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), y, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
				serverLevel.sendParticles(ParticleTypes.FISHING, this.getX(), y, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
				this.nibble = Mth.nextInt(this.random, 20, 40);
				this.entityData.set(DATA_BITING, true);
			}
		} else if(this.timeUntilLured > 0) {
			this.timeUntilLured -= fishingSpeed;
			if(this.random.nextFloat() < 0.15F) {
				// 溅水粒子喵~
				float angle = Mth.nextFloat(this.random, 0.0F, 360.0F) * (float) (Math.PI / 180.0);
				float dist = Mth.nextFloat(this.random, 25.0F, 60.0F);
				double fishX = this.getX() + Mth.sin(angle) * dist * 0.1;
				double fishY = Mth.floor(this.getY()) + 1.0;
				double fishZ = this.getZ() + Mth.cos(angle) * dist * 0.1;
				BlockState splashBlockState = serverLevel.getBlockState(BlockPos.containing(fishX, fishY - 1.0, fishZ));
				if(splashBlockState.is(Blocks.WATER)) {
					serverLevel.sendParticles(ParticleTypes.SPLASH, fishX, fishY, fishZ, 2 + this.random.nextInt(2), 0.1F, 0.0, 0.1F, 0.0);
				}
			}
			if(this.timeUntilLured <= 0) {
				this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
			}
		} else {
			this.timeUntilLured = Mth.nextInt(this.random, 100, 600);
		}
	}

	/**
	 * 收竿：获取战利品 → 生成物品 → 消耗钓竿耐久 → 清除浮标喵~
	 *
	 * @param rod 钓竿物品喵~
	 */
	public void retrieve(ItemStack rod) {
		LivingEntity owner = this.getLivingOwner();
		if(this.level().isClientSide() || owner == null) {
			this.discard();
			return;
		}
		if(this.nibble > 0) {
			LootParams params = new LootParams.Builder((ServerLevel) this.level())
					.withParameter(LootContextParams.ORIGIN, this.position())
					.withParameter(LootContextParams.TOOL, rod)
					.withParameter(LootContextParams.THIS_ENTITY, this)
					.withParameter(LootContextParams.ATTACKING_ENTITY, this.getOwner())
					.withLuck(0)
					.create(LootContextParamSets.FISHING);
			LootTable lootTable = this.level().getServer()
					.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
			List<ItemStack> items = lootTable.getRandomItems(params);

			for(ItemStack itemStack : items) {
				ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack);
				double dx = owner.getX() - this.getX();
				double dy = owner.getY() - this.getY();
				double dz = owner.getZ() - this.getZ();
				itemEntity.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
				this.level().addFreshEntity(itemEntity);
				this.level()
						.addFreshEntity(new ExperienceOrb(this.level(), owner.getX(), owner.getY() + 0.5, owner.getZ() + 0.5,
								this.random.nextInt(6) + 1));
			}

			rod.hurtAndBreak(1, owner, LivingEntity.getSlotForHand(net.minecraft.world.InteractionHand.MAIN_HAND));
		}
		this.discard();
	}

	/**
	 * 检查是否应继续钓鱼喵~
	 *
	 * @param owner 钓鱼的角色实体喵~
	 * @return 是否继续钓鱼喵~
	 */
	private boolean shouldKeepFishing(LivingEntity owner) {
		if(!owner.isAlive() || !owner.canInteractWithLevel()) {
			return false;
		}
		if(this.distanceToSqr(owner) > 1024.0) {
			return false;
		}
		ItemStack mainHand = owner.getMainHandItem();
		return mainHand.canPerformAction(ItemAbilities.FISHING_ROD_CAST);
	}
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew classes
```

预期：编译成功。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendFishingHook.java src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendsEntities.java
git commit -m "feat(REQ-7): add GirlfriendFishingHook entity for Yuxi fishing"
```

---

### Task 3: 创建客户端渲染器

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/client/renderer/GirlfriendFishingHookRenderer.java`

**Interfaces:**
- Consumes: `GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK` (from Task 1), `GirlfriendFishingHook` (from Task 2)
- Produces: `GirlfriendFishingHookRenderer` — EntityRenderer 子类

- [ ] **Step 1: 创建 GirlfriendFishingHookRenderer.java**

```java
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
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew classes
```

预期：编译成功。

- [ ] **Step 3: 提交（与 Task 4 一起提交）**

---

### Task 4: 在 GirlfriendsModClient 中注册渲染器

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java`

**Interfaces:**
- Consumes: `GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK` (from Task 1), `GirlfriendFishingHookRenderer` (from Task 3)

- [ ] **Step 1: 修改 onRegisterRenderers 方法**

在现有五个 `event.registerEntityRenderer` 调用之后，添加：

```java
		event.registerEntityRenderer(
				GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK.get(),
				GirlfriendFishingHookRenderer::new
		);
```

同时在文件头部添加 import：
```java
import com.hexagram2021.girlfriends.client.renderer.GirlfriendFishingHookRenderer;
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew classes
```

预期：编译成功。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/client/renderer/GirlfriendFishingHookRenderer.java src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java
git commit -m "feat(REQ-7): add GirlfriendFishingHook renderer"
```

---

### Task 5: 创建 FishNearbyWater Behavior

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/entity/ai/behavior/FishNearbyWater.java`

**Interfaces:**
- Consumes: `GirlfriendFishingHook` (from Task 2), `GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK` (from Task 1)
- Produces: `FishNearbyWater extends Behavior<GirlfriendEntity>` — 管理钓鱼生命周期

- [ ] **Step 1: 创建 FishNearbyWater.java**

```java
package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.GirlfriendFishingHook;
import com.hexagram2021.girlfriends.common.entity.GirlfriendsEntities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 渔溪钓鱼行为喵~
 * <p>
 * 管理抛竿→等待→收竿→冷却的完整钓鱼生命周期喵~
 *
 * @author liudongyu
 */
public class FishNearbyWater extends Behavior<GirlfriendEntity> {
	private static final int MAX_FISHING_TIME = 1200;
	private static final int COOLDOWN_TICKS = 100;
	private static final double MAX_DISTANCE_TO_WATER = 8.0;

	private @Nullable GirlfriendFishingHook activeHook;
	private long nextCastAvailableTick;

	public FishNearbyWater() {
		super(Map.of(GirlfriendsMemoryTypes.NEAREST_WATER.get(), MemoryStatus.VALUE_PRESENT), MAX_FISHING_TIME);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, GirlfriendEntity entity) {
		// 冷却检查喵~
		if(level.getGameTime() < this.nextCastAvailableTick) {
			return false;
		}
		// 背包必须有钓竿喵~
		if(!this.hasFishingRod(entity)) {
			return false;
		}
		// 距水域不超过最大距离喵~
		return entity.getBrain().getMemory(GirlfriendsMemoryTypes.NEAREST_WATER.get())
				.filter(waterPos -> entity.position().closerThan(waterPos.getBottomCenter(), MAX_DISTANCE_TO_WATER))
				.isPresent();
	}

	@Override
	protected void start(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		// 从背包找出钓竿并装备到主手喵~
		this.equipFishingRod(entity);
		// 生成浮标实体喵~
		GirlfriendFishingHook hook = GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK.get().create(level);
		hook.setupFishing(entity);
		level.addFreshEntity(hook);
		this.activeHook = hook;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		if(this.activeHook == null || !this.activeHook.isAlive()) {
			return false;
		}
		// 主手必须仍有钓竿喵~
		if(!entity.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return false;
		}
		return true;
	}

	@Override
	protected void tick(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		if(this.activeHook != null && this.activeHook.isBiting()) {
			// 鱼已上钩，收竿喵~
			this.activeHook.retrieve(entity.getMainHandItem());
			this.activeHook = null;
			this.nextCastAvailableTick = level.getGameTime() + COOLDOWN_TICKS;
		}
	}

	@Override
	protected void stop(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		// 清理悬空的浮标喵~
		if(this.activeHook != null) {
			if(this.activeHook.isAlive()) {
				this.activeHook.discard();
			}
			this.activeHook = null;
		}
	}

	/**
	 * 检查背包是否含有钓竿喵~
	 */
	private boolean hasFishingRod(GirlfriendEntity entity) {
		SimpleContainer inventory = entity.getInventory();
		for(int slot = 0; slot < inventory.getContainerSize(); ++slot) {
			ItemStack stack = inventory.getItem(slot);
			if(!stack.isEmpty() && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 将钓竿装备到主手，原主手物品放回背包喵~
	 */
	private void equipFishingRod(GirlfriendEntity entity) {
		SimpleContainer inventory = entity.getInventory();
		// 如果主手已是钓竿则无需装备喵~
		if(entity.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return;
		}
		for(int slot = 0; slot < inventory.getContainerSize(); ++slot) {
			ItemStack stack = inventory.getItem(slot);
			if(!stack.isEmpty() && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
				ItemStack rodStack = inventory.removeItemNoUpdate(slot);
				if(!rodStack.isEmpty()) {
					// 原主手物品与钓竿互换喵~
					inventory.addItem(entity.getItemInHand(InteractionHand.MAIN_HAND));
					entity.setItemInHand(InteractionHand.MAIN_HAND, rodStack);
					break;
				}
			}
		}
	}
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew classes
```

预期：编译成功。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/entity/ai/behavior/FishNearbyWater.java
git commit -m "feat(REQ-7): add FishNearbyWater behavior for Yuxi fishing"
```

---

### Task 6: 在 YuxiEntity 中注册 FishNearbyWater

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/entity/YuxiEntity.java`

**Interfaces:**
- Consumes: `FishNearbyWater` (from Task 5)
- 变更：dayWork activity 中 priority 2 注册 `FishNearbyWater`

- [ ] **Step 1: 修改 dayWork activity**

将当前的 dayWork 块：

```java
						// 上午：垂钓喵~
						dayWork.add(
								Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) GoToTargetLocation.create(GirlfriendsMemoryTypes.NEAREST_WATER.get(), 4, 0.5F)),
								Pair.of(2, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 300), 30.0F, -10.0F, 0.0F)),
								Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
						);
```

替换为：

```java
						// 上午：垂钓喵~
						dayWork.add(
								Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) GoToTargetLocation.create(GirlfriendsMemoryTypes.NEAREST_WATER.get(), 4, 0.5F)),
								Pair.of(2, new FishNearbyWater()),
								Pair.of(3, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 300), 30.0F, -10.0F, 0.0F)),
								Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
						);
```

其他 activity（panic / morning / afternoon / sunset / nightRest / follow / core）保持不变。

- [ ] **Step 2: 编译验证**

```bash
./gradlew classes
```

预期：编译成功。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/entity/YuxiEntity.java
git commit -m "feat(REQ-7): register FishNearbyWater in YuxiEntity dayWork"
```

---

### Task 7: 运行全部测试

**Files:** 无变更

- [ ] **Step 1: 运行全部单元测试**

```bash
./gradlew test
```

预期：所有已有测试通过（`BUILD SUCCESSFUL`）。

- [ ] **Step 2: 检查测试输出**

确认无回归失败的测试用例。

---

## 实施顺序

```
Task 1 (注册 EntityType) → Task 2 (创建 Hook 实体)
                                ↓
                    Task 3 (创建渲染器)   Task 5 (创建 Behavior)
                        ↓                    ↓
                    Task 4 (注册渲染器)  ↔  Task 6 (更新 YuxiEntity)
                                ↓
                            Task 7 (测试)
```

推荐按顺序执行 Task 1→2→5→6→7（核心逻辑链），然后 3→4（渲染链），最后再次测试验证。
