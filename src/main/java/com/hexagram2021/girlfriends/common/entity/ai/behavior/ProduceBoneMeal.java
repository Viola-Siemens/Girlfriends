package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.block.GirlfriendBlockTags;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * 生产骨粉行为 — 沫沫专属<br/>
 * 每天上午或下午工作时生产一个骨粉
 *
 * @author liudongyu
 */
public final class ProduceBoneMeal {
	/**
	 * 创建生产骨粉行为
	 * @return 行为
	 */
	public static OneShot<GirlfriendEntity> create() {
		return BehaviorBuilder.create(i -> i.group(i.registered(GirlfriendsMemoryTypes.PRODUCED_BONE_MEAL.get())).apply(i, producedBoneMeal -> (level, entity, _) -> {
			Optional<Boolean> optional = i.tryGet(producedBoneMeal);

			if(optional.isEmpty()) {
				// 清理周围杂草
				BlockPos.withinManhattan(entity.blockPosition(), 5, 3, 5).forEach(pos -> {
					if(level.getBlockState(pos).is(GirlfriendBlockTags.GRASS_TO_WEED)) {
						level.destroyBlock(pos, false, entity);
					}
				});
				// 放置骨粉，背包满则忽略
				entity.getInventory().addItem(new ItemStack(Items.BONE_MEAL));
				// 15 分钟 CD
				producedBoneMeal.setWithExpiry(true, 900L * SharedConstants.TICKS_PER_SECOND);
				return true;
			}
			return false;
		}));
	}

	private ProduceBoneMeal() {
	}
}
