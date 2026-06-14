package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceManager;
import com.hexagram2021.girlfriends.common.gift.GiftService;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundQuestIconPacket;
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundSyncInteractionDataPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundAcceptQuestPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundGiveGiftPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundSetFollowModePacket;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.QuestService;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Girlfriends 网络数据包注册入口喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsNetwork {
	private static final String PROTOCOL_VERSION = "1";

	/**
	 * 注册网络数据包喵~
	 *
	 * @param event 网络负载注册事件喵~
	 */
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(GirlfriendsMod.MODID + ":" + PROTOCOL_VERSION);
		registrar.playToClient(ClientboundSyncInteractionDataPacket.TYPE, ClientboundSyncInteractionDataPacket.STREAM_CODEC, GirlfriendsNetwork::handleSyncInteractionData);
		registrar.playToClient(ClientboundQuestIconPacket.TYPE, ClientboundQuestIconPacket.STREAM_CODEC, GirlfriendsNetwork::handleQuestIcon);
		registrar.playToServer(ServerboundGiveGiftPacket.TYPE, ServerboundGiveGiftPacket.STREAM_CODEC, GirlfriendsNetwork::handleGiveGift);
		registrar.playToServer(ServerboundAcceptQuestPacket.TYPE, ServerboundAcceptQuestPacket.STREAM_CODEC, GirlfriendsNetwork::handleAcceptQuest);
		registrar.playToServer(ServerboundSetFollowModePacket.TYPE, ServerboundSetFollowModePacket.STREAM_CODEC, GirlfriendsNetwork::handleSetFollowMode);
	}

	private static void handleSyncInteractionData(ClientboundSyncInteractionDataPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientInteractionStore.setSummary(packet.summary()));
	}

	private static void handleQuestIcon(ClientboundQuestIconPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientInteractionStore.setQuestIcon(packet.summary()));
	}

	private static void handleGiveGift(ServerboundGiveGiftPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
			GirlfriendsWorldData data = getWorldData(player);
			if(!canReachEntity(player, data, packet.girlfriendTypeId())) {
				return;
			}
			RelationshipService relationshipService = new RelationshipService(data);
			GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
					(playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
			giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), player.getItemInHand(InteractionHand.MAIN_HAND));
		}
	}

	private static void handleAcceptQuest(ServerboundAcceptQuestPacket packet, IPayloadContext context) {
		if(context.player() instanceof ServerPlayer player) {
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
