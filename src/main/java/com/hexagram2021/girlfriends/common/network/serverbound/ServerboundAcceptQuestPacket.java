package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收接取委托请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundAcceptQuestPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
	public static final Type<ServerboundAcceptQuestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "accept_quest"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundAcceptQuestPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundAcceptQuestPacket::write,
			ServerboundAcceptQuestPacket::read
	);

	private static ServerboundAcceptQuestPacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundAcceptQuestPacket(buffer.readIdentifier());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
