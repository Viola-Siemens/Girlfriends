package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.Set;

/**
 * 花朵传感器 — 检测 8 格内所有花朵方块喵~
 *
 * @author liudongyu
 */
public class FlowerSensor extends GirlfriendBlockSensor {
	private int found = 0;

	/**
	 * 构造花朵传感器
	 */
	public FlowerSensor() {
		super(GirlfriendsMemoryTypes.NEARBY_FLOWER.get(), 8);
	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(this.nearbyBlock, GirlfriendsMemoryTypes.NEARBY_FLOWER_COUNT.get());
	}

	@Override
	protected boolean attractsGirlfriend(BlockState blockState) {
		return blockState.is(Tags.Blocks.FLOWERS);
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		this.found = 0;
		super.doTick(level, entity);
		entity.getBrain().setMemoryWithExpiry(GirlfriendsMemoryTypes.NEARBY_FLOWER_COUNT.get(), this.found, this.memoryTicksToLive);
	}

	@Override
	protected boolean addAndCheck(GirlfriendEntity entity, BlockPos pos, Long2LongOpenHashMap tempCache) {
		if(this.found == 0) {
			super.addAndCheck(entity, pos, tempCache);
		}
		this.found += 1;
		return false;
	}
}
