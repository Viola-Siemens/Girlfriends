package com.hexagram2021.girlfriends.common.network.clientbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.network.NetworkCodecs;
import com.hexagram2021.girlfriends.common.network.QuestIconSummary;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 同步角色委托图标到客户端的数据包喵~
 *
 * @param summary 委托图标摘要喵~
 * @author liudongyu
 */
public record ClientboundQuestIconPacket(QuestIconSummary summary) implements CustomPacketPayload {
	public static final Type<ClientboundQuestIconPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "quest_icon"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundQuestIconPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ClientboundQuestIconPacket::write,
			ClientboundQuestIconPacket::read
	);

	private static ClientboundQuestIconPacket read(RegistryFriendlyByteBuf buffer) {
		return new ClientboundQuestIconPacket(NetworkCodecs.readQuestIconSummary(buffer));
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		NetworkCodecs.writeQuestIconSummary(buffer, this.summary);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
