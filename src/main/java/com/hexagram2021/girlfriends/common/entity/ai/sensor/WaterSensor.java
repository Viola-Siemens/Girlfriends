package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

/**
 * 水域传感器 — 检测 12 格内水域方块，写入最近水域位置喵~
 *
 * @author liudongyu
 */
public class WaterSensor extends GirlfriendBlockSensor {
	/**
	 * 构造水域传感器
	 */
	public WaterSensor() {
		super(GirlfriendsMemoryTypes.NEAREST_WATER.get(), 12);
	}

	@Override
	protected boolean attractsGirlfriend(BlockState blockState) {
		FluidState fluidState = blockState.getFluidState();
		return !fluidState.isEmpty() && fluidState.is(FluidTags.WATER);
	}
}