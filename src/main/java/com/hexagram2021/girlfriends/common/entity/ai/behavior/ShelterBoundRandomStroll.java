package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 在家附近的随机移动行为
 *
 * @author liudongyu
 */
public final class ShelterBoundRandomStroll {
	/**
	 * 创建在家附近的随机移动行为
	 *
	 * @param speedModifier 速度修饰
	 * @return 随机移动行为
	 */
	public static OneShot<GirlfriendEntity> create(float speedModifier) {
		return create(speedModifier, 12, 8, 48);
	}

	/**
	 * 创建在家附近的随机移动行为
	 *
	 * @param speedModifier 速度修饰
	 * @param maxXzDist     XZ 轴最大距离
	 * @param maxYDist      Y 轴最大距离
	 * @param tooFarDist    放弃距离——转为瞬移
	 * @return 随机移动行为
	 */
	public static OneShot<GirlfriendEntity> create(float speedModifier, int maxXzDist, int maxYDist, int tooFarDist) {
		return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET), i.present(GirlfriendsMemoryTypes.SHELTER_POINT.get()), i.registered(GirlfriendsMemoryTypes.HOME_BED_POINT.get())).apply(i, (walkTarget, shelterLocation, homeBedLocation) -> (level, body, _) -> {
			BlockPos bodyPos = body.blockPosition();
			SectionPos sectionPos = SectionPos.of(bodyPos);
			SectionPos optimalSectionPos = SectionPos.of(
					body.getFollowMode() == FollowMode.HOME ?
							i.tryGet(homeBedLocation).map(GlobalPos::pos).orElse(bodyPos) :
							i.get(shelterLocation).pos()
			);
			Vec3 landPos = optimalSectionPos != sectionPos ?
					DefaultRandomPos.getPosTowards(
							body, maxXzDist, maxYDist,
							Vec3.atBottomCenterOf(optimalSectionPos.center()),
							Math.PI / 2.0D
					) : LandRandomPos.getPos(body, maxXzDist, maxYDist);

			if(landPos != null && !landPos.closerThan(bodyPos.getBottomCenter(), tooFarDist)) {
				if(body.getFollowMode().isStayOrFollow()) {
					landPos = LandRandomPos.getPos(body, maxXzDist, maxYDist);
				} else {
					return false;
				}
			}

			walkTarget.setOrErase(Optional.ofNullable(landPos).map(pos -> new WalkTarget(pos, speedModifier, 0)));
			return true;
		}));
	}

	private ShelterBoundRandomStroll() {
	}
}
