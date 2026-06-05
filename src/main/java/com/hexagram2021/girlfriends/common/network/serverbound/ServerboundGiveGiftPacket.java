package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收赠礼请求的数据包，仅携带角色类型 ID，
 * 偏好等级由服务端从玩家手持物品与 GiftPreferenceManager 判定喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundGiveGiftPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
	public static final Type<ServerboundGiveGiftPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "give_gift"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGiveGiftPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ServerboundGiveGiftPacket::write,
			ServerboundGiveGiftPacket::read
	);

	private static ServerboundGiveGiftPacket read(RegistryFriendlyByteBuf buffer) {
		return new ServerboundGiveGiftPacket(buffer.readIdentifier());
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(this.girlfriendTypeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
