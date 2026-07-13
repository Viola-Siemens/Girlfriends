package com.hexagram2021.girlfriends.common.event;

import com.hexagram2021.girlfriends.common.blessing.GirlfriendsMobEffects;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.item.GirlfriendsItems;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.FixedQuestDefinitionManager;
import com.hexagram2021.girlfriends.common.quest.QuestService;
import com.hexagram2021.girlfriends.common.quest.RandomQuestTemplateManager;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/**
 * 角色实体事件处理器喵~
 * <p>
 * 负责实体生命周期同步和每日维护（委托刷新、每日重置）喵~
 *
 * @author liudongyu
 */
public final class GirlfriendGameEvents {
	private long lastProcessedDay = -1L;

	/**
	 * 实体加入世界事件喵~
	 * <p>
	 * 将实体 UUID 写入 CharacterWorldState，建立实体与持久化状态的关联喵~
	 *
	 * @param event 实体加入世界事件喵~
	 */
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		Entity entity = event.getEntity();
		if (entity instanceof GirlfriendEntity girlfriend) {
			GirlfriendsWorldData data = getWorldData((ServerLevel)event.getLevel());
			Identifier girlfriendTypeId = girlfriend.getGirlfriendTypeId();
			girlfriend.syncToWorldState(data);
			data.getCharacterState(girlfriendTypeId).setAlive(true);
			data.setDirty();
		}
	}

	/**
	 * 服务端 Tick 事件喵~
	 * <p>
	 * 每日维护：重置每日计数器、刷新/过期随机委托喵~
	 *
	 * @param event 服务端 Tick 事件喵~
	 */
	@SubscribeEvent
	public void onServerTick(ServerTickEvent.Post event) {
		ServerLevel overworld = event.getServer().overworld();
		GirlfriendsWorldData data = getWorldData(overworld);
		long gameDay = overworld.getGameTime() / 24000L;
		if (gameDay == this.lastProcessedDay) {
			return;
		}
		this.lastProcessedDay = gameDay;

		RelationshipService relationshipService = new RelationshipService(data);
		relationshipService.resetDailyCounters(gameDay);

		QuestService questService = new QuestService(
				data,
				relationshipService,
				id -> FixedQuestDefinitionManager.INSTANCE.getDefinition(id).orElse(null),
				id -> RandomQuestTemplateManager.INSTANCE.getRandomDefinitionForType(id, overworld.getRandom()),
				randomSource -> 5 + randomSource.nextInt(6)
		);

		for (Map.Entry<Identifier, CharacterWorldState> entry : data.getCharacters().entrySet()) {
			Identifier girlfriendTypeId = entry.getKey();
			CharacterWorldState state = entry.getValue();
			if (state.getEntityUuid() == null || !state.isAlive()) {
				continue;
			}
			// 过期随机委托
			questService.expireRandomQuest(girlfriendTypeId, gameDay);
			// 如果没有委托，尝试刷新随机委托
			if (state.getCurrentQuest() == null) {
				questService.refreshRandomQuest(girlfriendTypeId, gameDay, overworld.getRandom());
			}
		}
	}

	/**
	 * 实体死亡事件喵~
	 * <p>
	 * 标记角色为非存活状态，清理委托槽（终章奖励保留）喵~
	 *
	 * @param event 实体死亡事件喵~
	 */
	@SubscribeEvent
	public void onEntityDie(LivingDeathEvent event) {
		if (event.getEntity().level().isClientSide()) {
			return;
		}
		Entity entity = event.getEntity();
		if (entity instanceof GirlfriendEntity girlfriend) {
			ServerLevel level = (ServerLevel)girlfriend.level();
			GirlfriendsWorldData data = getWorldData(level);
			Identifier girlfriendTypeId = girlfriend.getGirlfriendTypeId();
			data.updateCharacter(girlfriendTypeId, state -> {
				state.setAlive(false);
				state.setPendingRespawn(true);
				state.setDeathPos(GlobalPos.of(level.dimension(), entity.blockPosition()));
			});
		}
	}

	/**
	 * 右键方块事件喵~
	 * @param event 右键方块事件喵~
	 */
	@SubscribeEvent
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if(event.getLevel().getBlockState(event.getHitVec().getBlockPos()).getBlock() instanceof FlowerPotBlock &&
				event.getItemStack().is(GirlfriendsItems.WATERING_CAN)) {
			event.setUseBlock(TriState.FALSE);
		}
	}

	/**
	 * 受击事件
	 *
	 * @param event 受击事件
	 */
	@SubscribeEvent
	public void onLivingDamagePre(LivingDamageEvent.Pre event) {
		LivingEntity livingEntity = event.getEntity();
		if(livingEntity.hasEffect(GirlfriendsMobEffects.FLAME_GUARDIAN)) {
			event.setNewDamage(event.getNewDamage() * 0.75F);
		}
	}

	/**
	 * 挖掘方块掉落事件
	 *
	 * @param event 挖掘方块掉落事件
	 */
	@SubscribeEvent
	public void onBlockDrop(BlockDropsEvent event) {
		if(event.getBreaker() instanceof LivingEntity livingEntity && livingEntity.hasEffect(GirlfriendsMobEffects.BOUNTY_OF_EARTH)) {
			event.getDrops().forEach(drop -> {
				// 50% 几率不变，50% 几率将原来的 n 个物品增加到 [n, 2n]，区间内概率均等。
				// 最终期望为 1.25n
				if(livingEntity.getRandom().nextBoolean()) {
					ItemStack itemStack = drop.getItem();
					itemStack.setCount(itemStack.getCount() + livingEntity.getRandom().nextInt(itemStack.getCount() + 1));
				}
			});
		}
	}

	/**
	 * 钓鱼事件
	 *
	 * @param event 钓鱼事件
	 */
	@SubscribeEvent
	public void onItemFished(ItemFishedEvent event) {
		Player player = event.getEntity();
		if(player.hasEffect(GirlfriendsMobEffects.TIDE_COMPANION)) {
			event.getDrops().forEach(drop -> {
				// 50% 几率不变，50% 几率将原来的 n 个物品增加到 [n, 2n]，区间内概率均等。
				// 最终期望为 1.25n
				if(player.getRandom().nextBoolean()) {
					drop.setCount(drop.getCount() + player.getRandom().nextInt(drop.getCount() + 1));
				}
			});
		}
	}

	/**
	 * 获取世界数据喵~
	 *
	 * @param level 服务端世界喵~
	 * @return 世界数据喵~
	 */
	private static GirlfriendsWorldData getWorldData(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(GirlfriendsWorldData.TYPE);
	}
}
