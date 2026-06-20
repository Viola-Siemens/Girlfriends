package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * 女友冷静行为
 *
 * @author liudongyu
 */
public final class GirlfriendCalmDown {
	/**
	 * 创建女友冷静行为工厂函数
	 * @return 女友冷静行为
	 */
	public static BehaviorControl<GirlfriendEntity> create() {
		return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.HURT_BY), i.registered(MemoryModuleType.HURT_BY_ENTITY), i.registered(MemoryModuleType.NEAREST_HOSTILE)).apply(i, (hurtBy, hurtByEntity, nearestHostile) -> (level, body, _) -> {
			boolean feelScared = i.tryGet(hurtBy).isPresent() || i.tryGet(nearestHostile).isPresent() || i.tryGet(hurtByEntity).filter(entity -> entity.distanceToSqr(body) <= 64.0F).isPresent();
			if (!feelScared) {
				hurtBy.erase();
				hurtByEntity.erase();
				body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
			}

			return true;
		}));
	}

	private GirlfriendCalmDown() {
	}
}
