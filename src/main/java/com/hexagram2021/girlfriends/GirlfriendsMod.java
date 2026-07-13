package com.hexagram2021.girlfriends;

import com.hexagram2021.girlfriends.common.block.GirlfriendsBlocks;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import com.hexagram2021.girlfriends.common.command.AffectionCommand;
import com.hexagram2021.girlfriends.common.components.GirlfriendsDataComponentTypes;
import com.hexagram2021.girlfriends.common.config.GirlfriendsCommonConfig;
import com.hexagram2021.girlfriends.common.creativemodetab.GirlfriendsCreativeModeTabs;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.GirlfriendsEntities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsEnvironmentAttributes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.event.GirlfriendEntityEvents;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceManager;
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import com.hexagram2021.girlfriends.common.item.GirlfriendsItems;
import com.hexagram2021.girlfriends.common.network.GirlfriendsNetwork;
import com.hexagram2021.girlfriends.common.quest.FixedQuestDefinitionManager;
import com.hexagram2021.girlfriends.common.quest.RandomQuestTemplateManager;
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceEvents;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;

/**
 * 模组主类喵~
 *
 * @author liudongyu
 */
@Mod(GirlfriendsMod.MODID)
public class GirlfriendsMod {
	public static final String MODID = "girlfriends";

	private static final Identifier GIFT_PREFERENCE_MANAGER_ID = Identifier.fromNamespaceAndPath(MODID, "gift_preferences");
	private static final Identifier FIXED_QUEST_DEFINITION_MANAGER_ID = Identifier.fromNamespaceAndPath(MODID, "fixed_quest_definitions");
	private static final Identifier RANDOM_QUEST_TEMPLATE_MANAGER_ID = Identifier.fromNamespaceAndPath(MODID, "random_quest_templates");
	private static final Identifier GIFT_QUOTE_MANAGER_ID = Identifier.fromNamespaceAndPath(MODID, "gift_quotes");

	/**
	 * 模组主类构造函数喵~
	 *
	 * @param modEventBus 模组事件总线喵~
	 * @param modContainer 模组容器喵~
	 */
	public GirlfriendsMod(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::onCommonSetup);
		modEventBus.addListener(this::registerRegistries);
		modEventBus.addListener(GirlfriendsNetwork::register);
		modEventBus.addListener(this::registerEntityAttributes);
		NeoForge.EVENT_BUS.addListener(this::registerServerReloadListeners);
		NeoForge.EVENT_BUS.addListener(this::registerCommands);
		GirlfriendEntityEvents events = new GirlfriendEntityEvents();
		NeoForge.EVENT_BUS.addListener(events::onEntityDie);
		NeoForge.EVENT_BUS.addListener(events::onEntityJoinWorld);
		NeoForge.EVENT_BUS.addListener(events::onRightClickBlock);
		NeoForge.EVENT_BUS.addListener(events::onServerTick);

		GirlfriendTypes.REGISTER.register(modEventBus);
		GirlfriendsActivities.REGISTER.register(modEventBus);
		GirlfriendsBlocks.REGISTER.register(modEventBus);
		GirlfriendsCreativeModeTabs.REGISTER.register(modEventBus);
		GirlfriendsDataComponentTypes.REGISTER.register(modEventBus);
		GirlfriendsEntities.REGISTER.register(modEventBus);
		GirlfriendsEnvironmentAttributes.REGISTER.register(modEventBus);
		GirlfriendsItems.REGISTER.register(modEventBus);
		GirlfriendsMemoryTypes.REGISTER.register(modEventBus);
		GirlfriendsSensorTypes.REGISTER.register(modEventBus);
		GirlfriendsVoiceEvents.REGISTER.register(modEventBus);

		modContainer.registerConfig(ModConfig.Type.COMMON, GirlfriendsCommonConfig.CONFIG);
	}

	private void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(GirlfriendsVoiceEvents::registerVoices);
	}

	private void registerServerReloadListeners(AddServerReloadListenersEvent event) {
		event.addListener(GIFT_PREFERENCE_MANAGER_ID, GiftPreferenceManager.INSTANCE);
		event.addListener(FIXED_QUEST_DEFINITION_MANAGER_ID, FixedQuestDefinitionManager.INSTANCE);
		event.addListener(RANDOM_QUEST_TEMPLATE_MANAGER_ID, RandomQuestTemplateManager.INSTANCE);
		event.addListener(GIFT_QUOTE_MANAGER_ID, GiftQuoteManager.INSTANCE);
	}

	private void registerRegistries(NewRegistryEvent event) {
		event.register(GirlfriendsRegistries.GIRLFRIEND_TYPE_REGISTRY);
	}

	private void registerEntityAttributes(EntityAttributeCreationEvent event) {
		event.put(GirlfriendsEntities.MOMO.get(), GirlfriendEntity.createAttributes().build());
		event.put(GirlfriendsEntities.YUXI.get(), GirlfriendEntity.createAttributes().build());
		event.put(GirlfriendsEntities.MEISHU.get(), GirlfriendEntity.createAttributes().build());
		event.put(GirlfriendsEntities.WANYING.get(), GirlfriendEntity.createAttributes().build());
		event.put(GirlfriendsEntities.YOURUO.get(), GirlfriendEntity.createAttributes().build());
	}

	/**
	 * 注册模组命令喵~
	 *
	 * @param event 命令注册事件喵~
	 */
	private void registerCommands(RegisterCommandsEvent event) {
		AffectionCommand.register(event.getDispatcher());
	}
}
