package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Map;

/**
 * 恐慌触发器
 *
 * @author liudongyu
 */
public class GirlfriendPanicTrigger extends Behavior<GirlfriendEntity> {
	/**
	 * 创建恐慌触发器
	 */
	public GirlfriendPanicTrigger() {
		super(Map.of());
	}

	@Override
	protected boolean canStillUse(ServerLevel level, GirlfriendEntity body, long timestamp) {
		return isHurt(body) || hasHostile(body);
	}

	@Override
	protected void start(ServerLevel level, GirlfriendEntity body, long timestamp) {
		if (isHurt(body) || hasHostile(body)) {
			Brain<?> brain = body.getBrain();
			if (!brain.isActive(Activity.PANIC)) {
				brain.eraseMemory(MemoryModuleType.PATH);
				brain.eraseMemory(MemoryModuleType.WALK_TARGET);
				brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
				brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
			}

			brain.setActiveActivityIfPossible(Activity.PANIC);
		}
	}

	@Override
	protected void tick(ServerLevel level, GirlfriendEntity body, long timestamp) {
		// do nothing
	}

	/**
	 * 检测是否有敌队实体
	 *
	 * @param myBody 实体
	 * @return 是否有 hostile
	 */
	public static boolean hasHostile(LivingEntity myBody) {
		return myBody.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
	}

	/**
	 * 检测是否受伤
	 *
	 * @param myBody 实体
	 * @return 是否受伤
	 */
	public static boolean isHurt(LivingEntity myBody) {
		return myBody.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
	}
}
