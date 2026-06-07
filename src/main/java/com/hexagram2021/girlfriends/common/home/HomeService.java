package com.hexagram2021.girlfriends.common.home;

import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.AffectionChangeSource;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;

import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * 家园规则服务喵~
 *
 * @author liudongyu
 */
public class HomeService {
	private static final float HOME_AFFECTION_DELTA = 2.0F;
	private static final int REQUIRED_FIXED_QUEST_INDEX = 10;

	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;
	private final BedValidator bedValidator;

	/**
	 * 创建家园规则服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 * @param bedValidator 双人床校验器喵~
	 */
	public HomeService(GirlfriendsWorldData worldData, RelationshipService relationshipService, BedValidator bedValidator) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
		this.bedValidator = bedValidator;
	}

	/**
	 * 邀请角色成为家园伙伴喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param dimension 维度 ID 喵~
	 * @param x 床位 X 坐标喵~
	 * @param y 床位 Y 坐标喵~
	 * @param z 床位 Z 坐标喵~
	 * @return 是否邀请成功喵~
	 */
	public boolean inviteHome(UUID playerUuid, Identifier girlfriendTypeId, Identifier dimension, int x, int y, int z) {
		HomeState homeState = this.worldData.getOrCreateHomeState(playerUuid);
		if(homeState.isActive()) {
			return false;
		}
		HomeAnchor homeAnchor = new HomeAnchor(dimension, x, y, z);
		if(!this.bedValidator.isValid(homeAnchor)) {
			return false;
		}
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		if(this.relationshipService.getNumericStage(relation.getAffection()).ordinal() < AffectionStage.HOME_PARTNER.ordinal()) {
			return false;
		}
		if(!relation.isConfirmedIntimacy() || !relation.getCompletedFixedQuests().contains(REQUIRED_FIXED_QUEST_INDEX)) {
			return false;
		}
		this.worldData.updateHome(playerUuid, state -> {
			state.setCharacterId(girlfriendTypeId);
			state.setHomeAnchor(homeAnchor);
			state.setActive(true);
		});
		this.worldData.updateRelation(playerUuid, girlfriendTypeId, updatedRelation -> updatedRelation.setHomePartner(true));
		return true;
	}

	/**
	 * 解除家园伙伴喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @return 是否解除成功喵~
	 */
	public boolean releaseHomePartner(UUID playerUuid) {
		HomeState homeState = this.worldData.getOrCreateHomeState(playerUuid);
		if(!homeState.isActive() || homeState.getCharacterId() == null) {
			return false;
		}
		Identifier girlfriendTypeId = homeState.getCharacterId();
		this.worldData.updateHome(playerUuid, state -> {
			state.setCharacterId(null);
			state.setBedPos(null);
			state.setHomeAnchor(null);
			state.setActive(false);
		});
		this.worldData.updateRelation(playerUuid, girlfriendTypeId, relation -> relation.setHomePartner(false));
		return true;
	}

	/**
	 * 应用家园日常收益喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param playerNearBed 玩家是否在床边喵~
	 * @param partnerNearBed 家园伙伴是否在床边喵~
	 * @return 家园日常收益结果喵~
	 */
	public HomeTickResult applyHomeBenefit(UUID playerUuid, boolean playerNearBed, boolean partnerNearBed) {
		HomeState homeState = this.worldData.getOrCreateHomeState(playerUuid);
		if(!homeState.isActive() || homeState.getCharacterId() == null || !playerNearBed || !partnerNearBed) {
			return new HomeTickResult(false, 0.0F);
		}
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, homeState.getCharacterId());
		if(relation.isDailyHomeGainClaimed()) {
			return new HomeTickResult(false, 0.0F);
		}
		this.relationshipService.changeAffection(playerUuid, homeState.getCharacterId(), AffectionChangeSource.HOME_DAILY, HOME_AFFECTION_DELTA);
		this.worldData.updateRelation(playerUuid, homeState.getCharacterId(), updatedRelation -> updatedRelation.setDailyHomeGainClaimed(true));
		return new HomeTickResult(true, HOME_AFFECTION_DELTA);
	}

	/**
	 * 触发家园争执喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param visitorTypeId 访客角色类型 ID 喵~
	 * @param penaltySupplier 扣减好感提供器喵~
	 * @return 家园争执结果喵~
	 */
	public HomeConflictResult triggerConflict(UUID playerUuid, Identifier visitorTypeId, IntSupplier penaltySupplier) {
		HomeState homeState = this.worldData.getOrCreateHomeState(playerUuid);
		Identifier homePartnerTypeId = homeState.getCharacterId();
		if(!homeState.isActive() || homePartnerTypeId == null || homePartnerTypeId.equals(visitorTypeId)) {
			return new HomeConflictResult(false, 0.0F, 0.0F);
		}
		PlayerCharacterRelation homePartnerRelation = this.relationshipService.getRelation(playerUuid, homePartnerTypeId);
		PlayerCharacterRelation visitorRelation = this.relationshipService.getRelation(playerUuid, visitorTypeId);
		if(!visitorRelation.isConfirmedIntimacy() || homePartnerRelation.isDailyConflictTriggered()) {
			return new HomeConflictResult(false, 0.0F, 0.0F);
		}
		int penalty = Math.clamp(penaltySupplier.getAsInt(), 3, 5);
		int delta = -penalty;
		this.relationshipService.changeAffection(playerUuid, homePartnerTypeId, AffectionChangeSource.HOME_CONFLICT, delta);
		this.relationshipService.changeAffection(playerUuid, visitorTypeId, AffectionChangeSource.HOME_CONFLICT, delta);
		this.worldData.updateRelation(playerUuid, homePartnerTypeId, relation -> relation.setDailyConflictTriggered(true));
		return new HomeConflictResult(true, delta, delta);
	}
}
