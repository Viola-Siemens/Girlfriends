package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

/**
 * 短距瞬移行为 — 幽若专属喵~
 * <p>
 * 每约 15 秒随机瞬移到 5 格范围内的安全位置，播放末影人传送粒子喵~
 *
 * @author liudongyu
 */
public final class ShortRangeTeleport {
	private static final int MAX_RANGE = 5;
	private static final int MAX_ATTEMPTS = 16;
	private static final int TELEPORT_CHANCE = 300; // 约每 15 秒触发一次喵~

	/**
	 * 创建短距瞬移行为喵~
	 *
	 * @return 瞬移行为喵~
	 */
	public static OneShot<GirlfriendEntity> create() {
		return BehaviorBuilder.create(i -> i.point((level, entity, timestamp) -> {
			if (entity.getRandom().nextInt(TELEPORT_CHANCE) != 0) {
				return false;
			}
			for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
				double dx = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * MAX_RANGE * 2;
				double dy = entity.getY() + (entity.getRandom().nextDouble() - 0.5D) * MAX_RANGE;
				double dz = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * MAX_RANGE * 2;
				// 播放传送粒子效果喵~
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(
							ParticleTypes.PORTAL,
							entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
							16, 0.25D, 0.25D, 0.25D, 0.1D
					);
				}
				if (entity.randomTeleport(dx, dy, dz, true)) {
					// 瞬移成功后再次播放粒子喵~
					if (level instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(
								ParticleTypes.PORTAL,
								entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
								16, 0.25D, 0.25D, 0.25D, 0.1D
						);
					}
					return true;
				}
			}
			return false;
		}));
	}

	private ShortRangeTeleport() {
	}
}
