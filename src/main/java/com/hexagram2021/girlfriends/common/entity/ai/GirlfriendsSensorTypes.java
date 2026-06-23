package com.hexagram2021.girlfriends.common.entity.ai;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.entity.ai.sensor.BeehiveSensor;
import com.hexagram2021.girlfriends.common.entity.ai.sensor.FlowerSensor;
import com.hexagram2021.girlfriends.common.entity.ai.sensor.GirlfriendShelterSensor;
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

	private GirlfriendsSensorTypes() {
	}
}
