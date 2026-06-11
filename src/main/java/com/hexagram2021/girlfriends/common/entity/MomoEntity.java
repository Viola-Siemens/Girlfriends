package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsEnvironmentAttributes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.BackToShelter;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.GirlfriendCommonAiPackages;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.level.Level;

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

	@SuppressWarnings("unchecked")
	@Override
	protected Brain.Provider<GirlfriendEntity> getBrainProvider() {
		return Brain.provider(
				List.of(
						GirlfriendsSensorTypes.SHELTER_SENSOR.get(),
						GirlfriendsSensorTypes.FLOWER_SENSOR.get(),
						GirlfriendsSensorTypes.BEEHIVE_SENSOR.get()
				), girlfriend -> {
					ImmutableList.Builder<ActivityData<GirlfriendEntity>> activities = ImmutableList.builder();

					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> morning = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> dayWork = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> afternoon = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> sunset = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> nightRest = ImmutableList.builder();

					// 通用行为
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, morning, 3, 16);
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, dayWork, 3, 16);
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, afternoon, 3, 16);
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, sunset, 3, 16);
					GirlfriendCommonAiPackages.addCoreActivities(girlfriend, nightRest, 3, 16);

					// 工作行为
					dayWork.add(
							Pair.of(2, GoToTargetLocation.create(GirlfriendsMemoryTypes.NEAREST_BEEHIVE.get(), 3, 0.6F))
					);
					afternoon.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 250), 30.0F, -10.0F, 0.0F)),
							Pair.of(1, BackToShelter.create(16, 0.6F)),
							Pair.of(2, (BehaviorControl<GirlfriendEntity>)(Object) RandomStroll.stroll(0.5F))
					);

					activities.add(ActivityData.create(GirlfriendsActivities.MORNING.get(), morning.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.DAY_WORK.get(), dayWork.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.AFTERNOON.get(), afternoon.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.SUNSET.get(), sunset.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.NIGHT_REST.get(), nightRest.build()));

					return activities.build();
				}
		);
	}

	@Override
	protected void registerBrainGoals(Brain<?> brain) {
		brain.setSchedule(GirlfriendsEnvironmentAttributes.MOMO_ACTIVITY.get());

		brain.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
	}
}
