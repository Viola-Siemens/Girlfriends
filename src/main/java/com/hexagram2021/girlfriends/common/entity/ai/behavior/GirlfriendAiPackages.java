package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;

import java.util.List;

/**
 * 角色 AI 行为包工具类喵~
 * <p>
 * 为 Brain 提供可复用的基础行为编排喵~
 * 日程更新与角色专属行为在对应实体子类的 makeBrain() 中注册喵~
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
	 * @return 核心行为列表喵~
	 */
	public static List<BehaviorControl<GirlfriendEntity>> coreActivities() {
		return ImmutableList.of(
				(BehaviorControl) new Swim(0.8f)
		);
	}

	/**
	 * 默认闲置行为包 — 闲逛、注视、休息喵~
	 *
	 * @return 闲置行为列表喵~
	 */
	public static List<BehaviorControl<GirlfriendEntity>> idleActivities() {
		return ImmutableList.of(
				(BehaviorControl) RandomStroll.stroll(0.5f),
				(BehaviorControl) new SetEntityLookTarget(),
				(BehaviorControl) new DoNothing(30, 60)
		);
	}
}
