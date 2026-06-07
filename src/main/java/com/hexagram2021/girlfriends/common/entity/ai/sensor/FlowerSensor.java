package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 花朵传感器 — 检测 8 格内所有花朵方块喵~
 *
 * @author liudongyu
 */
public class FlowerSensor extends Sensor<GirlfriendEntity> {
	private static final int SCAN_RANGE = 8;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(GirlfriendsMemoryTypes.NEARBY_FLOWERS.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		BlockPos entityPos = entity.blockPosition();
		List<BlockPos> flowers = new ArrayList<>();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++) {
			for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++) {
				for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
					mutable.set(entityPos.getX() + dx, entityPos.getY() + dy, entityPos.getZ() + dz);
					Block block = level.getBlockState(mutable).getBlock();
					if (block instanceof FlowerBlock || block == Blocks.FLOWERING_AZALEA ||
							block == Blocks.FLOWERING_AZALEA_LEAVES || block == Blocks.SPORE_BLOSSOM) {
						flowers.add(mutable.immutable());
					}
				}
			}
		}
		entity.getBrain().setMemory(GirlfriendsMemoryTypes.NEARBY_FLOWERS.get(), flowers);
	}
}
