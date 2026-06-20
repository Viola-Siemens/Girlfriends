package com.hexagram2021.girlfriends.common.item;

import com.hexagram2021.girlfriends.common.components.GirlfriendsDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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
		ItemStack itemStack = context.getItemInHand();
		Player player = context.getPlayer();
		int waterLevel = itemStack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0);
		if(waterLevel > 0) {
			if(state.hasProperty(BlockStateProperties.MOISTURE)) {
				int moisture = state.getValue(BlockStateProperties.MOISTURE);
				int max = BlockStateProperties.MOISTURE.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
				if (moisture < max) {
					if (level.isClientSide()) {
						addParticles(level, pos, level.getRandom(), pos.getY() + 0.9D);
						return InteractionResult.SUCCESS;
					}
					level.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, max), Block.UPDATE_ALL);
					itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, waterLevel - 1);
					if(player != null) {
						player.getCooldowns().addCooldown(itemStack, 20);
					}
					return InteractionResult.CONSUME;
				}
				return super.useOn(context);
			}
			if(state.getBlock() instanceof FlowerPotBlock potBlock && potBlock.getPotted() != Blocks.AIR) {
				if (level.isClientSide()) {
					addParticles(level, pos, level.getRandom(), pos.getY() + 0.5D);
					return InteractionResult.SUCCESS;
				}
				if(level.getRandom().nextInt(6) == 0) {
					Block.popResource(level, pos, new ItemStack(potBlock.getPotted()));
				}
				itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, waterLevel - 1);
				if(player != null) {
					player.getCooldowns().addCooldown(itemStack, 20);
				}
				return InteractionResult.CONSUME;
			}
		}
		if(state.is(Blocks.WATER_CAULDRON) && waterLevel + 3 <= MAX_WATER_LEVEL) {
			if(level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			int cauldronLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
			level.playSound(player, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			if(cauldronLevel > 1) {
				level.setBlock(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, cauldronLevel - 1), Block.UPDATE_ALL);
			} else {
				level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
			}
			itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, waterLevel + 3);
			return InteractionResult.CONSUME;
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hitResult.getType() != HitResult.Type.MISS && hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hitResult.getBlockPos();
			if (!level.mayInteract(player, pos)) {
				return InteractionResult.PASS;
			}

			if (level.getFluidState(pos).is(FluidTags.WATER)) {
				level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
				itemStack.set(GirlfriendsDataComponentTypes.WATER_LEVEL, MAX_WATER_LEVEL);
				return InteractionResult.SUCCESS;
			}
		}

		return super.use(level, player, hand);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.clamp(stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) * 2L - 1L, 1, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return stack.getOrDefault(GirlfriendsDataComponentTypes.WATER_LEVEL, 0) >= MAX_WATER_LEVEL ?
				FULL_BAR_COLOR : BAR_COLOR;
	}

	private static void addParticles(Level level, BlockPos pos, RandomSource random, double minY) {
		for(int i = 0; i < 3; ++i) {
			double x = pos.getX() + random.nextDouble() * 0.6D + 0.2D;
			double z = pos.getZ() + random.nextDouble() * 0.6D + 0.2D;
			level.addParticle(
					ParticleTypes.DRIPPING_WATER,
					x, minY + random.nextDouble() * 0.1D, z,
					0.0D, -0.02D, 0.0D
			);
		}
	}
}
