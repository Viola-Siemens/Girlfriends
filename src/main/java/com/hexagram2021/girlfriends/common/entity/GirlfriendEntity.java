package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * 角色实体抽象基类喵~
 * <p>
 * 统一管理角色身份、跟随状态、AI 日程切换和持久化喵~
 * 子类通过覆写 {@link #getGirlfriendTypeId()} 返回对应角色类型 ID，
 * 并注册差异化的 Brain、Sensor 和 Behavior 喵~
 *
 * @author liudongyu
 */
public abstract class GirlfriendEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_FOLLOW_MODE =
			SynchedEntityData.defineId(GirlfriendEntity.class, EntityDataSerializers.INT);

	private static final String TAG_FOLLOW_MODE = "follow_mode";
	private static final String TAG_FOLLOW_TARGET = "follow_target";
	private static final String TAG_LIKED_PLAYER = "liked_player";

	@Nullable
	private UUID likedPlayerUuid;

	@Nullable
	private UUID followTargetUuid;

	protected GirlfriendEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.followTargetUuid = null;
	}

	/**
	 * 获取该实体的角色类型 ID 喵~
	 *
	 * @return 角色类型注册表 key 喵~
	 */
	public abstract Identifier getGirlfriendTypeId();

	/**
	 * 解析角色类型对象喵~
	 *
	 * @return 角色类型，若注册表缺失则返回 null 喵~
	 */
	public Optional<GirlfriendType> getGirlfriendType() {
		return this.registryAccess()
				.lookupOrThrow(GirlfriendsRegistries.GIRLFRIEND_TYPE)
				.get(this.getGirlfriendTypeId())
				.map(Holder.Reference::value);
	}

	@Override
	protected Brain<?> makeBrain(Brain.Packed packed) {
		Brain<?> brain = this.getBrainProvider().makeBrain(this, packed);

		this.registerBrainGoals(brain);

		return brain;
	}

	/**
	 * 获取当前跟随模式喵~
	 *
	 * @return 跟随模式喵~
	 */
	public FollowMode getFollowMode() {
		int id = this.entityData.get(DATA_FOLLOW_MODE);
		return FollowMode.fromId(id);
	}

	/**
	 * 设置跟随模式喵~
	 *
	 * @param mode 新的跟随模式喵~
	 */
	public void setFollowMode(FollowMode mode) {
		this.entityData.set(DATA_FOLLOW_MODE, mode.ordinal());
	}

	/**
	 * 获取跟随目标玩家 UUID 喵~
	 *
	 * @return 跟随目标 UUID，可能为 null 喵~
	 */
	@Nullable
	public UUID getFollowTargetUuid() {
		return this.followTargetUuid;
	}

	/**
	 * 设置跟随目标玩家 UUID 喵~
	 *
	 * @param uuid 目标玩家 UUID，可为 null 喵~
	 */
	public void setFollowTargetUuid(@Nullable UUID uuid) {
		this.followTargetUuid = uuid;
	}

	/**
	 * 获取跟随玩家
	 *
	 * @return 跟随玩家，非跟随状态、非玩家、不同维度时返回 null
	 */
	@Nullable
	public Player getFollowedPlayer() {
		if(this.followTargetUuid != null && this.level().getEntity(this.followTargetUuid) instanceof Player player) {
			return player;
		}
		return null;
	}

	/**
	 * 判断是否对指定玩家感兴趣<br/>
	 * 仅当与该玩家确认关系，或未与任何玩家确认关系时，返回 true
	 *
	 * @param player 玩家
	 * @return 是否对指定玩家感兴趣
	 */
	public boolean isInterestedIn(Player player) {
		if(this.likedPlayerUuid == null) {
			return true;
		}
		return this.likedPlayerUuid.equals(player.getUUID());
	}

	/**
	 * 创建女友通用属性喵~
	 *
	 * @return 属性构建器喵~
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.FOLLOW_RANGE, 48.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.5D)
				.add(Attributes.MAX_HEALTH, 40.0D)
				.add(Attributes.ATTACK_DAMAGE, 2.0D)
				.add(Attributes.ARMOR, 0.0D);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void customServerAiStep(ServerLevel level) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("villagerBrain");
		((Brain<GirlfriendEntity>) this.getBrain()).tick(level, this);
		profiler.pop();
	}

	/**
	 * 获取角色实体的脑提供器
	 * @return 脑提供器
	 */
	protected abstract Brain.Provider<GirlfriendEntity> getBrainProvider();

	/**
	 * 注册角色实体的脑行为
	 * @param brain 实体的脑
	 */
	protected abstract void registerBrainGoals(Brain<?> brain);

	/**
	 * 刷新 AI
	 * @param level 世界
	 */
	@SuppressWarnings("unchecked")
	protected void refreshBrain(ServerLevel level) {
		Brain<GirlfriendEntity> oldBrain = (Brain<GirlfriendEntity>)this.getBrain();
		oldBrain.stopAll(level, this);
		this.brain = this.getBrainProvider().makeBrain(this, oldBrain.pack());
		this.registerBrainGoals(this.getBrain());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FOLLOW_MODE, FollowMode.STAY.ordinal());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store(TAG_FOLLOW_MODE, FollowMode.CODEC, this.getFollowMode());
		if (this.followTargetUuid != null) {
			output.store(TAG_FOLLOW_TARGET, UUIDUtil.CODEC, this.followTargetUuid);
		}
		if (this.likedPlayerUuid != null) {
			output.store(TAG_LIKED_PLAYER, UUIDUtil.CODEC, this.likedPlayerUuid);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read(TAG_FOLLOW_MODE, FollowMode.CODEC).ifPresent(this::setFollowMode);
		input.read(TAG_FOLLOW_TARGET, UUIDUtil.CODEC).ifPresent(uuid -> this.followTargetUuid = uuid);
		input.read(TAG_LIKED_PLAYER, UUIDUtil.CODEC).ifPresent(uuid -> this.likedPlayerUuid = uuid);
		input.read("Brain", Brain.Packed.CODEC).ifPresent(packed -> this.brain = this.makeBrain(packed));
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
