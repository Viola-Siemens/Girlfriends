package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsEnvironmentAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;

/**
 * 沫沫实体喵~
 * <p>
 * 热爱花草与蜜蜂的女孩，生成于繁华森林喵~
 * 日程：清晨浇花 → 上午采集花朵+照料蜂箱 → 下午散步 → 傍晚整理干花 → 夜晚赏月喵~
 *
 * @author liudongyu
 */
public class MomoEntity extends GirlfriendEntity {
	public MomoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public Identifier getGirlfriendTypeId() {
		return GirlfriendTypes.MOMO_ID;
	}

	@Override
	protected Brain.Provider<GirlfriendEntity> getBrainProvider() {
		return Brain.provider(
				ImmutableList.of(GirlfriendsSensorTypes.SHELTER_SENSOR.get()),
				_ -> {
					ImmutableList.Builder<ActivityData<GirlfriendEntity>> activities = ImmutableList.builder();

					activities.add(ActivityData.create(GirlfriendsActivities.MORNING.get(), ImmutableList.of(
					)));
					activities.add(ActivityData.create(GirlfriendsActivities.DAY_WORK.get(), ImmutableList.of(
					)));
					activities.add(ActivityData.create(GirlfriendsActivities.AFTERNOON.get(), ImmutableList.of(
					)));
					activities.add(ActivityData.create(GirlfriendsActivities.SUNSET.get(), ImmutableList.of(
					)));
					activities.add(ActivityData.create(GirlfriendsActivities.NIGHT_REST.get(), ImmutableList.of(
					)));

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
