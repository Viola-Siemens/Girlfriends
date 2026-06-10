package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

/**
 * 跟随亲密的玩家，仅当玩家要求女友跟随时生效
 *
 * @author liudongyu
 */
public final class StayCloseToIntimatePlayer {
	/**
	 * 创建女友跟随行为工厂函数
	 * @param closeEnough 最近距离，停止跟随
	 * @param tooFar 最远距离，放弃跟随
	 * @param speedModifier 速度
	 * @return 跟随行为
	 */
	public static BehaviorControl<GirlfriendEntity> create(int closeEnough, int tooFar, float speedModifier) {
		return BehaviorBuilder.create(instance -> instance.group(
				instance.registered(MemoryModuleType.LOOK_TARGET), instance.registered(MemoryModuleType.WALK_TARGET)
		).apply(instance, (lookTarget, walkTarget) -> (level, body, timestamp) -> {
			if(body.getFollowMode() != FollowMode.FOLLOW) {
				return false;
			}
			Player followed = body.getFollowedPlayer();
			if(followed == null) {
				return false;
			}
			PositionTracker positionTracker = new EntityTracker(followed, true, true);
			if (body.position().closerThan(positionTracker.currentPosition(), tooFar)) {
				return false;
			} else {
				lookTarget.set(positionTracker);
				walkTarget.set(new WalkTarget(positionTracker, speedModifier, closeEnough));
				return true;
			}
		}));
	}

	private StayCloseToIntimatePlayer() {
	}
}
