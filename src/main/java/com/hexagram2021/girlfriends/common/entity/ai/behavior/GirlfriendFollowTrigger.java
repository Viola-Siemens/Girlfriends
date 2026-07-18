package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Map;

/**
 * 跟随触发器
 *
 * @author liudongyu
 */
public class GirlfriendFollowTrigger extends Behavior<GirlfriendEntity> {
	/**
	 * 创建跟随触发器
	 */
	public GirlfriendFollowTrigger() {
		super(Map.of());
	}

	@Override
	protected boolean canStillUse(ServerLevel level, GirlfriendEntity body, long timestamp) {
		return body.getFollowMode().isStayOrFollow();
	}

	@Override
	protected void start(ServerLevel level, GirlfriendEntity body, long timestamp) {
		if (body.getFollowMode().isStayOrFollow()) {
			Brain<?> brain = body.getBrain();
			if (!brain.isActive(GirlfriendsActivities.FOLLOW.get())) {
				brain.eraseMemory(MemoryModuleType.PATH);
				brain.eraseMemory(MemoryModuleType.WALK_TARGET);
				brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
				brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
			}

			brain.setActiveActivityIfPossible(GirlfriendsActivities.FOLLOW.get());
		}
	}

	@Override
	protected void tick(ServerLevel level, GirlfriendEntity body, long timestamp) {
		// do nothing
	}
}
