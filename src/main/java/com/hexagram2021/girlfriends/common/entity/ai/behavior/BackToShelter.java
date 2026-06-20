package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Optional;
import java.util.Set;

/**
 * 角色返回避难所行为
 *
 * @author liudongyu
 */
public final class BackToShelter {
	/**
	 * 创建角色返回避难所行为
	 * @param closeEnoughDist 不需要移动的最远距离
	 * @param tooFarDist 传送的最近距离
	 * @param speedModifier 速度修饰
	 * @return 行为
	 */
	public static OneShot<GirlfriendEntity> create(int closeEnoughDist, int tooFarDist, float speedModifier) {
		return BehaviorBuilder.create(i -> i.group(i.present(GirlfriendsMemoryTypes.SHELTER_POINT.get()), i.registered(GirlfriendsMemoryTypes.HOME_BED_POINT.get()), i.absent(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET)).apply(i, (location, bed, _, _, _) -> (_, body, _) -> {
			if(body.getFollowMode().isStayOrFollow()) {
				// 跟随玩家，不回家
				return false;
			}
			GlobalPos shelterGlobalPosition;
			if(body.getFollowMode() == FollowMode.HOME) {
				// 不寻找自己家，而是找玩家的家园
				Optional<GlobalPos> bedGlobalPosition = i.tryGet(bed);
				if(bedGlobalPosition.isEmpty()) {
					return false;
				}
				shelterGlobalPosition = bedGlobalPosition.get();
			} else {
				shelterGlobalPosition = i.get(location);
			}
			BlockPos shelterPosition = shelterGlobalPosition.pos();
			if((!shelterGlobalPosition.dimension().equals(body.level().dimension()) || !shelterPosition.closerThan(body.blockPosition(), tooFarDist)) &&
					body.level() instanceof ServerLevel serverLevel) {
				// 太远，直接传送
				ServerLevel targetLevel = serverLevel.getServer().getLevel(shelterGlobalPosition.dimension());
				if(targetLevel == null) {
					return false;
				}
				body.teleportTo(targetLevel, shelterPosition.getX(), shelterPosition.getY(), shelterPosition.getZ(), Set.of(), body.getYRot(), body.getXRot(), false);
				return true;
			}
			if (!shelterPosition.closerThan(body.blockPosition(), closeEnoughDist)) {
				// 走回家
				BehaviorUtils.setWalkAndLookTargetMemories(body, shelterPosition, speedModifier, closeEnoughDist);
				return true;
			}

			// 太近，不需要移动
			return false;
		}));
	}

	private BackToShelter() {
	}
}
