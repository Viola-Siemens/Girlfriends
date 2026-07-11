package com.hexagram2021.girlfriends.common.blessing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.character.GirlfriendsRegistries;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * 祝福规则服务喵~
 *
 * @author liudongyu
 */
public class BlessingService {
	private static final double MAX_BLESSING_DISTANCE = 32.0D;
	private static final String PEACEFUL_WILDLIFE = "peaceful_wildlife";
	private static final String BOAT_SPEED_MULTIPLIER = "boat_speed_multiplier";
	private static final String FISHING_DOUBLE_PROBABILITY = "fishing_double_probability";
	private static final String MINING_EXTRA_DROP_PROBABILITY = "mining_extra_drop_probability";
	private static final String MELEE_DAMAGE_MULTIPLIER = "melee_damage_multiplier";
	private static final String INCOMING_DAMAGE_MULTIPLIER = "incoming_damage_multiplier";
	private static final String ENDER_PEARL_CONSERVE_PROBABILITY = "ender_pearl_conserve_probability";
	private static final double DEFAULT_BOAT_SPEED_MULTIPLIER = 1.5D;
	private static final double DEFAULT_TWENTY_FIVE_PERCENT = 0.25D;
	private static final double DEFAULT_MELEE_DAMAGE_MULTIPLIER = 1.4D;
	private static final double DEFAULT_INCOMING_DAMAGE_MULTIPLIER = 0.75D;

	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;
	private final Function<Identifier, Optional<GirlfriendType>> girlfriendTypeResolver;
	private final BlessingParameterManager blessingParameterManager;

	/**
	 * 创建祝福规则服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 */
	public BlessingService(GirlfriendsWorldData worldData, RelationshipService relationshipService) {
		this(worldData, relationshipService, GirlfriendsRegistries.GIRLFRIEND_TYPE_REGISTRY::getOptional, BlessingParameterManager.INSTANCE);
	}

	/**
	 * 创建祝福规则服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 * @param girlfriendTypeResolver 角色类型解析器喵~
	 */
	public BlessingService(GirlfriendsWorldData worldData, RelationshipService relationshipService,
			Function<Identifier, Optional<GirlfriendType>> girlfriendTypeResolver) {
		this(worldData, relationshipService, girlfriendTypeResolver, BlessingParameterManager.INSTANCE);
	}

	/**
	 * 创建祝福规则服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 * @param girlfriendTypeResolver 角色类型解析器喵~
	 * @param blessingParameterManager 祝福参数管理器喵~
	 */
	public BlessingService(GirlfriendsWorldData worldData, RelationshipService relationshipService,
			Function<Identifier, Optional<GirlfriendType>> girlfriendTypeResolver, BlessingParameterManager blessingParameterManager) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
		this.girlfriendTypeResolver = girlfriendTypeResolver;
		this.blessingParameterManager = blessingParameterManager;
	}

	/**
	 * 判断角色祝福是否对玩家生效喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param sameDimension 是否同维度喵~
	 * @param distance 与角色距离喵~
	 * @return 是否生效喵~
	 */
	public boolean hasActiveBlessing(UUID playerUuid, Identifier girlfriendTypeId, boolean sameDimension, double distance) {
		PlayerCharacterRelation relation = this.relationshipService.getExistingRelation(playerUuid, girlfriendTypeId);
		if(relation == null || !relation.isConfirmedIntimacy()) {
			return false;
		}
		if(this.getBlessingTypeId(girlfriendTypeId).isEmpty()) {
			return false;
		}
		CharacterWorldState characterState = this.worldData.getExistingCharacterState(girlfriendTypeId);
		return characterState != null
				&& characterState.isAlive()
				&& characterState.getEntityUuid() != null
				&& characterState.getFollowMode() == FollowMode.FOLLOW
				&& playerUuid.equals(characterState.getFollowTargetUuid())
				&& sameDimension
				&& distance <= MAX_BLESSING_DISTANCE;
	}

	/**
	 * 获取角色祝福类型 ID 喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 祝福类型 ID 喵~
	 */
	public Optional<Identifier> getBlessingTypeId(Identifier girlfriendTypeId) {
		return this.girlfriendTypeResolver.apply(girlfriendTypeId).map(GirlfriendType::blessingTypeId);
	}

	/**
	 * 判断是否阻止野生动物仇恨喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @return 是否阻止野生动物仇恨喵~
	 */
	public boolean blocksWildlifeAggro(Identifier blessingTypeId) {
		return this.getBooleanParameter(blessingTypeId, PEACEFUL_WILDLIFE, false);
	}

	/**
	 * 应用船速祝福喵~
	 *
	 * @param baseSpeed 基础船速喵~
	 * @return 修正后船速喵~
	 */
	public double applyBoatSpeedBlessing(double baseSpeed) {
		return baseSpeed * DEFAULT_BOAT_SPEED_MULTIPLIER;
	}

	/**
	 * 应用船速祝福喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param baseSpeed 基础船速喵~
	 * @return 修正后船速喵~
	 */
	public double applyBoatSpeedBlessing(Identifier blessingTypeId, double baseSpeed) {
		return baseSpeed * this.getDoubleParameter(blessingTypeId, BOAT_SPEED_MULTIPLIER, DEFAULT_BOAT_SPEED_MULTIPLIER);
	}

	/**
	 * 应用近战伤害祝福喵~
	 *
	 * @param baseDamage 基础伤害喵~
	 * @return 修正后伤害喵~
	 */
	public double applyMeleeDamageBlessing(double baseDamage) {
		return baseDamage * DEFAULT_MELEE_DAMAGE_MULTIPLIER;
	}

	/**
	 * 应用近战伤害祝福喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param baseDamage 基础伤害喵~
	 * @return 修正后伤害喵~
	 */
	public double applyMeleeDamageBlessing(Identifier blessingTypeId, double baseDamage) {
		return baseDamage * this.getDoubleParameter(blessingTypeId, MELEE_DAMAGE_MULTIPLIER, DEFAULT_MELEE_DAMAGE_MULTIPLIER);
	}

	/**
	 * 应用受到伤害祝福喵~
	 *
	 * @param baseDamage 基础伤害喵~
	 * @return 修正后伤害喵~
	 */
	public double applyIncomingDamageBlessing(double baseDamage) {
		return baseDamage * DEFAULT_INCOMING_DAMAGE_MULTIPLIER;
	}

	/**
	 * 应用受到伤害祝福喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param baseDamage 基础伤害喵~
	 * @return 修正后伤害喵~
	 */
	public double applyIncomingDamageBlessing(Identifier blessingTypeId, double baseDamage) {
		return baseDamage * this.getDoubleParameter(blessingTypeId, INCOMING_DAMAGE_MULTIPLIER, DEFAULT_INCOMING_DAMAGE_MULTIPLIER);
	}

	/**
	 * 判定 25% 概率祝福是否成功喵~
	 *
	 * @param randomValue 随机值喵~
	 * @return 是否成功喵~
	 */
	public boolean rollTwentyFivePercent(double randomValue) {
		return randomValue < DEFAULT_TWENTY_FIVE_PERCENT;
	}

	/**
	 * 判定钓鱼双倍祝福是否成功喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param randomValue 随机值喵~
	 * @return 是否成功喵~
	 */
	public boolean rollFishingDouble(Identifier blessingTypeId, double randomValue) {
		return randomValue < this.getDoubleParameter(blessingTypeId, FISHING_DOUBLE_PROBABILITY, DEFAULT_TWENTY_FIVE_PERCENT);
	}

	/**
	 * 判定挖矿额外掉落祝福是否成功喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param randomValue 随机值喵~
	 * @return 是否成功喵~
	 */
	public boolean rollMiningExtraDrop(Identifier blessingTypeId, double randomValue) {
		return randomValue < this.getDoubleParameter(blessingTypeId, MINING_EXTRA_DROP_PROBABILITY, DEFAULT_TWENTY_FIVE_PERCENT);
	}

	/**
	 * 判定末影珍珠保留祝福是否成功喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param randomValue 随机值喵~
	 * @return 是否成功喵~
	 */
	public boolean rollEnderPearlConserve(Identifier blessingTypeId, double randomValue) {
		return randomValue < this.getDoubleParameter(blessingTypeId, ENDER_PEARL_CONSERVE_PROBABILITY, DEFAULT_TWENTY_FIVE_PERCENT);
	}

	private boolean getBooleanParameter(Identifier blessingTypeId, String key, boolean defaultValue) {
		JsonObject jsonObject = this.getParameterObject(blessingTypeId);
		if(jsonObject == null) {
			return defaultValue;
		}
		JsonElement value = jsonObject.get(key);
		if(value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			return defaultValue;
		}
		return value.getAsBoolean();
	}

	private double getDoubleParameter(Identifier blessingTypeId, String key, double defaultValue) {
		JsonObject jsonObject = this.getParameterObject(blessingTypeId);
		if(jsonObject == null) {
			return defaultValue;
		}
		JsonElement value = jsonObject.get(key);
		if(value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			return defaultValue;
		}
		return value.getAsDouble();
	}

	private JsonObject getParameterObject(Identifier blessingTypeId) {
		JsonElement parameter = this.blessingParameterManager.getParameter(blessingTypeId);
		if(parameter == null || !parameter.isJsonObject()) {
			return null;
		}
		return parameter.getAsJsonObject();
	}
}
