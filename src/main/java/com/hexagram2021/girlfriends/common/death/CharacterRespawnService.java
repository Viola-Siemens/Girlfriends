package com.hexagram2021.girlfriends.common.death;

import com.hexagram2021.girlfriends.common.binding.CharacterBindingState;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.home.HomeState;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 角色死亡与重生规则服务喵~
 *
 * @author liudongyu
 */
public class CharacterRespawnService {
	private final GirlfriendsWorldData worldData;
	private final RelationshipService relationshipService;

	/**
	 * 创建角色死亡与重生规则服务喵~
	 *
	 * @param worldData 世界数据喵~
	 * @param relationshipService 关系服务喵~
	 */
	public CharacterRespawnService(GirlfriendsWorldData worldData, RelationshipService relationshipService) {
		this.worldData = worldData;
		this.relationshipService = relationshipService;
	}

	/**
	 * 处理角色死亡喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param dimension 维度 ID 喵~
	 * @param x 死亡位置 X 坐标喵~
	 * @param y 死亡位置 Y 坐标喵~
	 * @param z 死亡位置 Z 坐标喵~
	 * @return 重生处理结果喵~
	 */
	public RespawnResult handleCharacterDeath(Identifier girlfriendTypeId, Identifier dimension, int x, int y, int z) {
		Optional<ShelterRecord> nearestShelter = this.findNearestShelter(girlfriendTypeId, dimension, x, y, z);
		this.relationshipService.resetCharacterRelations(girlfriendTypeId);
		this.releaseHomeStates(girlfriendTypeId);
		this.worldData.updateCharacter(girlfriendTypeId, state -> {
			state.setCurrentQuest(null);
			state.setBinding(new CharacterBindingState());
			state.setAlive(nearestShelter.isPresent());
			state.setPendingRespawn(nearestShelter.isEmpty());
			state.setDeathDimensionId(dimension);
			state.setDeathX(x);
			state.setDeathY(y);
			state.setDeathZ(z);
			state.setEntityUuid(null);
		});
		return nearestShelter.map(shelterRecord -> new RespawnResult(true, false, shelterRecord))
				.orElseGet(() -> new RespawnResult(false, true, null));
	}

	/**
	 * 注册角色庇护所喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param dimension 维度 ID 喵~
	 * @param x 庇护所 X 坐标喵~
	 * @param y 庇护所 Y 坐标喵~
	 * @param z 庇护所 Z 坐标喵~
	 * @param currentDay 当前游戏日喵~
	 * @return 庇护所记录喵~
	 */
	public ShelterRecord registerShelter(Identifier girlfriendTypeId, Identifier dimension, int x, int y, int z, long currentDay) {
		ShelterRecord shelterRecord = new ShelterRecord();
		shelterRecord.setStructureId(girlfriendTypeId);
		shelterRecord.setDimension(dimension);
		shelterRecord.setX(x);
		shelterRecord.setY(y);
		shelterRecord.setZ(z);
		shelterRecord.setRegisteredDay(currentDay);
		shelterRecord.setGenerated(true);
		shelterRecord.setDiscovered(true);
		this.worldData.updateCharacter(girlfriendTypeId, state -> state.getDiscoveredShelters().add(shelterRecord));
		return shelterRecord;
	}

	/**
	 * 查找最近庇护所喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param dimension 维度 ID 喵~
	 * @param x 当前位置 X 坐标喵~
	 * @param y 当前位置 Y 坐标喵~
	 * @param z 当前位置 Z 坐标喵~
	 * @return 最近庇护所喵~
	 */
	public Optional<ShelterRecord> findNearestShelter(Identifier girlfriendTypeId, Identifier dimension, int x, int y, int z) {
		CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
		if(state == null) {
			return Optional.empty();
		}
		ShelterRecord nearest = null;
		long nearestDistance = Long.MAX_VALUE;
		for(ShelterRecord shelterRecord : state.getDiscoveredShelters()) {
			if(!shelterRecord.isDiscovered() || !dimension.equals(shelterRecord.getDimension())) {
				continue;
			}
			long distance = squaredDistance(shelterRecord, x, y, z);
			if(distance < nearestDistance) {
				nearest = shelterRecord;
				nearestDistance = distance;
			}
		}
		return Optional.ofNullable(nearest);
	}

	/**
	 * 尝试重生等待中的角色喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 */
	public void tryRespawnPendingCharacter(Identifier girlfriendTypeId) {
		CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
		if(state == null || !state.isPendingRespawn()) {
			return;
		}
		Optional<ShelterRecord> shelterRecord = state.getDiscoveredShelters().stream().filter(ShelterRecord::isDiscovered).findFirst();
		if(shelterRecord.isEmpty()) {
			return;
		}
		this.worldData.updateCharacter(girlfriendTypeId, current -> {
			current.setAlive(true);
			current.setPendingRespawn(false);
		});
	}

	private void releaseHomeStates(Identifier girlfriendTypeId) {
		for(Map.Entry<UUID, HomeState> entry : this.worldData.getHomes().entrySet()) {
			HomeState homeState = entry.getValue();
			if(!homeState.isActive() || !girlfriendTypeId.equals(homeState.getCharacterId())) {
				continue;
			}
			this.worldData.updateHome(entry.getKey(), state -> {
				state.setCharacterId(null);
				state.setBedPos(null);
				state.setHomeAnchor(null);
				state.setActive(false);
			});
		}
	}

	private static long squaredDistance(ShelterRecord shelterRecord, int x, int y, int z) {
		long dx = (long)shelterRecord.getX() - x;
		long dy = (long)shelterRecord.getY() - y;
		long dz = (long)shelterRecord.getZ() - z;
		return dx * dx + dy * dy + dz * dz;
	}
}
