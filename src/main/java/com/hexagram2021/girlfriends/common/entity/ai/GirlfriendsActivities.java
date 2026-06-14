package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 日程活动注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsActivities {
	public static final DeferredRegister<Activity> REGISTER =
			DeferredRegister.create(Registries.ACTIVITY, GirlfriendsMod.MODID);

	/** 清晨在庇护所附近轻度活动喵~ */
	public static final DeferredHolder<Activity, Activity> MORNING =
			REGISTER.register("morning", () -> new Activity(GirlfriendsMod.MODID + "$morning"));

	/** 核心工作行为喵~ */
	public static final DeferredHolder<Activity, Activity> DAY_WORK =
			REGISTER.register("day_work", () -> new Activity(GirlfriendsMod.MODID + "$day_work"));

	/** 下午次要活动喵~ */
	public static final DeferredHolder<Activity, Activity> AFTERNOON =
			REGISTER.register("afternoon", () -> new Activity(GirlfriendsMod.MODID + "$afternoon"));

	/** 傍晚收尾活动喵~ */
	public static final DeferredHolder<Activity, Activity> SUNSET =
			REGISTER.register("sunset", () -> new Activity(GirlfriendsMod.MODID + "$sunset"));

	/** 夜晚庇护所内休息喵~ */
	public static final DeferredHolder<Activity, Activity> NIGHT_REST =
			REGISTER.register("night_rest", () -> new Activity(GirlfriendsMod.MODID + "$night_rest"));

	/** 跟随喵~ */
	public static final DeferredHolder<Activity, Activity> FOLLOW =
			REGISTER.register("follow", () -> new Activity(GirlfriendsMod.MODID + "$follow"));

	private GirlfriendsActivities() {
	}
}
