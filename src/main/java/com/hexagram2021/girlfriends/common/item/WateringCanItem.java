package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.common.components.GirlfriendsDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.Tags;

/**
 * 洒水壶物品
 *
 * @author liudongyu
 */
public class WateringCanItem extends Item {
	public static final int MAX_WATER_LEVEL = 7;

	private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);
	private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);

	/**
	 * 构造洒水壶物品
	 * @param properties 物品属性
	 */
	public WateringCanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		FluidState fluid = level.getFluidState(pos);
		ItemStack itemStack = context.getItemInHand();
		int waterLevel = itemStack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0);
		if(waterLevel > 0) {
			if (state.hasProperty(BlockStateProperties.MOISTURE)) {
				int moisture = state.getValue(BlockStateProperties.MOISTURE);
				int max = BlockStateProperties.MOISTURE.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
				if (moisture < max) {
					level.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, max), Block.UPDATE_ALL);
					itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, waterLevel - 1);
				}
			}
		} else {
			// TODO what will happen if it's empty?
		}
		if(state.is(Blocks.WATER_CAULDRON) && waterLevel + 3 <= MAX_WATER_LEVEL) {
			int cauldronLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
			if(cauldronLevel > 0) {
				level.setBlock(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, cauldronLevel - 1), Block.UPDATE_ALL);
				itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, waterLevel + 3);
			}
		} else if(fluid.is(Tags.Fluids.WATER)) {
			itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, MAX_WATER_LEVEL);
		}
		return super.useOn(context);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.clamp(stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) * 2 - 1, 1, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) >= MAX_WATER_LEVEL ?
				FULL_BAR_COLOR : BAR_COLOR;
	}
}
