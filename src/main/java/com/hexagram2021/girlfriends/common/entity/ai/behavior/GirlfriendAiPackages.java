package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;

/**
 * 角色 AI 行为包工具类喵~
 * <p>
 * 为 Brain.addActivity() 提供 Pair 编排的基础行为喵~
 *
 * @author liudongyu
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class GirlfriendAiPackages {
	private GirlfriendAiPackages() {
	}

	/**
	 * 核心行为包 — 始终生效的游泳行为喵~
	 *
	 * @return 核心行为对列表喵~
	 */
	public static ImmutableList<Pair<Integer, BehaviorControl<GirlfriendEntity>>> coreActivities() {
		return ImmutableList.of(
				Pair.of(0, (BehaviorControl) new Swim(0.8f))
		);
	}

	/**
	 * 默认闲置行为包 — 闲逛、注视、休息喵~
	 *
	 * @return 闲置行为对列表喵~
	 */
	public static ImmutableList<Pair<Integer, BehaviorControl<GirlfriendEntity>>> idleActivities() {
		return ImmutableList.of(
				Pair.of(0, (BehaviorControl) RandomStroll.stroll(0.5f)),
				Pair.of(1, (BehaviorControl) new SetEntityLookTarget()),
				Pair.of(2, (BehaviorControl) new DoNothing(30, 60))
		);
	}
}
