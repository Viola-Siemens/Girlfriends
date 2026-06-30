package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * 矿石传感器 — 检测 8 格内矿石方块，写入最近矿石位置喵~
 *
 * @author liudongyu
 */
public class OreSensor extends GirlfriendBlockSensor {
	/**
	 * 构造矿石传感器
	 */
	public OreSensor() {
		super(GirlfriendsMemoryTypes.NEARBY_ORE.get(), 8);
	}

	@Override
	protected boolean attractsGirlfriend(BlockState blockState) {
		return blockState.is(Tags.Blocks.ORES);
	}
}
