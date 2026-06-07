package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.GirlfriendAiPackages;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;
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
	 * 沫沫的 Brain Provider，静态共享喵~
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Brain.Provider<MomoEntity> BRAIN_PROVIDER = Brain.provider(
			(List) ImmutableList.of(GirlfriendsSensorTypes.SHELTER_SENSOR.get()),
			body -> ImmutableList.of()
	);

	public MomoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public Identifier getGirlfriendTypeId() {
		return GirlfriendTypes.MOMO_ID;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Brain<?> makeBrain(net.minecraft.world.entity.ai.Brain.Packed packed) {
		Brain<MomoEntity> brain = (Brain<MomoEntity>) BRAIN_PROVIDER.makeBrain(this, packed);

		// 核心活动 — 游泳等喵~
		brain.addActivity(Activity.CORE, GirlfriendAiPackages.coreActivities(), ImmutableSet.of(), ImmutableSet.of());
		// 闲置活动喵~
		brain.addActivity(Activity.IDLE, GirlfriendAiPackages.idleActivities(), ImmutableSet.of(), ImmutableSet.of());

		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		return brain;
	}
}
