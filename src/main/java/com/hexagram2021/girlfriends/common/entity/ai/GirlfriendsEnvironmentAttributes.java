package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 环境属性注册表喵~
 *
 * @author liudongyu
 */
public class GirlfriendsEnvironmentAttributes {
	public static final DeferredRegister<EnvironmentAttribute<?>> REGISTER =
			DeferredRegister.create(Registries.ENVIRONMENT_ATTRIBUTE, GirlfriendsMod.MODID);

	/** 沫沫日程喵~ */
	public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Activity>> MOMO_ACTIVITY =
			REGISTER.register(
					"gameplay/momo_activity",
					() -> EnvironmentAttribute.builder(AttributeTypes.ACTIVITY)
							.defaultValue(GirlfriendsActivities.MORNING.get())
							.build()
			);

	/** 渔溪日程喵~ */
	public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Activity>> YUXI_ACTIVITY =
			REGISTER.register(
					"gameplay/yuxi_activity",
					() -> EnvironmentAttribute.builder(AttributeTypes.ACTIVITY)
							.defaultValue(GirlfriendsActivities.MORNING.get())
							.build()
			);

	/** 梅疏日程喵~ */
	public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Activity>> MEISHU_ACTIVITY =
			REGISTER.register(
					"gameplay/meishu_activity",
					() -> EnvironmentAttribute.builder(AttributeTypes.ACTIVITY)
							.defaultValue(GirlfriendsActivities.MORNING.get())
							.build()
			);

	/** 晚萤日程喵~ */
	public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Activity>> WANYING_ACTIVITY =
			REGISTER.register(
					"gameplay/wanying_activity",
					() -> EnvironmentAttribute.builder(AttributeTypes.ACTIVITY)
							.defaultValue(GirlfriendsActivities.MORNING.get())
							.build()
			);

	/** 幽若日程喵~ */
	public static final DeferredHolder<EnvironmentAttribute<?>, EnvironmentAttribute<Activity>> YOURUO_ACTIVITY =
			REGISTER.register(
					"gameplay/youruo_activity",
					() -> EnvironmentAttribute.builder(AttributeTypes.ACTIVITY)
							.defaultValue(GirlfriendsActivities.MORNING.get())
							.build()
			);

	private GirlfriendsEnvironmentAttributes() {
	}
}
