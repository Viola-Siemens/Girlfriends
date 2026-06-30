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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 梅疏实体喵~
 * <p>
 * 沉默寡言、专注于矿石与材料的女孩，生成于尖峭山峰喵~
 * 日程：清晨检查矿道 → 上午采矿 → 下午分类冶炼 → 傍晚堆放材料 → 夜晚研究样本喵~
 *
 * @author liudongyu
 */
public class MeishuEntity extends GirlfriendEntity {
	/**
	 * 构造梅疏实体
	 * @param entityType 实体类型
	 * @param level 世界
	 */
	public MeishuEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public Identifier getGirlfriendTypeId() {
		return GirlfriendTypes.MEISHU_ID;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
		return itemStack.is(GirlfriendsItemTags.MEISHU_PICKS_UP);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Brain.Provider<GirlfriendEntity> getBrainProvider() {
		return Brain.provider(
				List.of(
						SensorType.HURT_BY,
						GirlfriendsSensorTypes.SHELTER_SENSOR.get(),
						GirlfriendsSensorTypes.ORE_SENSOR.get()
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

					// 清晨：检查矿道安全喵~
					morning.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) GoToTargetLocation.create(GirlfriendsMemoryTypes.NEARBY_ORE.get(), 4, 0.5F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 上午：采矿喵~
					dayWork.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) GoToTargetLocation.create(GirlfriendsMemoryTypes.NEARBY_ORE.get(), 2, 0.5F)),
							Pair.of(2, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 300), 30.0F, -10.0F, 0.0F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 下午：分类冶炼喵~
					afternoon.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 250), 30.0F, -10.0F, 0.0F)),
							Pair.of(3, ShelterBoundRandomStroll.create(0.4F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 傍晚：堆放材料喵~
					sunset.add(
							Pair.of(3, BackToShelter.create(2, 48, 0.4F)),
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 夜晚：研究样本喵~
					nightRest.add(
							Pair.of(49, (BehaviorControl<GirlfriendEntity>)(Object) UpdateActivityFromSchedule.create())
					);

					// 跟随行为
					follow.add(
							Pair.of(1, StayCloseToIntimatePlayer.create(3, 16, 1.0F))
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
		brain.setSchedule(GirlfriendsEnvironmentAttributes.MEISHU_ACTIVITY.get());

		brain.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
	}
}
