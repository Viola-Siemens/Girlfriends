package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Set;

/**
 * 敌对生物传感器 — 扫描 16 格内敌对生物，写入最近敌对生物喵~
 *
 * @author liudongyu
 */
public class HostileSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RANGE = 16;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(MemoryModuleType.NEAREST_HOSTILE);
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		AABB scanArea = entity.getBoundingBox().inflate(SCAN_RANGE);
		level.getEntitiesOfClass(Monster.class, scanArea, LivingEntity::isAlive)
				.stream()
				.min(Comparator.comparingDouble(entity::distanceToSqr))
				.ifPresentOrElse(
						hostile -> entity.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, hostile),
						() -> entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_HOSTILE)
				);
	}
}
