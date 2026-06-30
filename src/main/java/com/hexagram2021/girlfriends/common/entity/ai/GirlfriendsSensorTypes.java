package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.ai.sensor.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 角色 AI 传感器类型注册表喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsSensorTypes {
	public static final DeferredRegister<SensorType<?>> REGISTER =
			DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, GirlfriendsMod.MODID);

	/** 庇护所位置传感器喵~ */
	public static final DeferredHolder<SensorType<?>, SensorType<GirlfriendShelterSensor>> SHELTER_SENSOR =
			REGISTER.register("shelter_sensor", () -> new SensorType<>(GirlfriendShelterSensor::new));

	/** 花朵位置传感器 */
	public static final DeferredHolder<SensorType<?>, SensorType<FlowerSensor>> FLOWER_SENSOR =
			REGISTER.register("flower_sensor", () -> new SensorType<>(FlowerSensor::new));

	/** 蜂箱位置传感器 */
	public static final DeferredHolder<SensorType<?>, SensorType<BeehiveSensor>> BEEHIVE_SENSOR =
			REGISTER.register("beehive_sensor", () -> new SensorType<>(BeehiveSensor::new));

	/** 水域位置传感器 — 渔溪专属 */
	public static final DeferredHolder<SensorType<?>, SensorType<WaterSensor>> WATER_SENSOR =
			REGISTER.register("water_sensor", () -> new SensorType<>(WaterSensor::new));

	/** 矿石位置传感器 — 梅疏专属 */
	public static final DeferredHolder<SensorType<?>, SensorType<OreSensor>> ORE_SENSOR =
			REGISTER.register("ore_sensor", () -> new SensorType<>(OreSensor::new));

	/** 敌对生物传感器 — 晚萤专属 */
	public static final DeferredHolder<SensorType<?>, SensorType<HostileSensor>> HOSTILE_SENSOR =
			REGISTER.register("hostile_sensor", () -> new SensorType<>(HostileSensor::new));

	private GirlfriendsSensorTypes() {
	}
}
