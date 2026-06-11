package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * 角色返回避难所行为
 *
 * @author liudongyu
 */
public final class BackToShelter {
	/**
	 * 创建角色返回避难所行为
	 * @param closeEnoughDist 最近距离
	 * @param speedModifier 速度修饰
	 * @return 行为
	 */
	public static OneShot<GirlfriendEntity> create(int closeEnoughDist, float speedModifier) {
		return BehaviorBuilder.create(i -> i.group(i.present(GirlfriendsMemoryTypes.SHELTER_POINT.get()), i.absent(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET)).apply(i, (location, _, _, _) -> (_, body, _) -> {
			BlockPos shelterPosition = i.get(location).pos();
			boolean closeEnoughToTarget = shelterPosition.closerThan(body.blockPosition(), closeEnoughDist);
			if (!closeEnoughToTarget) {
				BehaviorUtils.setWalkAndLookTargetMemories(body, shelterPosition, speedModifier, closeEnoughDist);
			}

			return true;
		}));
	}

	private BackToShelter() {
	}
}
