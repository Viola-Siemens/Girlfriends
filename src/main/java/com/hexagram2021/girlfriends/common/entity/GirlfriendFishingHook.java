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
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

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

	private State state = State.FLYING;
	private int timeUntilLured;
	private int timeUntilHooked;
	private int nibble;
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
				this.catchingFish(blockPos);
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

			rod.hurtAndBreak(1, owner, InteractionHand.MAIN_HAND.asEquipmentSlot());
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
