package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 角色采集附近矿石行为 — 梅疏专属喵~
 * <p>
 * 检测到附近矿石后导航过去并破坏，无视数量喵~
 *
 * @author liudongyu
 */
public final class MineNearbyOre {
	/**
	 * 创建角色采集附近矿石行为喵~
	 *
	 * @param workDistance 工作距离喵~
	 * @return 行为喵~
	 */
	public static OneShot<GirlfriendEntity> create(double workDistance) {
		return BehaviorBuilder.create(i -> i.group(i.present(GirlfriendsMemoryTypes.NEARBY_ORE.get())).apply(i, orePos -> (level, entity, _) -> {
			BlockPos oreBlockPos = i.get(orePos);
			if(entity.position().closerThan(oreBlockPos.getBottomCenter(), workDistance) && EventHooks.canEntityGrief(level, entity)) {
				BlockState oreBlockState = level.getBlockState(oreBlockPos);
				if(oreBlockState.is(Tags.Blocks.ORES)) {
					// 只要发现矿物就采集喵~
					level.destroyBlock(oreBlockPos, true, entity);
					orePos.erase();
					return true;
				}
			}
			return false;
		}));
	}

	private MineNearbyOre() {
	}
}
