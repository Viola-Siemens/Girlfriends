package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.GirlfriendFishingHook;
import com.hexagram2021.girlfriends.common.entity.GirlfriendsEntities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 渔溪钓鱼行为喵~
 * <p>
 * 管理抛竿→等待→收竿→冷却的完整钓鱼生命周期喵~
 *
 * @author liudongyu
 */
public class FishNearbyWater extends Behavior<GirlfriendEntity> {
	private static final int MAX_FISHING_TIME = 1200;
	private static final int COOLDOWN_TICKS = 100;
	private static final double MAX_DISTANCE_TO_WATER = 8.0;

	private @Nullable GirlfriendFishingHook activeHook;
	private long nextCastAvailableTick;

	public FishNearbyWater() {
		super(Map.of(GirlfriendsMemoryTypes.NEAREST_WATER.get(), MemoryStatus.VALUE_PRESENT), MAX_FISHING_TIME);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, GirlfriendEntity entity) {
		// 冷却检查喵~
		if(level.getGameTime() < this.nextCastAvailableTick) {
			return false;
		}
		// 背包必须有钓竿喵~
		if(!this.hasFishingRod(entity)) {
			return false;
		}
		// 距水域不超过最大距离喵~
		return entity.getBrain().getMemory(GirlfriendsMemoryTypes.NEAREST_WATER.get())
				.filter(waterPos -> entity.position().closerThan(waterPos.getBottomCenter(), MAX_DISTANCE_TO_WATER))
				.isPresent();
	}

	@Override
	protected void start(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		// 从背包找出钓竿并装备到主手喵~
		this.equipFishingRod(entity);
		// 生成浮标实体喵~
		GirlfriendFishingHook hook = GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		hook.setupFishing(entity);
		level.addFreshEntity(hook);
		this.activeHook = hook;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		if(this.activeHook == null || !this.activeHook.isAlive()) {
			return false;
		}
		// 主手必须仍有钓竿喵~
		if(!entity.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return false;
		}
		return true;
	}

	@Override
	protected void tick(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		if(this.activeHook != null && this.activeHook.isBiting()) {
			// 鱼已上钩，收竿喵~
			this.activeHook.retrieve(entity.getMainHandItem());
			this.activeHook = null;
			this.nextCastAvailableTick = level.getGameTime() + COOLDOWN_TICKS;
		}
	}

	@Override
	protected void stop(ServerLevel level, GirlfriendEntity entity, long gameTime) {
		// 清理悬空的浮标喵~
		if(this.activeHook != null) {
			if(this.activeHook.isAlive()) {
				this.activeHook.discard();
			}
			this.activeHook = null;
		}
	}

	/**
	 * 检查背包是否含有钓竿喵~
	 */
	private boolean hasFishingRod(GirlfriendEntity entity) {
		SimpleContainer inventory = entity.getInventory();
		for(int slot = 0; slot < inventory.getContainerSize(); ++slot) {
			ItemStack stack = inventory.getItem(slot);
			if(!stack.isEmpty() && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 将钓竿装备到主手，原主手物品放回背包喵~
	 */
	private void equipFishingRod(GirlfriendEntity entity) {
		SimpleContainer inventory = entity.getInventory();
		// 如果主手已是钓竿则无需装备喵~
		if(entity.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return;
		}
		for(int slot = 0; slot < inventory.getContainerSize(); ++slot) {
			ItemStack stack = inventory.getItem(slot);
			if(!stack.isEmpty() && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
				ItemStack rodStack = inventory.removeItemNoUpdate(slot);
				if(!rodStack.isEmpty()) {
					// 原主手物品与钓竿互换喵~
					inventory.addItem(entity.getItemInHand(InteractionHand.MAIN_HAND));
					entity.setItemInHand(InteractionHand.MAIN_HAND, rodStack);
					break;
				}
			}
		}
	}
}
