package com.hexagram2021.girlfriends.common.relationship;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * RelationshipService 行为测试喵~
 *
 * @author liudongyu
 */
class RelationshipServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	private static final Identifier YUXI_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");

	/**
	 * 验证好感变更会被限制在合法范围内喵~
	 */
	@Test
	void changeAffectionClampsToRange() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService service = new RelationshipService(data);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");

		service.changeAffection(playerUuid, MOMO_ID, AffectionChangeSource.GIFT, 1200);
		Assertions.assertEquals(1000, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());

		service.changeAffection(playerUuid, MOMO_ID, AffectionChangeSource.PLAYER_ATTACK, -2000);
		Assertions.assertEquals(0, data.getOrCreateRelation(playerUuid, MOMO_ID).getAffection());
	}

	/**
	 * 验证 700 到 899 区间需要亲密确认才能进入亲密阶段喵~
	 */
	@Test
	void stageRequiresIntimacyConfirmation() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService service = new RelationshipService(data);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000003");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, MOMO_ID);
		relation.setAffection(700);

		Assertions.assertEquals(AffectionStage.AFFECTION, service.getEffectiveStage(relation));
		relation.setConfirmedIntimacy(true);
		Assertions.assertEquals(AffectionStage.INTIMATE, service.getEffectiveStage(relation));
	}

	/**
	 * 验证每日计数只会在跨天时重置一次喵~
	 */
	@Test
	void resetDailyCountersOnlyOncePerDay() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService service = new RelationshipService(data);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000004");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, YUXI_ID);
		relation.setDailyGiftGain(15);
		relation.setDailyHomeGainClaimed(true);
		relation.setDailyConflictTriggered(true);

		service.resetDailyCounters(10L);

		Assertions.assertEquals(0, relation.getDailyGiftGain());
		Assertions.assertFalse(relation.isDailyHomeGainClaimed());
		Assertions.assertFalse(relation.isDailyConflictTriggered());
		Assertions.assertEquals(10L, relation.getLastDailyResetDay());

		relation.setDailyGiftGain(8);
		relation.setDailyHomeGainClaimed(true);
		relation.setDailyConflictTriggered(true);
		service.resetDailyCounters(10L);

		Assertions.assertEquals(8, relation.getDailyGiftGain());
		Assertions.assertTrue(relation.isDailyHomeGainClaimed());
		Assertions.assertTrue(relation.isDailyConflictTriggered());
		Assertions.assertEquals(10L, relation.getLastDailyResetDay());
	}
}
