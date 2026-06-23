package com.hexagram2021.girlfriends.common.network.clientbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 语音播放网络包——Server → Client 喵~
 *
 * @param voiceKey 语音 key（如 "momo.liked_0"）喵~
 * @param x        音源 X 坐标喵~
 * @param y        音源 Y 坐标喵~
 * @param z        音源 Z 坐标喵~
 *
 * @author liudongyu
 */
public record ClientboundPlayVoicePacket(String voiceKey, double x, double y, double z)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ClientboundPlayVoicePacket> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "play_voice"));

	public static final StreamCodec<ByteBuf, ClientboundPlayVoicePacket> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ClientboundPlayVoicePacket::voiceKey,
					ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::x,
					ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::y,
					ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::z,
					ClientboundPlayVoicePacket::new
			);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
