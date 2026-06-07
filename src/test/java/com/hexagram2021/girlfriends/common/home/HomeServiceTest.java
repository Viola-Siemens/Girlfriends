package com.hexagram2021.girlfriends.common.home;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * HomeService 行为测试喵~
 *
 * @author liudongyu
 */
class HomeServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	private static final Identifier YUXI_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");

	/**
	 * 验证邀请家园需要家园阶段、亲密确认、固定委托完成且无现有伙伴喵~
	 */
	@Test
	void inviteHomeRequiresAffectionAndNoExistingPartner() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		HomeService homeService = new HomeService(data, relationshipService, _ -> true);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000012");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, MOMO_ID);
		relation.setAffection(900);
		relation.setConfirmedIntimacy(true);
		relation.getCompletedFixedQuests().add(10);

		boolean invited = homeService.inviteHome(playerUuid, MOMO_ID, Level.OVERWORLD.identifier(), 1, 64, 1);

		Assertions.assertTrue(invited);
		Assertions.assertEquals(MOMO_ID, data.getOrCreateHomeState(playerUuid).getCharacterId());
		Assertions.assertTrue(data.getOrCreateHomeState(playerUuid).isActive());
		Assertions.assertTrue(relation.isHomePartner());
		Assertions.assertFalse(homeService.inviteHome(playerUuid, YUXI_ID, Level.OVERWORLD.identifier(), 1, 64, 1));
	}

	/**
	 * 验证家园收益每日只增加一次好感喵~
	 */
	@Test
	void homeBenefitAddsDailyAffectionOnce() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		HomeService homeService = new HomeService(data, relationshipService, bedPos -> true);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000013");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, YUXI_ID);
		relation.setAffection(900);
		relation.setConfirmedIntimacy(true);
		relation.getCompletedFixedQuests().add(10);
		homeService.inviteHome(playerUuid, YUXI_ID, Level.OVERWORLD.identifier(), 0, 64, 0);

		HomeTickResult first = homeService.applyHomeBenefit(playerUuid, true, true);
		HomeTickResult second = homeService.applyHomeBenefit(playerUuid, true, true);

		Assertions.assertTrue(first.healed());
		Assertions.assertEquals(2.0F, first.affectionDelta());
		Assertions.assertFalse(second.healed());
		Assertions.assertEquals(0.0F, second.affectionDelta());
		Assertions.assertEquals(902.0F, relation.getAffection());
	}

	/**
	 * 验证家园争执每日只触发一次并扣减双方好感喵~
	 */
	@Test
	void conflictPenalizesHomePartnerAndVisitorOncePerDay() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		HomeService homeService = new HomeService(data, relationshipService, bedPos -> true);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		PlayerCharacterRelation homePartner = data.getOrCreateRelation(playerUuid, MOMO_ID);
		homePartner.setAffection(900);
		homePartner.setConfirmedIntimacy(true);
		homePartner.getCompletedFixedQuests().add(10);
		PlayerCharacterRelation visitor = data.getOrCreateRelation(playerUuid, YUXI_ID);
		visitor.setAffection(700);
		visitor.setConfirmedIntimacy(true);
		homeService.inviteHome(playerUuid, MOMO_ID, Level.OVERWORLD.identifier(), 0, 64, 0);

		HomeConflictResult first = homeService.triggerConflict(playerUuid, YUXI_ID, () -> 4);
		HomeConflictResult second = homeService.triggerConflict(playerUuid, YUXI_ID, () -> 4);

		Assertions.assertTrue(first.triggered());
		Assertions.assertEquals(-4.0F, first.homePartnerDelta());
		Assertions.assertEquals(-4.0F, first.visitorDelta());
		Assertions.assertFalse(second.triggered());
		Assertions.assertEquals(896.0F, homePartner.getAffection());
		Assertions.assertEquals(696.0F, visitor.getAffection());
	}
}
