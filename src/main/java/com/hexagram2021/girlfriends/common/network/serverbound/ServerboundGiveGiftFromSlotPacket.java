package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收从背包槽位送礼请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param slotIndex 背包槽位索引喵~
 * @author liudongyu
 */
public record ServerboundGiveGiftFromSlotPacket(Identifier girlfriendTypeId, int slotIndex) implements CustomPacketPayload {
	public static final Type<ServerboundGiveGiftFromSlotPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "give_gift_from_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGiveGiftFromSlotPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundGiveGiftFromSlotPacket::write,
			ServerboundGiveGiftFromSlotPacket::read
	);

	private static ServerboundGiveGiftFromSlotPacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundGiveGiftFromSlotPacket(buffer.readIdentifier(), buffer.readVarInt());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
		buffer.writeVarInt(this.slotIndex);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
