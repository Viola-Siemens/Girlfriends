package com.hexagram2021.girlfriends.common.network.clientbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.hexagram2021.girlfriends.common.network.NetworkCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 同步角色交互数据到客户端的数据包喵~
 *
 * @param summary 交互摘要喵~
 * @author liudongyu
 */
public record ClientboundSyncInteractionDataPacket(InteractionSummary summary) implements CustomPacketPayload {
	public static final Type<ClientboundSyncInteractionDataPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "sync_interaction_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncInteractionDataPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ClientboundSyncInteractionDataPacket::write,
			ClientboundSyncInteractionDataPacket::read
	);

	private static ClientboundSyncInteractionDataPacket read(RegistryFriendlyByteBuf buffer) {
		return new ClientboundSyncInteractionDataPacket(NetworkCodecs.readInteractionSummary(buffer));
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		NetworkCodecs.writeInteractionSummary(buffer, this.summary);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
