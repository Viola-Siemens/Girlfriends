package com.hexagram2021.girlfriends.common.quest;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * QuestService 行为测试喵~
 *
 * @author liudongyu
 */
class QuestServiceTest {
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	/**
	 * 验证单个角色同一时间只能拥有一个委托槽喵~
	 */
	@Test
	void characterOnlyHasOneQuestSlot() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService service = new QuestService(data, relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000008");
		data.getOrCreateRelation(playerUuid, MOMO_ID).setAffection(150);

		boolean first = service.publishFixedQuest(playerUuid, MOMO_ID, 1, 10L);
		boolean second = service.publishFixedQuest(playerUuid, MOMO_ID, 1, 10L);

		Assertions.assertTrue(first);
		Assertions.assertFalse(second);
		Assertions.assertNotNull(data.getCharacterState(MOMO_ID).getCurrentQuest());
	}

	/**
	 * 验证已被首位玩家接取的委托不能被第二位玩家抢走喵~
	 */
	@Test
	void secondPlayerCannotStealAcceptedQuest() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService service = new QuestService(data, relationshipService);
		UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000018");
		UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000019");
		data.getOrCreateRelation(firstPlayer, MOMO_ID).setAffection(150);
		data.getOrCreateRelation(secondPlayer, MOMO_ID).setAffection(150);
		service.publishFixedQuest(firstPlayer, MOMO_ID, 1, 10L);

		Assertions.assertTrue(service.acceptCurrentQuest(firstPlayer, MOMO_ID));
		Assertions.assertFalse(service.acceptCurrentQuest(secondPlayer, MOMO_ID));
		Assertions.assertEquals(firstPlayer, data.getCharacterState(MOMO_ID).getCurrentQuest().getOwnerPlayerUuid());
	}

	/**
	 * 验证空槽位会刷新随机委托喵~
	 */
	@Test
	void refreshRandomQuestCreatesQuestWhenSlotIsEmpty() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService service = new QuestService(data, relationshipService, id -> null, id -> null, () -> 11);

		Assertions.assertTrue(service.refreshRandomQuest(MOMO_ID, 100L));
		Assertions.assertNotNull(data.getCharacterState(MOMO_ID).getCurrentQuest());
		Assertions.assertEquals(QuestType.RANDOM, data.getCharacterState(MOMO_ID).getCurrentQuest().getQuestType());
		Assertions.assertEquals(111L, data.getCharacterState(MOMO_ID).getCurrentQuest().getExpireDay());
	}

	/**
	 * 验证随机委托到期后会清空槽位喵~
	 */
	@Test
	void expireRandomQuestClearsExpiredQuest() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService service = new QuestService(data, relationshipService, id -> null, id -> null, () -> 11);

		Assertions.assertTrue(service.refreshRandomQuest(MOMO_ID, 100L));
		Assertions.assertTrue(service.expireRandomQuest(MOMO_ID, 111L));
		Assertions.assertNull(data.getCharacterState(MOMO_ID).getCurrentQuest());
	}

	/**
	 * 验证随机委托不会覆盖已接取的固定委托喵~
	 */
	@Test
	void refreshRandomQuestDoesNotReplaceAcceptedFixedQuest() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		QuestService service = new QuestService(data, relationshipService, id -> null, id -> null, () -> 11);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000028");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, MOMO_ID);
		relation.setAffection(150);

		Assertions.assertTrue(service.publishFixedQuest(playerUuid, MOMO_ID, 1, 100L));
		Assertions.assertTrue(service.acceptCurrentQuest(playerUuid, MOMO_ID));
		QuestInstance currentQuest = data.getCharacterState(MOMO_ID).getCurrentQuest();

		Assertions.assertFalse(service.refreshRandomQuest(MOMO_ID, 100L));
		Assertions.assertSame(currentQuest, data.getCharacterState(MOMO_ID).getCurrentQuest());
	}
}
