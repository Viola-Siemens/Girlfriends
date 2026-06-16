package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ClientInteractionStore 单元测试喵~
 *
 * @author liudongyu
 */
public class ClientInteractionStoreTest {
	private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	@Test
	void markPendingAndConsumeReturnsTrue() {
		ClientInteractionStore.markPendingInteraction(TEST_ID);
		assertTrue(ClientInteractionStore.consumePendingInteraction(TEST_ID));
	}

	@Test
	void consumeWithoutMarkReturnsFalse() {
		assertFalse(ClientInteractionStore.consumePendingInteraction(TEST_ID));
	}

	@Test
	void consumeIsIdempotent() {
		ClientInteractionStore.markPendingInteraction(TEST_ID);
		assertTrue(ClientInteractionStore.consumePendingInteraction(TEST_ID));
		assertFalse(ClientInteractionStore.consumePendingInteraction(TEST_ID));
	}

	@Test
	void markMultipleAndConsumeEach() {
		Identifier id1 = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
		Identifier id2 = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");
		ClientInteractionStore.markPendingInteraction(id1);
		ClientInteractionStore.markPendingInteraction(id2);
		assertTrue(ClientInteractionStore.consumePendingInteraction(id1));
		assertFalse(ClientInteractionStore.consumePendingInteraction(id1));
		assertTrue(ClientInteractionStore.consumePendingInteraction(id2));
	}
}
