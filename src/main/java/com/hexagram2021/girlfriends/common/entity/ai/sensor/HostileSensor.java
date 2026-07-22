package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntityTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Set;

/**
 * 敌对生物传感器 — 晚萤专用<br/>
 * 扫描 32 格内敌对生物，写入最近敌对生物喵~
 *
 * @author liudongyu
 */
public class HostileSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RANGE = 32;
	private static final int ATTACK_RANGE = 10;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.ATTACK_TARGET);
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		AABB scanArea = entity.getBoundingBox().inflate(SCAN_RANGE);
		level.getEntitiesOfClass(Monster.class, scanArea, LivingEntity::isAlive)
				.stream()
				.filter(hostile -> hostile.is(GirlfriendEntityTags.WANYING_ATTACKS))
				.min(Comparator.comparingDouble(entity::distanceToSqr))
				.ifPresentOrElse(
						hostile -> {
							entity.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, hostile);
							if(entity.distanceToSqr(entity) < ATTACK_RANGE * ATTACK_RANGE) {
								entity.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, hostile);
							}
						},
						() -> entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_HOSTILE)
				);
	}
}
