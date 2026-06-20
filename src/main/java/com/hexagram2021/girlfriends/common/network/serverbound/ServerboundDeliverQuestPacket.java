package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收交付委托请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundDeliverQuestPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
	public static final Type<ServerboundDeliverQuestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "deliver_quest"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeliverQuestPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundDeliverQuestPacket::write,
			ServerboundDeliverQuestPacket::read
	);

	private static ServerboundDeliverQuestPacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundDeliverQuestPacket(buffer.readIdentifier());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
