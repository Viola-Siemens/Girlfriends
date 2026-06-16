package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收确立关系请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundConfirmIntimacyPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
	public static final Type<ServerboundConfirmIntimacyPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "confirm_intimacy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundConfirmIntimacyPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundConfirmIntimacyPacket::write,
			ServerboundConfirmIntimacyPacket::read
	);

	private static ServerboundConfirmIntimacyPacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundConfirmIntimacyPacket(buffer.readIdentifier());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
