package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsEnvironmentAttributes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.*;
import com.hexagram2021.girlfriends.common.item.GirlfriendsItemTags;
import com.mojang.datafixers.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * 沫沫实体喵~
 * <p>
 * 热爱花草与蜜蜂的女孩，生成于繁华森林喵~
 * 日程：清晨浇花 → 上午采集花朵+照料蜂箱 → 下午散步 → 傍晚整理干花 → 夜晚赏月喵~
 *
 * @author liudongyu
 */
public class MomoEntity extends GirlfriendEntity {
	private static final String TAG_NEXT_BONE_MEAL_TICK = "next_bone_meal_tick";

	private long nextBoneMealTick = 0L;

	/**
	 * 构造沫沫实体
	 * @param entityType 实体类型
	 * @param level 世界
	 */
	public MomoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public Identifier getGirlfriendTypeId() {
		return GirlfriendTypes.MOMO_ID;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
		return itemStack.is(GirlfriendsItemTags.MOMO_PICKS_UP);
	}

	/**
	 * 更新下次使用骨粉的时间
	 *
	 * @return 如果未到更新时间，则返回 false 且不更新；否则更新并返回 true
	 */
	public boolean updateBoneMealTick() {
		if(!this.getFollowMode().isStayOrFollow() && this.level().getGameTime() >= this.nextBoneMealTick) {
			this.nextBoneMealTick = level().getGameTime() + 30L * SharedConstants.TICKS_PER_SECOND;
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Brain.Provider<GirlfriendEntity> getBrainProvider() {
		return Brain.provider(
				List.of(
						SensorType.HURT_BY,
						GirlfriendsSensorTypes.SHELTER_SENSOR.get(),
						GirlfriendsSensorTypes.FLOWER_SENSOR.get(),
						GirlfriendsSensorTypes.BEEHIVE_SENSOR.get()
				), girlfriend -> {
					ImmutableList.Builder<ActivityData<GirlfriendEntity>> activities = ImmutableList.builder();

					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> core = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> panic = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> morning = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> dayWork = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> afternoon = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> sunset = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> nightRest = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> follow = ImmutableList.builder();

					// 通用行为
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, core);

					// 恐慌行为
					panic.add(
							Pair.of(0, GirlfriendCalmDown.create()),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, 0.8F, 6, false)),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, 0.8F, 6, false)),
							Pair.of(2, ShelterBoundRandomStroll.create(0.8F))
					);

					// 工作行为
					morning.add(
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);
					dayWork.add(
							Pair.of(1, ProduceBoneMeal.create()),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) PlantAndHarvestFlower.create(4, 2.0D)),
							Pair.of(3, new RunOneLoop<>(List.of(
									GoToTargetLocation.create(GirlfriendsMemoryTypes.NEAREST_BEEHIVE.get(), 3, 0.5F),
									GoToTargetLocation.create(GirlfriendsMemoryTypes.NEARBY_FLOWER.get(), 2, 0.5F)
							), 10 * SharedConstants.TICKS_PER_SECOND, 2 * SharedConstants.TICKS_PER_SECOND)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);
					afternoon.add(
							Pair.of(1, ProduceBoneMeal.create()),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 250), 30.0F, -10.0F, 0.0F)),
							Pair.of(3, ShelterBoundRandomStroll.create(0.4F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);
					sunset.add(
							Pair.of(3, BackToShelter.create(2, 48, 0.4F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);
					nightRest.add(
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 跟随行为
					follow.add(
							Pair.of(1, StayCloseToIntimatePlayer.create(3, 16, 1.0F)),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) PlantAndHarvestFlower.create(4, 2.0D))
					);

					activities.add(ActivityData.create(Activity.CORE, core.build()));
					activities.add(ActivityData.create(Activity.PANIC, panic.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.MORNING.get(), morning.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.DAY_WORK.get(), dayWork.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.AFTERNOON.get(), afternoon.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.SUNSET.get(), sunset.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.NIGHT_REST.get(), nightRest.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.FOLLOW.get(), follow.build()));

					return activities.build();
				}
		);
	}

	@Override
	protected void registerBrainGoals(Brain<?> brain) {
		brain.setSchedule(GirlfriendsEnvironmentAttributes.MOMO_ACTIVITY.get());

		brain.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong(TAG_NEXT_BONE_MEAL_TICK, this.nextBoneMealTick);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.nextBoneMealTick = input.getLongOr(TAG_NEXT_BONE_MEAL_TICK, 0L);
	}
}
