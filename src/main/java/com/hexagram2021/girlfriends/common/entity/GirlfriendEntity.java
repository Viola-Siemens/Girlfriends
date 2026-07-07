package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummaryService;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundSyncInteractionDataPacket;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.FixedQuestDefinitionManager;
import com.hexagram2021.girlfriends.common.quest.QuestService;
import com.hexagram2021.girlfriends.common.quest.RandomQuestTemplateManager;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
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
@SuppressWarnings("java:S2160")
public abstract class GirlfriendEntity extends PathfinderMob implements InventoryCarrier {
	private static final EntityDataAccessor<Integer> DATA_FOLLOW_MODE =
			SynchedEntityData.defineId(GirlfriendEntity.class, EntityDataSerializers.INT);

	private static final String TAG_FOLLOW_MODE = "follow_mode";
	private static final String TAG_LIKED_PLAYER = "liked_player";

	private static final int INVENTORY_SIZE = 18;

	private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);

	private int healCooldown = 0;
	private int worldStateSyncCooldown = 0;

	@Nullable
	private UUID likedPlayerUuid;

	protected GirlfriendEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.likedPlayerUuid = null;
		this.setCanPickUpLoot(true);
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
	public abstract Holder<GirlfriendType> getGirlfriendType();

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
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
	 * 获取跟随玩家<br/>
	 * 角色只有可能跟随爱慕的玩家
	 *
	 * @return 跟随玩家，非跟随状态、非玩家、不同维度时返回 null
	 */
	@Nullable
	public Player getFollowedPlayer() {
		if(this.likedPlayerUuid != null &&
				this.getFollowMode().isStayOrFollow() &&
				this.level().getEntity(this.likedPlayerUuid) instanceof Player player) {
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
	 * 设置当前爱慕玩家 UUID 喵~
	 *
	 * @param playerUuid 玩家 UUID，null 表示清除喵~
	 */
	public void setLikedPlayerUuid(@Nullable UUID playerUuid) {
		this.likedPlayerUuid = playerUuid;
	}

	/**
	 * 将实体运行时状态同步到持久化世界数据喵~
	 * <br/>
	 * 将 entityUuid、followMode、followTargetUuid 写入 CharacterWorldState 喵~
	 *
	 * @param data 世界数据喵~
	 */
	public void syncToWorldState(GirlfriendsWorldData data) {
		Identifier girlfriendTypeId = this.getGirlfriendTypeId();
		data.updateCharacter(girlfriendTypeId, state -> state.setEntityUuid(this.getUUID()));
	}

	/**
	 * 从持久化世界数据同步到实体运行时状态喵~
	 * <p>
	 * 读取 CharacterWorldState 的 followMode、followTargetUuid 并应用到实体喵~
	 *
	 * @param data 世界数据喵~
	 */
	public void syncFromWorldState(GirlfriendsWorldData data) {
		Identifier girlfriendTypeId = this.getGirlfriendTypeId();
		CharacterWorldState state = data.getExistingCharacterState(girlfriendTypeId);
		if (state != null) {
			this.setFollowMode(state.getFollowMode());
			this.likedPlayerUuid = state.getFollowTargetUuid();
		}
	}

	/**
	 * 玩家与实体交互喵~
	 * <p>
	 * 服务端：构建交互摘要并通过网络包发送给玩家喵~
	 * 客户端：返回 SUCCESS 触发 GUI 打开（SubTask 2 实现）喵~
	 *
	 * @param player 玩家喵~
	 * @param hand 交互手喵~
	 * @return 交互结果喵~
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide()) {
			ClientInteractionStore.markPendingInteraction(this.getGirlfriendTypeId());
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			ServerLevel level = (ServerLevel)this.level();
			GirlfriendsWorldData data = level.getServer().overworld().getDataStorage().computeIfAbsent(GirlfriendsWorldData.TYPE);
			// 先同步实体状态到世界数据
			this.syncToWorldState(data);
			// 构建并发送交互摘要
			InteractionSummaryService summaryService = this.getInteractionSummaryService(data, level);
			serverPlayer.connection.send(new ClientboundSyncInteractionDataPacket(
					summaryService.build(player.getUUID(), this.getGirlfriendTypeId())
			));
		}
		return InteractionResult.SUCCESS;
	}

	private InteractionSummaryService getInteractionSummaryService(GirlfriendsWorldData data, ServerLevel level) {
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService questService = new QuestService(
				data,
				relationshipService,
				id -> FixedQuestDefinitionManager.INSTANCE.getDefinition(id).orElse(null),
				id -> RandomQuestTemplateManager.INSTANCE.getRandomDefinitionForType(id, level.getRandom()),
				randomSource -> 5 + randomSource.nextInt(6)
		);
		return new InteractionSummaryService(data, relationshipService, questService);
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

	@Override
	public SimpleContainer getInventory() {
		return this.inventory;
	}

	@Override
	protected Brain<?> makeBrain(Brain.Packed packed) {
		Brain<?> brain = this.getBrainProvider().makeBrain(this, packed);

		this.registerBrainGoals(brain);

		return brain;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void customServerAiStep(ServerLevel level) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("girlfriendBrain");
		((Brain<GirlfriendEntity>) this.getBrain()).tick(level, this);
		profiler.pop();

		// 随机恢复 1 点生命值
		this.healCooldown -= 1;
		if(this.healCooldown <= 0) {
			if(this.getHealth() < this.getMaxHealth()) {
				this.heal(1.0F);
			}
			this.healCooldown = this.getNextHealCooldown();
		}

		// 每 200 tick（10 秒）同步实体状态到世界数据喵~
		this.worldStateSyncCooldown -= 1;
		if (this.worldStateSyncCooldown <= 0) {
			GirlfriendsWorldData data = level.getServer().overworld().getDataStorage().computeIfAbsent(GirlfriendsWorldData.TYPE);
			this.syncToWorldState(data);
			this.worldStateSyncCooldown = 200 + this.getRandom().nextInt(40);
		}

		super.customServerAiStep(level);
	}

	@Override
	protected void pickUpItem(ServerLevel level, ItemEntity entity) {
		InventoryCarrier.pickUpItem(level, this, this, entity);
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
		if (this.likedPlayerUuid != null) {
			output.store(TAG_LIKED_PLAYER, UUIDUtil.CODEC, this.likedPlayerUuid);
		}

		this.writeInventoryToTag(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read(TAG_FOLLOW_MODE, FollowMode.CODEC).ifPresent(this::setFollowMode);
		input.read(TAG_LIKED_PLAYER, UUIDUtil.CODEC).ifPresent(uuid -> this.likedPlayerUuid = uuid);
		input.read("Brain", Brain.Packed.CODEC).ifPresent(packed -> this.brain = this.makeBrain(packed));

		this.readInventoryFromTag(input);
	}

	protected int getNextHealCooldown() {
		// 默认每 4~5 秒回复一点生命值
		return 4 * SharedConstants.TICKS_PER_SECOND + this.getRandom().nextInt(SharedConstants.TICKS_PER_SECOND);
	}
}
