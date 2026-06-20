package com.hexagram2021.girlfriends.common.binding;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * BindingService 行为测试喵~
 *
 * @author liudongyu
 */
class BindingServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
	private static final Identifier YUXI_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");

	/**
	 * 验证爱慕阶段在无绑定时建立绑定喵~
	 */
	@Test
	void affectionStageCreatesBindingWhenNoneExists() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000009");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(500);

		bindingService.updateBindingAfterAffectionChange(playerUuid, MOMO_ID, 10L);

		Assertions.assertEquals(playerUuid, data.getCharacterState(MOMO_ID).getBinding().getBoundPlayerUuid());
		Assertions.assertTrue(bindingService.canReceiveGift(playerUuid, MOMO_ID));
	}

	/**
	 * 验证非绑定玩家不能推进礼物和固定委托喵~
	 */
	@Test
	void nonBoundPlayerCannotReceiveGiftOrFixedQuest() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000020");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000021");
		data.getOrCreateRelation(first, MOMO_ID).setAffection(500);

		bindingService.updateBindingAfterAffectionChange(first, MOMO_ID, 1L);

		Assertions.assertFalse(bindingService.canReceiveGift(second, MOMO_ID));
		Assertions.assertFalse(bindingService.canReceiveFixedQuest(second, MOMO_ID));
		Assertions.assertTrue(bindingService.canReceiveRandomQuest(second, MOMO_ID));
	}

	/**
	 * 验证挑战者动摇三天后转移绑定喵~
	 */
	@Test
	void challengerTransfersBindingAfterThreeDays() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000010");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000011");
		data.getOrCreateRelation(first, YUXI_ID).setAffection(500);
		data.getOrCreateRelation(second, YUXI_ID).setAffection(501);

		bindingService.updateBindingAfterAffectionChange(first, YUXI_ID, 1L);
		bindingService.updateBindingAfterAffectionChange(second, YUXI_ID, 2L);
		bindingService.checkWavering(YUXI_ID, 5L);

		Assertions.assertEquals(second, data.getCharacterState(YUXI_ID).getBinding().getBoundPlayerUuid());
	}

	/**
	 * 验证亲密确认会锁定绑定并限制随机委托喵~
	 */
	@Test
	void confirmIntimacyLocksBindingAndRandomQuestPermission() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000012");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000013");
		data.getOrCreateRelation(first, MOMO_ID).setAffection(700);

		bindingService.confirmIntimacy(first, MOMO_ID);

		Assertions.assertTrue(data.getOrCreateRelation(first, MOMO_ID).isConfirmedIntimacy());
		Assertions.assertEquals(first, data.getCharacterState(MOMO_ID).getBinding().getBoundPlayerUuid());
		Assertions.assertTrue(data.getCharacterState(MOMO_ID).getBinding().isLockedByIntimacy());
		Assertions.assertTrue(bindingService.canReceiveRandomQuest(first, MOMO_ID));
		Assertions.assertFalse(bindingService.canReceiveRandomQuest(second, MOMO_ID));
	}

	/**
	 * 验证绑定玩家降级后解除绑定喵~
	 */
	@Test
	void boundPlayerDowngradeClearsBinding() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(500);
		bindingService.updateBindingAfterAffectionChange(playerUuid, MOMO_ID, 1L);

		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(499);
		bindingService.updateBindingAfterAffectionChange(playerUuid, MOMO_ID, 2L);

		Assertions.assertNull(data.getCharacterState(MOMO_ID).getBinding().getBoundPlayerUuid());
		Assertions.assertTrue(bindingService.canReceiveGift(UUID.fromString("00000000-0000-0000-0000-000000000015"), MOMO_ID));
	}

	/**
	 * 验证非绑定玩家不能确认亲密并抢占锁定喵~
	 */
	@Test
	void nonBoundPlayerCannotConfirmIntimacy() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BindingService bindingService = new BindingService(data, relationshipService);
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000016");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000017");
		data.getOrCreateRelation(first, MOMO_ID).setAffection(500);
		data.getOrCreateRelation(second, MOMO_ID).setAffection(900);
		bindingService.updateBindingAfterAffectionChange(first, MOMO_ID, 1L);

		bindingService.confirmIntimacy(second, MOMO_ID);

		Assertions.assertFalse(data.getOrCreateRelation(second, MOMO_ID).isConfirmedIntimacy());
		Assertions.assertEquals(first, data.getCharacterState(MOMO_ID).getBinding().getBoundPlayerUuid());
		Assertions.assertFalse(data.getCharacterState(MOMO_ID).getBinding().isLockedByIntimacy());
	}
}
