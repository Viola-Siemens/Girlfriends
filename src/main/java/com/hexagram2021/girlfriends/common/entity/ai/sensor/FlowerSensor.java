package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * 花朵传感器 — 检测 8 格内所有花朵方块喵~
 *
 * @author liudongyu
 */
public class FlowerSensor extends GirlfriendBlockSensor {
	/**
	 * 构造花朵传感器
	 */
	public FlowerSensor() {
		super(GirlfriendsMemoryTypes.NEARBY_FLOWERS.get(), 8);
	}

	@Override
	protected boolean attractsGirlfriend(BlockState blockState) {
		return blockState.is(Tags.Blocks.FLOWERS);
	}
}
