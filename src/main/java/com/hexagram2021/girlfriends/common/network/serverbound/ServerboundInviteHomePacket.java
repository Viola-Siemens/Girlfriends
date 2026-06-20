package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收邀请同居请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundInviteHomePacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
	public static final Type<ServerboundInviteHomePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "invite_home"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundInviteHomePacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundInviteHomePacket::write,
			ServerboundInviteHomePacket::read
	);

	private static ServerboundInviteHomePacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundInviteHomePacket(buffer.readIdentifier());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
