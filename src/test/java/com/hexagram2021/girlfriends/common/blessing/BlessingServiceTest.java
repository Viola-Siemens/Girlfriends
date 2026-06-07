package com.hexagram2021.girlfriends.common.blessing;

import com.google.gson.JsonObject;
import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.character.DimensionPolicy;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * BlessingService 行为测试喵~
 *
 * @author liudongyu
 */
class BlessingServiceTest {
	private static final Identifier NATURE_PEACE_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "nature_peace");
	private static final Identifier MELEE_AND_DEFENSE_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "melee_and_defense");
	private static final Identifier WANYING_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying");

	/**
	 * 验证祝福需要亲密确认与跟随状态喵~
	 */
	@Test
	void blessingRequiresConfirmedIntimacyAndFollowMode() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BlessingService service = new BlessingService(data, relationshipService, BlessingServiceTest::resolveGirlfriendType);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, WANYING_ID);
		relation.setAffection(700);
		relation.setConfirmedIntimacy(true);
		data.getCharacterState(WANYING_ID).setEntityUuid(UUID.fromString("00000000-0000-0000-0000-000000000015"));
		data.getCharacterState(WANYING_ID).setFollowTargetUuid(playerUuid);
		data.getCharacterState(WANYING_ID).setFollowMode(FollowMode.FOLLOW);
		data.getCharacterState(WANYING_ID).setAlive(true);

		Assertions.assertTrue(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 16.0D));
		Assertions.assertFalse(service.hasActiveBlessing(playerUuid, WANYING_ID, false, 16.0D));
		Assertions.assertFalse(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 40.0D));
	}

	/**
	 * 验证祝福类型通过角色类型解析喵~
	 */
	@Test
	void blessingUsesGirlfriendTypeResolver() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BlessingService service = new BlessingService(data, relationshipService, girlfriendTypeId -> Optional.of(new GirlfriendType(
				"girlfriends.girlfriend_type.wanying",
				new DimensionPolicy(Set.of(Level.OVERWORLD.identifier())),
				Identifier.withDefaultNamespace("iron_sword"),
				MELEE_AND_DEFENSE_ID,
				Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying_shelter")
		)));
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, WANYING_ID);
		relation.setAffection(700);
		relation.setConfirmedIntimacy(true);
		data.getCharacterState(WANYING_ID).setEntityUuid(UUID.fromString("00000000-0000-0000-0000-000000000015"));
		data.getCharacterState(WANYING_ID).setFollowTargetUuid(playerUuid);
		data.getCharacterState(WANYING_ID).setFollowMode(FollowMode.FOLLOW);

		Assertions.assertEquals(Optional.of(MELEE_AND_DEFENSE_ID), service.getBlessingTypeId(WANYING_ID));
		Assertions.assertTrue(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 16.0D));
	}

	/**
	 * 验证角色实体无效时不会启用祝福喵~
	 */
	@Test
	void missingCharacterEntityDisablesBlessing() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BlessingService service = new BlessingService(data, relationshipService, BlessingServiceTest::resolveGirlfriendType);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, WANYING_ID);
		relation.setAffection(700);
		relation.setConfirmedIntimacy(true);
		data.getCharacterState(WANYING_ID).setFollowTargetUuid(playerUuid);
		data.getCharacterState(WANYING_ID).setFollowMode(FollowMode.FOLLOW);
		data.getCharacterState(WANYING_ID).setAlive(true);

		Assertions.assertFalse(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 16.0D));
	}

	/**
	 * 验证无法解析角色类型时不会启用祝福喵~
	 */
	@Test
	void missingGirlfriendTypeDisablesBlessing() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		RelationshipService relationshipService = new RelationshipService(data);
		BlessingService service = new BlessingService(data, relationshipService, girlfriendTypeId -> Optional.empty());
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
		PlayerCharacterRelation relation = data.getOrCreateRelation(playerUuid, WANYING_ID);
		relation.setAffection(700);
		relation.setConfirmedIntimacy(true);
		data.getCharacterState(WANYING_ID).setEntityUuid(UUID.fromString("00000000-0000-0000-0000-000000000015"));
		data.getCharacterState(WANYING_ID).setFollowTargetUuid(playerUuid);
		data.getCharacterState(WANYING_ID).setFollowMode(FollowMode.FOLLOW);

		Assertions.assertTrue(service.getBlessingTypeId(WANYING_ID).isEmpty());
		Assertions.assertFalse(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 16.0D));
	}

	/**
	 * 验证跟随字段坏存档会降级为安全默认值喵~
	 */
	@Test
	void invalidFollowFieldsFallbackDuringDeserialization() {
		CompoundTag tag = new CompoundTag();
		tag.putString("character_id", "invalid id");
		tag.putString("entity_uuid", "not-a-uuid");
		tag.putString("follow_target_uuid", "not-a-uuid");
		tag.putString("follow_mode", "UNKNOWN");

		CharacterWorldState state = CharacterWorldState.deserializeNBT(tag);

		Assertions.assertNull(state.getCharacterId());
		Assertions.assertNull(state.getEntityUuid());
		Assertions.assertNull(state.getFollowTargetUuid());
		Assertions.assertEquals(FollowMode.STAY, state.getFollowMode());
	}

	/**
	 * 验证菀莹伤害祝福按乘法修正喵~
	 */
	@Test
	void wanyingDamageModifiersAreMultiplicative() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		BlessingService service = new BlessingService(data, new RelationshipService(data));

		Assertions.assertEquals(14.0D, service.applyMeleeDamageBlessing(10.0D), 0.001D);
		Assertions.assertEquals(7.5D, service.applyIncomingDamageBlessing(10.0D), 0.001D);
	}

	/**
	 * 验证概率祝福使用注入随机值喵~
	 */
	@Test
	void probabilityUsesInjectedRandomValue() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		BlessingService service = new BlessingService(data, new RelationshipService(data));

		Assertions.assertTrue(service.rollTwentyFivePercent(0.24D));
		Assertions.assertFalse(service.rollTwentyFivePercent(0.25D));
	}

	/**
	 * 验证祝福数值从 JSON 参数读取并保留默认值喵~
	 */
	@Test
	void blessingParametersOverrideDefaults() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		JsonObject natureParameter = new JsonObject();
		natureParameter.addProperty("peaceful_wildlife", true);
		JsonObject parameter = new JsonObject();
		parameter.addProperty("boat_speed_multiplier", 2.0D);
		parameter.addProperty("fishing_double_probability", 0.5D);
		parameter.addProperty("mining_extra_drop_probability", 0.75D);
		parameter.addProperty("melee_damage_multiplier", 1.8D);
		parameter.addProperty("incoming_damage_multiplier", 0.5D);
		parameter.addProperty("ender_pearl_conserve_probability", 0.9D);
		BlessingService service = new BlessingService(data, new RelationshipService(data), BlessingServiceTest::resolveGirlfriendType,
				new BlessingParameterManager(Map.of(NATURE_PEACE_ID, natureParameter, MELEE_AND_DEFENSE_ID, parameter)));

		Assertions.assertTrue(service.blocksWildlifeAggro(NATURE_PEACE_ID));
		Assertions.assertFalse(service.blocksWildlifeAggro(WANYING_ID));
		Assertions.assertEquals(20.0D, service.applyBoatSpeedBlessing(MELEE_AND_DEFENSE_ID, 10.0D), 0.001D);
		Assertions.assertEquals(18.0D, service.applyMeleeDamageBlessing(MELEE_AND_DEFENSE_ID, 10.0D), 0.001D);
		Assertions.assertEquals(5.0D, service.applyIncomingDamageBlessing(MELEE_AND_DEFENSE_ID, 10.0D), 0.001D);
		Assertions.assertTrue(service.rollFishingDouble(MELEE_AND_DEFENSE_ID, 0.49D));
		Assertions.assertFalse(service.rollFishingDouble(MELEE_AND_DEFENSE_ID, 0.5D));
		Assertions.assertTrue(service.rollMiningExtraDrop(MELEE_AND_DEFENSE_ID, 0.74D));
		Assertions.assertFalse(service.rollMiningExtraDrop(MELEE_AND_DEFENSE_ID, 0.75D));
		Assertions.assertTrue(service.rollEnderPearlConserve(MELEE_AND_DEFENSE_ID, 0.89D));
		Assertions.assertFalse(service.rollEnderPearlConserve(MELEE_AND_DEFENSE_ID, 0.9D));
		Assertions.assertEquals(15.0D, service.applyBoatSpeedBlessing(WANYING_ID, 10.0D), 0.001D);
	}

	/**
	 * 验证祝福只读判定不会创建缺失的关系或角色状态喵~
	 */
	@Test
	void inactiveBlessingLookupDoesNotCreatePersistentState() {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		BlessingService service = new BlessingService(data, new RelationshipService(data), BlessingServiceTest::resolveGirlfriendType);
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000014");

		Assertions.assertFalse(service.hasActiveBlessing(playerUuid, WANYING_ID, true, 16.0D));
		Assertions.assertTrue(data.getRelations().isEmpty());
		Assertions.assertNull(data.getExistingCharacterState(WANYING_ID));
	}

	private static Optional<GirlfriendType> resolveGirlfriendType(Identifier girlfriendTypeId) {
		if(!WANYING_ID.equals(girlfriendTypeId)) {
			return Optional.empty();
		}
		return Optional.of(new GirlfriendType(
				"girlfriends.girlfriend_type.wanying",
				new DimensionPolicy(Set.of(Level.OVERWORLD.identifier())),
				Identifier.withDefaultNamespace("iron_sword"),
				MELEE_AND_DEFENSE_ID,
				Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "wanying_shelter")
		));
	}
}
