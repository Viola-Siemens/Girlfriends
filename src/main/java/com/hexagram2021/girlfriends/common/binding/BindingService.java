package com.hexagram2021.girlfriends.common.binding;

import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationKey;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 绑定关系服务喵~
 *
 * @author liudongyu
 */
public class BindingService {
	private static final long WAVERING_DAYS = 3L;

	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;

	/**
	 * 创建绑定关系服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 */
	public BindingService(GirlfriendsWorldData worldData, RelationshipService relationshipService) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
	}

	/**
	 * 在好感变化后更新绑定状态喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param currentDay 当前游戏日喵~
	 */
	public void updateBindingAfterAffectionChange(UUID playerUuid, Identifier girlfriendTypeId, long currentDay) {
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		if(this.relationshipService.getEffectiveStage(relation).ordinal() < AffectionStage.AFFECTION.ordinal()) {
			this.clearIneligibleBinding(playerUuid, girlfriendTypeId);
			return;
		}
		this.worldData.updateCharacter(girlfriendTypeId, character -> {
			CharacterBindingState binding = character.getBinding();
			UUID boundPlayerUuid = binding.getBoundPlayerUuid();
			if(boundPlayerUuid == null) {
				binding.setBoundPlayerUuid(playerUuid);
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
				return;
			}
			if(boundPlayerUuid.equals(playerUuid) || binding.isLockedByIntimacy()) {
				return;
			}
			PlayerCharacterRelation boundRelation = this.relationshipService.getRelation(boundPlayerUuid, girlfriendTypeId);
			if(relation.getAffection() <= boundRelation.getAffection()) {
				if(playerUuid.equals(binding.getChallengerPlayerUuid())) {
					binding.setChallengerPlayerUuid(null);
					binding.setWaveringStartDay(0L);
					binding.setWarnedBoundPlayer(false);
				}
				return;
			}
			UUID challengerPlayerUuid = binding.getChallengerPlayerUuid();
			if(challengerPlayerUuid == null || !challengerPlayerUuid.equals(playerUuid)) {
				binding.setChallengerPlayerUuid(playerUuid);
				binding.setWaveringStartDay(currentDay);
				binding.setWarnedBoundPlayer(false);
			}
		});
	}

	/**
	 * 判断玩家是否能赠礼喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否能赠礼喵~
	 */
	public boolean canReceiveGift(UUID playerUuid, Identifier girlfriendTypeId) {
		return this.canReceiveBoundProgress(playerUuid, girlfriendTypeId);
	}

	/**
	 * 判断玩家是否能推进固定委托喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否能推进固定委托喵~
	 */
	public boolean canReceiveFixedQuest(UUID playerUuid, Identifier girlfriendTypeId) {
		return this.canReceiveBoundProgress(playerUuid, girlfriendTypeId);
	}

	/**
	 * 判断玩家是否能推进随机委托喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否能推进随机委托喵~
	 */
	public boolean canReceiveRandomQuest(UUID playerUuid, Identifier girlfriendTypeId) {
		CharacterBindingState binding = this.worldData.getCharacterState(girlfriendTypeId).getBinding();
		UUID boundPlayerUuid = binding.getBoundPlayerUuid();
		return !binding.isLockedByIntimacy() || boundPlayerUuid == null || boundPlayerUuid.equals(playerUuid);
	}

	/**
	 * 检查并结算动摇期喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param currentDay 当前游戏日喵~
	 */
	public void checkWavering(Identifier girlfriendTypeId, long currentDay) {
		this.worldData.updateCharacter(girlfriendTypeId, character -> {
			CharacterBindingState binding = character.getBinding();
			UUID boundPlayerUuid = binding.getBoundPlayerUuid();
			if(boundPlayerUuid == null || binding.isLockedByIntimacy()) {
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
				return;
			}
			PlayerCharacterRelation boundRelation = this.relationshipService.getRelation(boundPlayerUuid, girlfriendTypeId);
			Optional<PlayerCharacterRelation> topChallenger = this.findTopChallenger(girlfriendTypeId, boundPlayerUuid, boundRelation.getAffection());
			if(topChallenger.isEmpty()) {
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
				return;
			}
			UUID challengerPlayerUuid = topChallenger.get().getPlayerUuid();
			if(!challengerPlayerUuid.equals(binding.getChallengerPlayerUuid())) {
				binding.setChallengerPlayerUuid(challengerPlayerUuid);
				binding.setWaveringStartDay(currentDay);
				binding.setWarnedBoundPlayer(false);
				return;
			}
			if(currentDay - binding.getWaveringStartDay() >= WAVERING_DAYS) {
				binding.setBoundPlayerUuid(challengerPlayerUuid);
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
			}
		});
	}

	/**
	 * 确认亲密关系并锁定绑定喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 */
	public void confirmIntimacy(UUID playerUuid, Identifier girlfriendTypeId) {
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		if(this.relationshipService.getNumericStage(relation.getAffection()).ordinal() < AffectionStage.INTIMATE.ordinal()) {
			return;
		}
		UUID boundPlayerUuid = this.worldData.getCharacterState(girlfriendTypeId).getBinding().getBoundPlayerUuid();
		if(boundPlayerUuid != null && !boundPlayerUuid.equals(playerUuid)) {
			return;
		}
		this.worldData.updateRelation(playerUuid, girlfriendTypeId, updatedRelation -> updatedRelation.setConfirmedIntimacy(true));
		this.worldData.updateCharacter(girlfriendTypeId, character -> {
			CharacterBindingState binding = character.getBinding();
			binding.setBoundPlayerUuid(playerUuid);
			binding.setLockedByIntimacy(true);
			binding.setChallengerPlayerUuid(null);
			binding.setWaveringStartDay(0L);
			binding.setWarnedBoundPlayer(false);
		});
	}

	private void clearIneligibleBinding(UUID playerUuid, Identifier girlfriendTypeId) {
		this.worldData.updateCharacter(girlfriendTypeId, character -> {
			CharacterBindingState binding = character.getBinding();
			if(playerUuid.equals(binding.getBoundPlayerUuid())) {
				binding.setBoundPlayerUuid(null);
				binding.setLockedByIntimacy(false);
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
				return;
			}
			if(playerUuid.equals(binding.getChallengerPlayerUuid())) {
				binding.setChallengerPlayerUuid(null);
				binding.setWaveringStartDay(0L);
				binding.setWarnedBoundPlayer(false);
			}
		});
	}

	private boolean canReceiveBoundProgress(UUID playerUuid, Identifier girlfriendTypeId) {
		UUID boundPlayerUuid = this.worldData.getCharacterState(girlfriendTypeId).getBinding().getBoundPlayerUuid();
		return boundPlayerUuid == null || boundPlayerUuid.equals(playerUuid);
	}

	private Optional<PlayerCharacterRelation> findTopChallenger(Identifier girlfriendTypeId, UUID boundPlayerUuid, int boundAffection) {
		return this.worldData.getRelations().entrySet().stream()
				.filter(entry -> this.isChallengerRelation(entry, girlfriendTypeId, boundPlayerUuid, boundAffection))
				.map(Map.Entry::getValue)
				.max(Comparator.comparingInt(PlayerCharacterRelation::getAffection));
	}

	private boolean isChallengerRelation(
			Map.Entry<RelationKey, PlayerCharacterRelation> entry, Identifier girlfriendTypeId, UUID boundPlayerUuid, int boundAffection
	) {
		RelationKey key = entry.getKey();
		return key.girlfriendTypeId().equals(girlfriendTypeId) && !key.playerUuid().equals(boundPlayerUuid) && entry.getValue().getAffection() > boundAffection;
	}
}
