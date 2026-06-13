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
	protected final MemoryModuleType<BlockPos> nearbyBlock;
	private final int scanRange;
	protected final long memoryTicksToLive;
	protected final long ticksToIgnore;
	private Long2LongMap unreachableCache = Long2LongMaps.EMPTY_MAP;

	/**
	 * 构造方块传感器
	 * @param nearbyBlock 方块位置记忆
	 * @param scanRange 扫描范围
	 */
	protected GirlfriendBlockSensor(MemoryModuleType<BlockPos> nearbyBlock, int scanRange) {
		this(nearbyBlock, scanRange, 10L * SharedConstants.TICKS_PER_SECOND, 30L * SharedConstants.TICKS_PER_SECOND);
	}

	/**
	 * 构造方块传感器
	 * @param nearbyBlock 方块位置记忆
	 * @param scanRange 扫描范围
	 * @param memoryTicksToLive 记忆时长
	 * @param ticksToIgnore 忽略已发现方块时长
	 */
	protected GirlfriendBlockSensor(MemoryModuleType<BlockPos> nearbyBlock, int scanRange, long memoryTicksToLive, long ticksToIgnore) {
		this.nearbyBlock = nearbyBlock;
		this.scanRange = scanRange;
		this.memoryTicksToLive = memoryTicksToLive;
		this.ticksToIgnore = ticksToIgnore;
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
				if (path != null && path.canReach() && this.addAndCheck(entity, pos, tempCache)) {
					break;
				}
			}
		}
		this.unreachableCache = tempCache;
	}

	/**
	 * 检测方块是否吸引角色
	 * @param blockState 方块状态
	 * @return 是否吸引角色
	 */
	protected abstract boolean attractsGirlfriend(BlockState blockState);

	/**
	 * 添加方块并检查是否需要跳出循环
	 * @param entity 实体
	 * @param pos 方块位置
	 * @param tempCache 缓存
	 * @return 是否需要跳出循环
	 */
	protected boolean addAndCheck(GirlfriendEntity entity, BlockPos pos, Long2LongOpenHashMap tempCache) {
		entity.getBrain().setMemoryWithExpiry(this.nearbyBlock, pos.immutable(), this.memoryTicksToLive);
		tempCache.put(pos.asLong(), entity.level().getGameTime() + this.ticksToIgnore);
		return true;
	}
}
