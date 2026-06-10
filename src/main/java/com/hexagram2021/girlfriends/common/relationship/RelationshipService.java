package com.hexagram2021.girlfriends.common.relationship;

import com.google.common.collect.Lists;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家与角色关系服务喵~
 *
 * @param worldData 世界数据喵~
 *
 * @author liudongyu
 */
public record RelationshipService(GirlfriendsWorldData worldData) {
	private static final float MIN_AFFECTION = 0;
	private static final float MAX_AFFECTION = 1000;
	private static final float AFFECTION_THRESHOLD = 700;
	private static final float HOME_PARTNER_THRESHOLD = 900;

	/**
	 * 获取玩家与角色关系喵~
	 *
	 * @param playerUuid       玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 关系状态喵~
	 */
	public PlayerCharacterRelation getRelation(UUID playerUuid, Identifier girlfriendTypeId) {
		return this.worldData.getOrCreateRelation(playerUuid, girlfriendTypeId);
	}

	/**
	 * 查询已存在的玩家与角色关系喵~
	 *
	 * @param playerUuid       玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 已存在的关系状态喵~
	 */
	public PlayerCharacterRelation getExistingRelation(UUID playerUuid, Identifier girlfriendTypeId) {
		return this.worldData.getExistingRelation(playerUuid, girlfriendTypeId);
	}

	/**
	 * 变更好感度并返回最新值喵~
	 *
	 * @param playerUuid       玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param source           变更来源喵~
	 * @param rawDelta         原始变更值喵~
	 * @return 最新好感度喵~
	 */
	public float changeAffection(UUID playerUuid, Identifier girlfriendTypeId, AffectionChangeSource source, float rawDelta) {
		PlayerCharacterRelation relation = this.worldData.updateRelation(playerUuid, girlfriendTypeId, current -> {
			float nextAffection = clampAffection(current.getAffection() + rawDelta);
			current.setAffection(nextAffection);
		});
		return relation.getAffection();
	}

	/**
	 * 按数值推导基础阶段喵~
	 *
	 * @param affection 好感度喵~
	 * @return 基础阶段喵~
	 */
	public AffectionStage getNumericStage(float affection) {
		float clampedAffection = clampAffection(affection);
		if (clampedAffection >= HOME_PARTNER_THRESHOLD) {
			return AffectionStage.HOME_PARTNER;
		}
		if (clampedAffection >= AFFECTION_THRESHOLD) {
			return AffectionStage.INTIMATE;
		}
		if (clampedAffection >= AffectionStage.AFFECTION.getMinAffection()) {
			return AffectionStage.AFFECTION;
		}
		if (clampedAffection >= AffectionStage.TRUST.getMinAffection()) {
			return AffectionStage.TRUST;
		}
		if (clampedAffection >= AffectionStage.FAMILIAR.getMinAffection()) {
			return AffectionStage.FAMILIAR;
		}
		return AffectionStage.STRANGER;
	}

	/**
	 * 按真实关系状态推导有效阶段喵~
	 *
	 * @param relation 关系状态喵~
	 * @return 有效阶段喵~
	 */
	public AffectionStage getEffectiveStage(PlayerCharacterRelation relation) {
		AffectionStage numericStage = this.getNumericStage(relation.getAffection());
		if (numericStage == AffectionStage.HOME_PARTNER) {
			return relation.isHomePartner() ? AffectionStage.HOME_PARTNER : AffectionStage.INTIMATE;
		}
		if (numericStage == AffectionStage.INTIMATE && !relation.isConfirmedIntimacy()) {
			return AffectionStage.AFFECTION;
		}
		return numericStage;
	}

	/**
	 * 按游戏日重置全部关系的每日计数喵~
	 *
	 * @param currentDay 当前游戏日喵~
	 */
	public void resetDailyCounters(long currentDay) {
		List<Map.Entry<RelationKey, PlayerCharacterRelation>> entries = Lists.newArrayList(this.worldData.getRelations().entrySet());
		for (Map.Entry<RelationKey, PlayerCharacterRelation> entry : entries) {
			PlayerCharacterRelation relation = entry.getValue();
			if (relation.getLastDailyResetDay() == currentDay) {
				continue;
			}
			this.worldData.updateRelation(entry.getKey().playerUuid(), entry.getKey().girlfriendTypeId(), current -> {
				if (current.getLastDailyResetDay() == currentDay) {
					return;
				}
				current.setDailyGiftGain(0);
				current.setDailyHomeGainClaimed(false);
				current.setDailyConflictTriggered(false);
				current.setLastDailyResetDay(currentDay);
			});
		}
	}

	/**
	 * 重置指定角色的全部关系进度喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 */
	public void resetCharacterRelations(Identifier girlfriendTypeId) {
		List<Map.Entry<RelationKey, PlayerCharacterRelation>> entries = Lists.newArrayList(this.worldData.getRelations().entrySet());
		for (Map.Entry<RelationKey, PlayerCharacterRelation> entry : entries) {
			if (!entry.getKey().girlfriendTypeId().equals(girlfriendTypeId)) {
				continue;
			}
			boolean claimedFinalReward = entry.getValue().isClaimedFinalReward();
			this.worldData.updateRelation(entry.getKey().playerUuid(), girlfriendTypeId, current -> current.resetProgressPreservingFinalReward(claimedFinalReward));
		}
	}

	private static float clampAffection(float affection) {
		return Math.clamp(affection, MIN_AFFECTION, MAX_AFFECTION);
	}
}
