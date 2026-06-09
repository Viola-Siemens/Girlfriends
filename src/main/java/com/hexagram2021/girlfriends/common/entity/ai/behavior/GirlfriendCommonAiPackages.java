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
 * 注意：26.1.2 中 SetEntityLookTarget 和 RandomStroll 为工具类，需通过静态工厂方法获取实例喵~
 *
 * @author liudongyu
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class GirlfriendCommonAiPackages {
	/**
	 * 核心行为包 — 始终生效的游泳行为喵~
	 *
	 * @param builder 行为列表
	 */
	public static void addCoreActivities(ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> builder) {
		builder.add(
				Pair.of(0, new Swim(0.8f))
		);
	}

	/**
	 * 默认闲置行为包 — 闲逛、注视、休息喵~
	 *
	 * @param builder 行为列表
	 */
	public static void addIdleActivities(ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> builder) {
		builder.add(
				Pair.of(0, (BehaviorControl) RandomStroll.stroll(0.5F)),
				Pair.of(1, (BehaviorControl) SetEntityLookTarget.create(8.0F)),
				Pair.of(2, (BehaviorControl) new DoNothing(30, 60))
		);
	}

	private GirlfriendCommonAiPackages() {
	}
}
