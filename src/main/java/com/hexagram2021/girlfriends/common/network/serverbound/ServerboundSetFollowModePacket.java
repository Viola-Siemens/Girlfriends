package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收跟随模式设置请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param followMode 跟随模式喵~
 * @author liudongyu
 */
public record ServerboundSetFollowModePacket(Identifier girlfriendTypeId, FollowMode followMode) implements CustomPacketPayload {
	public static final Type<ServerboundSetFollowModePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "set_follow_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetFollowModePacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundSetFollowModePacket::write,
			ServerboundSetFollowModePacket::read
	);

	private static ServerboundSetFollowModePacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundSetFollowModePacket(buffer.readIdentifier(), buffer.readEnum(FollowMode.class));
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
		buffer.writeEnum(this.followMode);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
