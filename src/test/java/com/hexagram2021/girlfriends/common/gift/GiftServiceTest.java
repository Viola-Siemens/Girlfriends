package com.hexagram2021.girlfriends.common.gift;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * GiftService 行为测试喵~
 *
 * @author liudongyu
 */
class GiftServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	/**
	 * 验证最喜欢礼物会应用递减公式并累计每日收益喵~
	 */
	@Test
	void favoriteGiftUsesDiminishingReturnsAndDailyCapAccounting() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftService giftService = new GiftService(relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000005");

		GiftResult firstResult = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);
		GiftResult secondResult = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);

		Assertions.assertFalse(firstResult.rejected());
		Assertions.assertEquals(5.0F, firstResult.affectionDelta(), 0.0001F);
		Assertions.assertFalse(secondResult.rejected());
		Assertions.assertEquals(4.14578F, secondResult.affectionDelta(), 0.0001F);
		Assertions.assertEquals(9.14578F, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection(), 0.0001F);
		Assertions.assertEquals(9.14578F, data.getOrCreateRelation(playerUuid, MOMO_ID).getDailyGiftGain(), 0.0001F);
	}

	/**
	 * 验证到达每日上限后正向礼物会被拒收喵~
	 */
	@Test
	void positiveGiftRejectedAfterDailyCap() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftService giftService = new GiftService(relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000006");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setDailyGiftGain(15);

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.LIKED);

		Assertions.assertTrue(result.rejected());
		Assertions.assertEquals(0.0F, result.affectionDelta());
		Assertions.assertEquals(15.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getDailyGiftGain());
		Assertions.assertEquals(0.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());
	}

	/**
	 * 验证不喜欢的礼物在上限时仍会降低好感喵~
	 */
	@Test
	void dislikedGiftStillReducesAffectionAtDailyCap() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftService giftService = new GiftService(relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000007");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(100);
		data.getOrCreateRelation(playerUuid, MOMO_ID).setDailyGiftGain(15);

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.DISLIKED);

		Assertions.assertFalse(result.rejected());
		Assertions.assertEquals(-1.0F, result.affectionDelta());
		Assertions.assertEquals(99.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());
		Assertions.assertEquals(15.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getDailyGiftGain());
	}

	/**
	 * 验证无赠礼权限时会拒绝且不修改好感喵~
	 */
	@Test
	void permissionPredicateFalseRejectsWithoutAffectionChange() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftService giftService = new GiftService(relationshipService, null, (_, _) -> false);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000008");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(20);
		data.getOrCreateRelation(playerUuid, MOMO_ID).setDailyGiftGain(3);

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);

		Assertions.assertTrue(result.rejected());
		Assertions.assertEquals(0.0F, result.affectionDelta());
		Assertions.assertEquals(20.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());
		Assertions.assertEquals(3.0F, data.getOrCreateRelation(playerUuid, MOMO_ID).getDailyGiftGain());
	}

	/**
	 * 验证有 GiftQuoteManager 时 favorite 礼物 GiftResult 包含非空 quoteKey 喵~
	 * 注：此时 GiftQuoteManager.quotesMap 为空（数据包未加载），quoteKey 应为 null 喵~
	 */
	@Test
	void favoriteGiftIncludesQuoteKey() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftQuoteManager quoteManager = GiftQuoteManager.INSTANCE;
		GiftService giftService = new GiftService(relationshipService, null, quoteManager, (_, _) -> true);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000010");

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);
		Assertions.assertFalse(result.rejected());
		Assertions.assertNull(result.quoteKey()); // 无数据包时 quoteKey 为 null
	}

	/**
	 * 验证无 GiftQuoteManager 时 GiftResult.quoteKey 为 null（向后兼容）喵~
	 */
	@Test
	void noQuoteManagerLeavesQuoteKeyNull() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		GiftService giftService = new GiftService(relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000011");

		GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);
		Assertions.assertFalse(result.rejected());
		Assertions.assertNull(result.quoteKey());
	}
}
