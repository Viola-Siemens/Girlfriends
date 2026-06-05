package com.hexagram2021.girlfriends.common.death;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.home.HomeService;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.quest.QuestInstance;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * CharacterRespawnService 行为测试喵~
 *
 * @author liudongyu
 */
class CharacterRespawnServiceTest {
	private static final Identifier MEISHU_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "meishu");
	private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

	/**
	 * 验证死亡清理角色状态但保留终章奖励喵~
	 */
	@Test
	void deathClearsCharacterStateButKeepsFinalReward() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		HomeService homeService = new HomeService(data, relationshipService, bedPos -> true);
		CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000015");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, MEISHU_ID);
		relation.setAffection(900);
		relation.setConfirmedIntimacy(true);
		relation.setClaimedFinalReward(true);
		relation.getCompletedFixedQuests().add(10);
		homeService.inviteHome(playerUuid, MEISHU_ID, "minecraft:overworld", 10, 64, 10);
		CharacterWorldState state = data.getCharacterState(MEISHU_ID);
		state.setCurrentQuest(new QuestInstance());
		state.getBinding().setBoundPlayerUuid(playerUuid);
		state.getBinding().setLockedByIntimacy(true);

		RespawnResult result = service.handleCharacterDeath(MEISHU_ID, "minecraft:overworld", 10, 64, 10);

		PlayerCharacterRelation restored = data.getOrCreateRelation(playerUuid, MEISHU_ID);
		Assertions.assertFalse(result.respawned());
		Assertions.assertTrue(result.pendingRespawn());
		Assertions.assertEquals(0, restored.getAffection());
		Assertions.assertFalse(restored.isConfirmedIntimacy());
		Assertions.assertTrue(restored.isClaimedFinalReward());
		Assertions.assertTrue(restored.getCompletedFixedQuests().isEmpty());
		Assertions.assertFalse(restored.isHomePartner());
		Assertions.assertFalse(data.getOrCreateHomeState(playerUuid).isActive());
		Assertions.assertNull(state.getCurrentQuest());
		Assertions.assertNull(state.getBinding().getBoundPlayerUuid());
		Assertions.assertFalse(state.getBinding().isLockedByIntimacy());
		Assertions.assertFalse(state.isAlive());
		Assertions.assertTrue(state.isPendingRespawn());
		Assertions.assertEquals("minecraft:overworld", state.getDeathDimensionId());
		Assertions.assertEquals(10, state.getDeathX());
		Assertions.assertEquals(64, state.getDeathY());
		Assertions.assertEquals(10, state.getDeathZ());
	}

	/**
	 * 验证最近庇护所会被选为重生点喵~
	 */
	@Test
	void nearestShelterIsSelectedForRespawn() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);
		service.registerShelter(MOMO_ID, "minecraft:overworld", 0, 64, 0, 1L);
		service.registerShelter(MOMO_ID, "minecraft:overworld", 100, 64, 100, 1L);

		RespawnResult result = service.handleCharacterDeath(MOMO_ID, "minecraft:overworld", 10, 64, 10);

		Assertions.assertTrue(result.respawned());
		Assertions.assertFalse(result.pendingRespawn());
		Assertions.assertNotNull(result.shelterRecord());
		Assertions.assertEquals(0, result.shelterRecord().getX());
		Assertions.assertTrue(data.getCharacterState(MOMO_ID).isAlive());
		Assertions.assertFalse(data.getCharacterState(MOMO_ID).isPendingRespawn());
	}

	/**
	 * 验证没有庇护所时进入待重生状态喵~
	 */
	@Test
	void deathWithoutShelterKeepsCharacterPendingRespawn() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);

		RespawnResult result = service.handleCharacterDeath(MOMO_ID, "minecraft:overworld", 10, 64, 10);

		CharacterWorldState state = data.getCharacterState(MOMO_ID);
		Assertions.assertFalse(result.respawned());
		Assertions.assertTrue(result.pendingRespawn());
		Assertions.assertNull(result.shelterRecord());
		Assertions.assertFalse(state.isAlive());
		Assertions.assertTrue(state.isPendingRespawn());
	}

	/**
	 * 验证待重生角色在发现庇护所后可恢复存活喵~
	 */
	@Test
	void pendingCharacterRespawnsAfterShelterRegistration() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		CharacterRespawnService service = new CharacterRespawnService(data, relationshipService);
		service.handleCharacterDeath(MOMO_ID, "minecraft:overworld", 10, 64, 10);

		service.registerShelter(MOMO_ID, "minecraft:overworld", 0, 64, 0, 1L);
		service.tryRespawnPendingCharacter(MOMO_ID);

		CharacterWorldState state = data.getCharacterState(MOMO_ID);
		Assertions.assertTrue(state.isAlive());
		Assertions.assertFalse(state.isPendingRespawn());
	}
}
