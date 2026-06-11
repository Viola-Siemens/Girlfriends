package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;

/**
 * 方块传感器 — 检测周围所有方块喵~
 *
 * @author liudongyu
 */
public abstract class GirlfriendBlockSensor extends Sensor<GirlfriendEntity> {
	private final MemoryModuleType<BlockPos> nearbyBlock;
	private final int scanRange;
	private Long2LongMap unreachableCache = Long2LongMaps.EMPTY_MAP;

	protected GirlfriendBlockSensor(MemoryModuleType<BlockPos> nearbyBlock, int scanRange) {
		this.nearbyBlock = nearbyBlock;
		this.scanRange = scanRange;
	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(this.nearbyBlock);
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		if(entity.getBrain().hasMemoryValue(this.nearbyBlock)) {
			return;
		}
		Iterable<BlockPos> nearbyPositions = BlockPos.withinManhattan(entity.blockPosition(), this.scanRange, this.scanRange, this.scanRange);
		Long2LongOpenHashMap tempCache = new Long2LongOpenHashMap();

		for(BlockPos pos: nearbyPositions) {
			long unreachableUntilTime = this.unreachableCache.getOrDefault(pos.asLong(), Long.MIN_VALUE);
			if(entity.level().getGameTime() < unreachableUntilTime) {
				tempCache.put(pos.asLong(), unreachableUntilTime);
			} else if(this.attractsGirlfriend(entity.level().getBlockState(pos))) {
				Path path = entity.getNavigation().createPath(pos, 1);
				if (path != null && path.canReach()) {
					entity.getBrain().setMemoryWithExpiry(this.nearbyBlock, pos, 60L * SharedConstants.TICKS_PER_SECOND);
					tempCache.put(pos.asLong(), entity.level().getGameTime() + 90L * SharedConstants.TICKS_PER_SECOND);
					break;
				}
			}
		}
		this.unreachableCache = tempCache;
	}

	protected abstract boolean attractsGirlfriend(BlockState blockState);
}
