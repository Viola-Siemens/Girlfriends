package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
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

	private static final String TAG_GIRLFRIEND_ID = "girlfriend_id";
	private static final String TAG_FOLLOW_MODE = "follow_mode";
	private static final String TAG_FOLLOW_TARGET = "follow_target";

	@Nullable
	private UUID followTargetUuid;

	/** Brain Provider，由子类在构造时设置喵~ */
	protected Brain.Provider<GirlfriendEntity> brainProvider;

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
	@Nullable
	public Optional<GirlfriendType> getGirlfriendType() {
		return this.registryAccess()
				.lookupOrThrow(GirlfriendsRegistries.GIRLFRIEND_TYPE)
				.get(this.getGirlfriendTypeId())
				.map(Holder.Reference::value);
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

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FOLLOW_MODE, FollowMode.STAY.ordinal());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString(TAG_GIRLFRIEND_ID, this.getGirlfriendTypeId().toString());
		output.putString(TAG_FOLLOW_MODE, this.getFollowMode().name());
		if (this.followTargetUuid != null) {
			output.putString(TAG_FOLLOW_TARGET, this.followTargetUuid.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.getString(TAG_FOLLOW_MODE).ifPresent(mode -> {
			try {
				this.setFollowMode(FollowMode.valueOf(mode));
			} catch (IllegalArgumentException ignored) {
				// 忽略无效的跟随模式值喵~
			}
		});
		input.getString(TAG_FOLLOW_TARGET).ifPresent(uuidStr -> {
			try {
				this.followTargetUuid = UUID.fromString(uuidStr);
			} catch (IllegalArgumentException ignored) {
				// 忽略无效的 UUID 字符串喵~
			}
		});
		if (this.brainProvider != null) {
			input.read("Brain", Brain.Packed.CODEC).ifPresent(packed -> this.brain = this.makeBrain(packed));
		}
	}
}
