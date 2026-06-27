package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.binding.BindingService;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.config.GirlfriendsCommonConfig;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceManager;
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import com.hexagram2021.girlfriends.common.gift.GiftResult;
import com.hexagram2021.girlfriends.common.gift.GiftService;
import com.hexagram2021.girlfriends.common.home.BedValidator;
import com.hexagram2021.girlfriends.common.home.HomeService;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundPlayVoicePacket;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundQuestIconPacket;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundSyncInteractionDataPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.*;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.FixedQuestDefinitionManager;
import com.hexagram2021.girlfriends.common.quest.QuestService;
import com.hexagram2021.girlfriends.common.quest.RandomQuestTemplateManager;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Girlfriends 网络数据包注册入口喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsNetwork {
	private static final String PROTOCOL_VERSION = "1";

	/**
	 * 客户端侧屏幕打开器，由 {@link com.hexagram2021.girlfriends.client.GirlfriendsModClient} 注入喵~
	 * 在专用服务端上此字段始终为空，从而隔离客户端类引用喵~
	 */
	private static BiConsumer<Identifier, InteractionSummary> screenOpener = (_, _) -> {};

	/**
	 * 由客户端模组类调用，注入屏幕打开逻辑喵~
	 *
	 * @param opener 屏幕打开器，接收角色类型 ID 和交互摘要喵~
	 */
	public static void setScreenOpener(BiConsumer<Identifier, InteractionSummary> opener) {
		screenOpener = opener;
	}

	/**
	 * 客户端侧语音播放 handler，由 {@link com.hexagram2021.girlfriends.client.GirlfriendsModClient} 注入喵~
	 * 在专用服务端上此字段始终为 null，从而隔离客户端类引用喵~
	 */
	@Nullable
	private static Consumer<ClientboundPlayVoicePacket> voiceHandler = null;

	/**
	 * 由客户端模组类调用，注入语音播放逻辑喵~
	 *
	 * @param handler 语音播放 handler 喵~
	 */
	public static void setVoiceHandler(@Nullable Consumer<ClientboundPlayVoicePacket> handler) {
		voiceHandler = handler;
	}

	/**
	 * 注册网络数据包喵~
	 *
	 * @param event 网络负载注册事件喵~
	 */
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(GirlfriendsMod.MODID + ":" + PROTOCOL_VERSION);
		registrar.playToClient(ClientboundSyncInteractionDataPacket.TYPE, ClientboundSyncInteractionDataPacket.STREAM_CODEC, GirlfriendsNetwork::handleSyncInteractionData);
		registrar.playToClient(ClientboundQuestIconPacket.TYPE, ClientboundQuestIconPacket.STREAM_CODEC, GirlfriendsNetwork::handleQuestIcon);
		registrar.playToClient(ClientboundPlayVoicePacket.TYPE, ClientboundPlayVoicePacket.STREAM_CODEC, GirlfriendsNetwork::handlePlayVoice);
		registrar.playToServer(ServerboundGiveGiftPacket.TYPE, ServerboundGiveGiftPacket.STREAM_CODEC, GirlfriendsNetwork::handleGiveGift);
		registrar.playToServer(ServerboundAcceptQuestPacket.TYPE, ServerboundAcceptQuestPacket.STREAM_CODEC, GirlfriendsNetwork::handleAcceptQuest);
		registrar.playToServer(ServerboundSetFollowModePacket.TYPE, ServerboundSetFollowModePacket.STREAM_CODEC, GirlfriendsNetwork::handleSetFollowMode);
		registrar.playToServer(ServerboundDeliverQuestPacket.TYPE, ServerboundDeliverQuestPacket.STREAM_CODEC, GirlfriendsNetwork::handleDeliverQuest);
		registrar.playToServer(ServerboundConfirmIntimacyPacket.TYPE, ServerboundConfirmIntimacyPacket.STREAM_CODEC, GirlfriendsNetwork::handleConfirmIntimacy);
		registrar.playToServer(ServerboundInviteHomePacket.TYPE, ServerboundInviteHomePacket.STREAM_CODEC, GirlfriendsNetwork::handleInviteHome);
		registrar.playToServer(ServerboundGiveGiftFromSlotPacket.TYPE, ServerboundGiveGiftFromSlotPacket.STREAM_CODEC, GirlfriendsNetwork::handleGiveGiftFromSlot);
	}

	private static void handleSyncInteractionData(ClientboundSyncInteractionDataPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			ClientInteractionStore.setSummary(packet.summary());
			BiConsumer<Identifier, InteractionSummary> opener = screenOpener;
			if (ClientInteractionStore.consumePendingInteraction(packet.summary().girlfriendTypeId())) {
				opener.accept(packet.summary().girlfriendTypeId(), packet.summary());
			}
		});
	}

	private static void handleQuestIcon(ClientboundQuestIconPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientInteractionStore.setQuestIcon(packet.summary()));
	}

	private static void handlePlayVoice(ClientboundPlayVoicePacket packet, IPayloadContext context) {
		Consumer<ClientboundPlayVoicePacket> handler = voiceHandler;
		if (handler != null) {
			// enqueueWork 在 Render Thread 上执行，满足 level.playSound 的主线程要求喵~
			context.enqueueWork(() -> handler.accept(packet));
		}
	}

	private static void handleGiveGift(ServerboundGiveGiftPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			GirlfriendsWorldData data = getWorldData(player);
			if(!canReachEntity(player, data, packet.girlfriendTypeId())) {
				return;
			}
			RelationshipService relationshipService = new RelationshipService(data);
			GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
					GiftQuoteManager.INSTANCE,
					(_, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
			GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), player.getItemInHand(InteractionHand.MAIN_HAND));
			sendGiftFeedback(player, packet.girlfriendTypeId(), result);
		}
	}

	private static void handleAcceptQuest(ServerboundAcceptQuestPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			if(!GirlfriendsCommonConfig.ENABLE_QUESTS.get()) {
				player.sendSystemMessage(Component.translatable("message.girlfriends.quests_disabled").withStyle(ChatFormatting.RED));
				return;
			}
			GirlfriendsWorldData data = getWorldData(player);
			if(canReachEntity(player, data, packet.girlfriendTypeId())) {
				new QuestService(data, new RelationshipService(data)).acceptCurrentQuest(player.getUUID(), packet.girlfriendTypeId());
			}
		}
	}

	private static void handleSetFollowMode(ServerboundSetFollowModePacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			GirlfriendsWorldData data = getWorldData(player);
			PlayerCharacterRelation relation = data.getExistingRelation(player.getUUID(), packet.girlfriendTypeId());
			CharacterWorldState state = data.getExistingCharacterState(packet.girlfriendTypeId());
			if(relation != null && state != null && state.isAlive() && relation.isConfirmedIntimacy()) {
				data.updateCharacter(packet.girlfriendTypeId(), characterState -> {
					characterState.setFollowTargetUuid(player.getUUID());
					characterState.setFollowMode(packet.followMode());
				});
				// 同步到实体喵~
				UUID entityUuid = state.getEntityUuid();
				if(entityUuid != null) {
					Entity entity = player.level().getEntity(entityUuid);
					if(entity instanceof GirlfriendEntity girlfriend) {
						girlfriend.setFollowMode(packet.followMode());
						girlfriend.setLikedPlayerUuid(player.getUUID());
					}
				}
			}
		}
	}

	private static void handleDeliverQuest(ServerboundDeliverQuestPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			GirlfriendsWorldData data = getWorldData(player);
			if(canReachEntity(player, data, packet.girlfriendTypeId())) {
				RelationshipService relationshipService = new RelationshipService(data);
				QuestService questService = new QuestService(
						data,
						relationshipService,
						id -> FixedQuestDefinitionManager.INSTANCE.getDefinition(id).orElse(null),
						id -> RandomQuestTemplateManager.INSTANCE.getRandomDefinitionForType(id, player.level().getRandom()),
						randomSource -> 5 + randomSource.nextInt(6)
				);
				if(questService.completeCurrentQuest(player.getUUID(), packet.girlfriendTypeId())) {
					InteractionSummaryService summaryService = new InteractionSummaryService(
							data, relationshipService, questService
					);
					QuestIconSummary questIcon = summaryService.buildQuestIcon(packet.girlfriendTypeId());
					if(questIcon != null) {
						player.connection.send(new ClientboundQuestIconPacket(questIcon));
					}
					player.sendSystemMessage(Component.translatable("message.girlfriends.quest_completed"));
				}
			}
		}
	}

	private static void handleConfirmIntimacy(ServerboundConfirmIntimacyPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			if(!GirlfriendsCommonConfig.ENABLE_RELATION_BIND.get()) {
				player.sendSystemMessage(Component.translatable("message.girlfriends.relation_binding_disabled").withStyle(ChatFormatting.RED));
				return;
			}
			GirlfriendsWorldData data = getWorldData(player);
			if(canReachEntity(player, data, packet.girlfriendTypeId())) {
				RelationshipService relationshipService = new RelationshipService(data);
				new BindingService(data, relationshipService)
						.confirmIntimacy(player.getUUID(), packet.girlfriendTypeId());
				player.sendSystemMessage(Component.translatable("message.girlfriends.intimacy_confirmed"));
			}
		}
	}

	private static void handleInviteHome(ServerboundInviteHomePacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			if(!GirlfriendsCommonConfig.ENABLE_HOME_INVITATION.get()) {
				player.sendSystemMessage(Component.translatable("message.girlfriends.home_invitation_disabled").withStyle(ChatFormatting.RED));
				return;
			}
			GirlfriendsWorldData data = getWorldData(player);
			if(canReachEntity(player, data, packet.girlfriendTypeId())) {
				ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
				if(respawnConfig == null) {
					player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_no_respawn"));
					return;
				}
				BlockPos respawnPos = respawnConfig.respawnData().pos();
				Identifier dimension = respawnConfig.respawnData().dimension().identifier();
				BedValidator bedValidator = anchor -> player.level().getBlockState(
						new BlockPos(anchor.x(), anchor.y(), anchor.z())
				).getBlock() instanceof BedBlock;
				HomeService homeService = new HomeService(data, new RelationshipService(data), bedValidator);
				if(homeService.inviteHome(player.getUUID(), packet.girlfriendTypeId(), dimension,
						respawnPos.getX(), respawnPos.getY(), respawnPos.getZ())) {
					player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_success"));
				} else {
					player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_failed"));
				}
			}
		}
	}

	private static void handleGiveGiftFromSlot(ServerboundGiveGiftFromSlotPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			GirlfriendsWorldData data = getWorldData(player);
			if(canReachEntity(player, data, packet.girlfriendTypeId())) {
				int slotIndex = packet.slotIndex();
				if(slotIndex < 0 || slotIndex >= 36) {
					return;
				}
				ItemStack itemStack = player.getInventory().getItem(slotIndex);
				if(itemStack.isEmpty()) {
					return;
				}
				RelationshipService relationshipService = new RelationshipService(data);
				GiftService giftService = new GiftService(
						relationshipService, GiftPreferenceManager.INSTANCE,
						GiftQuoteManager.INSTANCE,
						(_, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId)
				);
				GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), itemStack);
				if(!result.rejected()) {
					player.getInventory().removeItem(slotIndex, 1);
				}
				sendGiftFeedback(player, packet.girlfriendTypeId(), result);
			}
		}
	}

	/**
	 * 发送赠礼反馈到玩家——角色台词走聊天栏，好感度变化走 subtitle 喵~
	 *
	 * @param player 目标玩家喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param result 赠礼结果喵~
	 */
	private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId, GiftResult result) {
		// 1. 角色台词 → 聊天栏（若 quoteKey 不为 null）喵~
		if (result.quoteKey() != null) {
			player.sendSystemMessage(Component.translatable(result.quoteKey()));

			// 1.5 角色语音 → 客户端播放（从角色实体位置发出）喵~
			GirlfriendsWorldData data = getWorldData(player);
			CharacterWorldState state = data.getExistingCharacterState(girlfriendTypeId);
			if (state != null && state.isAlive() && state.getEntityUuid() != null) {
				Entity entity = player.level().getEntity(state.getEntityUuid());
				if (entity != null) {
					String voiceKey = GirlfriendsVoiceManager.extractVoiceKey(result.quoteKey());
					player.connection.send(new ClientboundPlayVoicePacket(
							voiceKey,
							entity.getX(), entity.getY(), entity.getZ()
					));
				}
			}
		}

		// 2. 好感度变化 → subtitle 三联包喵~
		// subtitle 在原版中必须配合 title 叠加层才能渲染：
		//   TitlesAnimation 设定时序（5 ticks 淡入，40 ticks 停留 = 2s，10 ticks 淡出 = 0.5s）
		//   SetTitleText 空标题激活叠加层
		//   SetSubtitleText 为实际好感度消息喵~
		Component characterName = Component.translatable("girlfriends.girlfriend_type." + girlfriendTypeId.getPath());
		Component subtitleMsg = Component.translatable(
				result.messageKey(), characterName,
				String.format("%+.1f", result.affectionDelta())
		).withStyle(result.level().getStyles());
		player.connection.send(new ClientboundSetActionBarTextPacket(subtitleMsg));
	}

	private static boolean canReachCharacter(GirlfriendsWorldData data, Identifier girlfriendTypeId) {
		CharacterWorldState state = data.getExistingCharacterState(girlfriendTypeId);
		return state != null && state.isAlive();
	}

	/**
	 * 校验玩家是否在实体交互范围内喵~
	 *
	 * @param player 玩家喵~
	 * @param data 世界数据喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否可交互喵~
	 */
	private static boolean canReachEntity(ServerPlayer player, GirlfriendsWorldData data, Identifier girlfriendTypeId) {
		CharacterWorldState state = data.getExistingCharacterState(girlfriendTypeId);
		if (state == null || !state.isAlive()) {
			return false;
		}
		UUID entityUuid = state.getEntityUuid();
		if (entityUuid == null) {
			return false;
		}
		Entity entity = player.level().getEntity(entityUuid);
		if (!(entity instanceof GirlfriendEntity)) {
			return false;
		}
		return player.distanceTo(entity) <= 8.0;
	}

	private static GirlfriendsWorldData getWorldData(ServerPlayer player) {
		ServerLevel level = player.level().getServer().overworld();
		return level.getDataStorage().computeIfAbsent(GirlfriendsWorldData.TYPE);
	}

	private GirlfriendsNetwork() {
	}
}
