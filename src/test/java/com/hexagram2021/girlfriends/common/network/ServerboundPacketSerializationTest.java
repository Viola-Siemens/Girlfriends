package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.network.serverbound.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 新增 Serverbound 包序列化往返测试喵~
 *
 * @author liudongyu
 */
public class ServerboundPacketSerializationTest {
	private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	@Test
	void deliverQuestPacketRoundTrip() {
		ServerboundDeliverQuestPacket packet = new ServerboundDeliverQuestPacket(TEST_ID);
		ByteBuf buf = Unpooled.buffer();
		RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
		ServerboundDeliverQuestPacket.STREAM_CODEC.encode(registryBuf, packet);
		ServerboundDeliverQuestPacket decoded = ServerboundDeliverQuestPacket.STREAM_CODEC.decode(registryBuf);
		assertEquals(TEST_ID, decoded.girlfriendTypeId());
	}

	@Test
	void confirmIntimacyPacketRoundTrip() {
		ServerboundConfirmIntimacyPacket packet = new ServerboundConfirmIntimacyPacket(TEST_ID);
		ByteBuf buf = Unpooled.buffer();
		RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
		ServerboundConfirmIntimacyPacket.STREAM_CODEC.encode(registryBuf, packet);
		ServerboundConfirmIntimacyPacket decoded = ServerboundConfirmIntimacyPacket.STREAM_CODEC.decode(registryBuf);
		assertEquals(TEST_ID, decoded.girlfriendTypeId());
	}

	@Test
	void inviteHomePacketRoundTrip() {
		ServerboundInviteHomePacket packet = new ServerboundInviteHomePacket(TEST_ID);
		ByteBuf buf = Unpooled.buffer();
		RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
		ServerboundInviteHomePacket.STREAM_CODEC.encode(registryBuf, packet);
		ServerboundInviteHomePacket decoded = ServerboundInviteHomePacket.STREAM_CODEC.decode(registryBuf);
		assertEquals(TEST_ID, decoded.girlfriendTypeId());
	}

	@Test
	void giveGiftFromSlotPacketRoundTrip() {
		ServerboundGiveGiftFromSlotPacket packet = new ServerboundGiveGiftFromSlotPacket(TEST_ID, 15);
		ByteBuf buf = Unpooled.buffer();
		RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
		ServerboundGiveGiftFromSlotPacket.STREAM_CODEC.encode(registryBuf, packet);
		ServerboundGiveGiftFromSlotPacket decoded = ServerboundGiveGiftFromSlotPacket.STREAM_CODEC.decode(registryBuf);
		assertEquals(TEST_ID, decoded.girlfriendTypeId());
		assertEquals(15, decoded.slotIndex());
	}
}
