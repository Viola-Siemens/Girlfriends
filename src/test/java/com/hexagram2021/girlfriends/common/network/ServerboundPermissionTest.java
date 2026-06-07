package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.gift.GiftResult;
import com.hexagram2021.girlfriends.common.gift.GiftService;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerboundPermissionTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	@Test
	void rejectedGiftPermissionDoesNotChangeAffection() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		UUID playerUuid = UUID.randomUUID();
		RelationshipService relationshipService = new RelationshipService(data);
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(120.0F);
		GiftService giftService = new GiftService(relationshipService, (uuid, girlfriendTypeId) -> false);

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);

		assertTrue(result.rejected());
		assertEquals(120.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());
	}
}
