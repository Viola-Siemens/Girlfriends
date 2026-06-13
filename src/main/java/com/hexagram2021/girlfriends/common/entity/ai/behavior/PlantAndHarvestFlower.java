package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.MomoEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * 角色种植和收获花行为 — 沫沫专属
 *
 * @author liudongyu
 */
public final class PlantAndHarvestFlower {
	/**
	 * 创建角色种植和收获花行为
	 *
	 * @param flowerCountThreshold 周围花朵数量阈值，低于它则使用骨粉催熟，否则收获
	 * @param workDistance 工作距离
	 * @return 行为
	 */
	public static OneShot<MomoEntity> create(int flowerCountThreshold, double workDistance) {
		return BehaviorBuilder.create(i -> i.group(i.present(GirlfriendsMemoryTypes.NEARBY_FLOWER.get()), i.present(GirlfriendsMemoryTypes.NEARBY_FLOWER_COUNT.get())).apply(i, (flowerPos, flowerCount) -> (level, entity, _) -> {
			BlockPos flowerBlockPos = i.get(flowerPos);
			if(entity.position().closerThan(flowerBlockPos.getBottomCenter(), workDistance) && EventHooks.canEntityGrief(level, entity)) {
				BlockState flowerBlockState = level.getBlockState(flowerBlockPos);
				if(flowerBlockState.is(Tags.Blocks.FLOWERS)) {
					if (i.get(flowerCount) >= flowerCountThreshold) {
						// 花朵充足，采集
						level.destroyBlock(flowerBlockPos, true, entity);
						flowerPos.erase();
					} else if(entity.updateBoneMealTick()) {
						prepareBoneMeal(entity);
						// 催熟
						ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
						if(mainHandItem.isEmpty() || !mainHandItem.is(Items.BONE_MEAL)) {
							return false;
						}
						BonemealableBlock bonemealableBlock = getBonemealableBlock(level, flowerBlockState, flowerBlockPos);
						if(bonemealableBlock == null) {
							return false;
						}
						bonemealableBlock.performBonemeal(level, level.getRandom(), flowerBlockPos, flowerBlockState);
						mainHandItem.shrink(1);
						entity.swing(InteractionHand.MAIN_HAND);
					}
					return true;
				}
			}
			return false;
		}));
	}

	@Nullable
	private static BonemealableBlock getBonemealableBlock(ServerLevel level, BlockState flowerBlockState, BlockPos flowerBlockPos) {
		BonemealableBlock ret = null;
		if(flowerBlockState.getBlock() instanceof BonemealableBlock bonemealableBlock) {
			// 花朵可以直接催熟
			ret = bonemealableBlock;
		} else {
			// 催熟下方的方块，间接获得花朵
			BlockPos bottom = flowerBlockPos.offset(level.getRandom().nextInt(3) - 1, -1, level.getRandom().nextInt(3) - 1);
			if (level.getBlockState(bottom).getBlock() instanceof BonemealableBlock bonemealableBlock) {
				ret = bonemealableBlock;
			}
		}
		return ret;
	}

	private static void prepareBoneMeal(MomoEntity entity) {
		// 花朵不足，准备骨粉
		SimpleContainer inventory = entity.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
			if (inventory.getItem(slot).is(Items.BONE_MEAL)) {
				ItemStack boneMealStack = inventory.removeItemNoUpdate(slot);
				if(!boneMealStack.isEmpty()) {
					// 骨粉与主手物品互换
					inventory.addItem(entity.getItemInHand(InteractionHand.MAIN_HAND));
					entity.setItemInHand(InteractionHand.MAIN_HAND, boneMealStack);
					break;
				}
			}
		}
	}

	private PlantAndHarvestFlower() {
	}
}
