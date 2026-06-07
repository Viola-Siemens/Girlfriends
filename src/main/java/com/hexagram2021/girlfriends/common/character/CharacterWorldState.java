package com.hexagram2021.girlfriends.common.character;

import com.google.common.collect.Lists;
import com.hexagram2021.girlfriends.common.binding.CharacterBindingState;
import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.death.ShelterRecord;
import com.hexagram2021.girlfriends.common.quest.QuestInstance;
import net.minecraft.IdentifierException;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 角色世界状态喵~
 *
 * @author liudongyu
 */
public class CharacterWorldState {
	public static final int DATA_VERSION = 1;

	@Nullable
	private Identifier characterId;
	@Nullable
	private UUID entityUuid;
	private boolean alive = true;
	private boolean pendingRespawn;
	@Nullable
	private GlobalPos deathPos;
	@Nullable
	private Identifier deathDimension;
	private int deathX;
	private int deathY;
	private int deathZ;
	@Nullable
	private QuestInstance currentQuest;
	private CharacterBindingState binding = new CharacterBindingState();
	@Nullable
	private UUID followTargetUuid;
	private FollowMode followMode = FollowMode.STAY;
	private final List<ShelterRecord> discoveredShelters = Lists.newArrayList();

	/**
	 * 获取角色 ID 喵~
	 *
	 * @return 角色 ID 喵~
	 */
	@Nullable
	public Identifier getCharacterId() {
		return this.characterId;
	}

	/**
	 * 设置角色 ID 喵~
	 *
	 * @param characterId 角色 ID 喵~
	 */
	public void setCharacterId(@Nullable Identifier characterId) {
		this.characterId = characterId;
	}

	/**
	 * 获取实体 UUID 喵~
	 *
	 * @return 实体 UUID 喵~
	 */
	@Nullable
	public UUID getEntityUuid() {
		return this.entityUuid;
	}

	/**
	 * 设置实体 UUID 喵~
	 *
	 * @param entityUuid 实体 UUID 喵~
	 */
	public void setEntityUuid(@Nullable UUID entityUuid) {
		this.entityUuid = entityUuid;
	}

	/**
	 * 判断当前是否存活喵~
	 *
	 * @return 是否存活喵~
	 */
	public boolean isAlive() {
		return this.alive;
	}

	/**
	 * 设置当前是否存活喵~
	 *
	 * @param alive 是否存活喵~
	 */
	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	/**
	 * 判断是否等待重生喵~
	 *
	 * @return 是否等待重生喵~
	 */
	public boolean isPendingRespawn() {
		return this.pendingRespawn;
	}

	/**
	 * 设置是否等待重生喵~
	 *
	 * @param pendingRespawn 是否等待重生喵~
	 */
	public void setPendingRespawn(boolean pendingRespawn) {
		this.pendingRespawn = pendingRespawn;
	}

	/**
	 * 获取死亡位置喵~
	 *
	 * @return 死亡位置喵~
	 */
	@Nullable
	public GlobalPos getDeathPos() {
		return this.deathPos;
	}

	/**
	 * 设置死亡位置喵~
	 *
	 * @param deathPos 死亡位置喵~
	 */
	public void setDeathPos(@Nullable GlobalPos deathPos) {
		this.deathPos = deathPos;
	}

	/**
	 * 获取死亡维度 ID 喵~
	 *
	 * @return 死亡维度 ID 喵~
	 */
	@Nullable
	public Identifier getDeathDimensionId() {
		return this.deathDimension;
	}

	/**
	 * 设置死亡维度 ID 喵~
	 *
	 * @param deathDimension 死亡维度 ID 喵~
	 */
	public void setDeathDimensionId(@Nullable Identifier deathDimension) {
		this.deathDimension = deathDimension;
	}

	/**
	 * 获取死亡 X 坐标喵~
	 *
	 * @return 死亡 X 坐标喵~
	 */
	public int getDeathX() {
		return this.deathX;
	}

	/**
	 * 设置死亡 X 坐标喵~
	 *
	 * @param deathX 死亡 X 坐标喵~
	 */
	public void setDeathX(int deathX) {
		this.deathX = deathX;
	}

	/**
	 * 获取死亡 Y 坐标喵~
	 *
	 * @return 死亡 Y 坐标喵~
	 */
	public int getDeathY() {
		return this.deathY;
	}

	/**
	 * 设置死亡 Y 坐标喵~
	 *
	 * @param deathY 死亡 Y 坐标喵~
	 */
	public void setDeathY(int deathY) {
		this.deathY = deathY;
	}

	/**
	 * 获取死亡 Z 坐标喵~
	 *
	 * @return 死亡 Z 坐标喵~
	 */
	public int getDeathZ() {
		return this.deathZ;
	}

	/**
	 * 设置死亡 Z 坐标喵~
	 *
	 * @param deathZ 死亡 Z 坐标喵~
	 */
	public void setDeathZ(int deathZ) {
		this.deathZ = deathZ;
	}

	/**
	 * 获取当前委托喵~
	 *
	 * @return 当前委托喵~
	 */
	@Nullable
	public QuestInstance getCurrentQuest() {
		return this.currentQuest;
	}

	/**
	 * 设置当前委托喵~
	 *
	 * @param currentQuest 当前委托喵~
	 */
	public void setCurrentQuest(@Nullable QuestInstance currentQuest) {
		this.currentQuest = currentQuest;
	}

	/**
	 * 获取绑定状态喵~
	 *
	 * @return 绑定状态喵~
	 */
	public CharacterBindingState getBinding() {
		return this.binding;
	}

	/**
	 * 设置绑定状态喵~
	 *
	 * @param binding 绑定状态喵~
	 */
	public void setBinding(CharacterBindingState binding) {
		this.binding = binding;
	}

	/**
	 * 获取跟随目标玩家 UUID 喵~
	 *
	 * @return 跟随目标玩家 UUID 喵~
	 */
	@Nullable
	public UUID getFollowTargetUuid() {
		return this.followTargetUuid;
	}

	/**
	 * 设置跟随目标玩家 UUID 喵~
	 *
	 * @param followTargetUuid 跟随目标玩家 UUID 喵~
	 */
	public void setFollowTargetUuid(@Nullable UUID followTargetUuid) {
		this.followTargetUuid = followTargetUuid;
	}

	/**
	 * 获取跟随模式喵~
	 *
	 * @return 跟随模式喵~
	 */
	public FollowMode getFollowMode() {
		return this.followMode;
	}

	/**
	 * 设置跟随模式喵~
	 *
	 * @param followMode 跟随模式喵~
	 */
	public void setFollowMode(FollowMode followMode) {
		this.followMode = followMode;
	}

	/**
	 * 获取已发现庇护所列表喵~
	 *
	 * @return 已发现庇护所列表喵~
	 */
	public List<ShelterRecord> getDiscoveredShelters() {
		return this.discoveredShelters;
	}

	/**
	 * 序列化角色世界状态喵~
	 *
	 * @return NBT 数据喵~
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		if(this.characterId != null) {
			tag.putString("character_id", this.characterId.toString());
		}
		if(this.entityUuid != null) {
			tag.putString("entity_uuid", this.entityUuid.toString());
		}
		tag.putBoolean("alive", this.alive);
		tag.putBoolean("pending_respawn", this.pendingRespawn);
		if(this.deathPos != null) {
			GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.deathPos).result().ifPresent(value -> tag.put("death_pos", value));
		}
		if(this.deathDimension != null) {
			tag.putString("death_dimension_id", this.deathDimension.toString());
		}
		tag.putInt("death_x", this.deathX);
		tag.putInt("death_y", this.deathY);
		tag.putInt("death_z", this.deathZ);
		if(this.currentQuest != null) {
			tag.put("current_quest", this.currentQuest.serializeNBT());
		}
		tag.put("binding", this.binding.serializeNBT());
		if(this.followTargetUuid != null) {
			tag.putString("follow_target_uuid", this.followTargetUuid.toString());
		}
		tag.putString("follow_mode", this.followMode.name());
		ListTag shelters = new ListTag();
		for(ShelterRecord shelterRecord : this.discoveredShelters) {
			shelters.add(shelterRecord.serializeNBT());
		}
		tag.put("discovered_shelters", shelters);
		return tag;
	}

	/**
	 * 反序列化角色世界状态喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 角色世界状态喵~
	 */
	public static CharacterWorldState deserializeNBT(CompoundTag tag) {
		CharacterWorldState state = new CharacterWorldState();
		tag.getString("character_id").ifPresent(value -> state.characterId = parseIdentifierOrNull(value));
		tag.getString("entity_uuid").ifPresent(value -> state.entityUuid = parseUuidOrNull(value));
		state.alive = tag.getBoolean("alive").orElse(true);
		state.pendingRespawn = tag.getBoolean("pending_respawn").orElse(false);
		if(tag.get("death_pos") != null) {
			GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("death_pos")).result().ifPresent(pos -> state.deathPos = pos);
		}
		state.deathDimension = tag.getString("death_dimension_id").map(Identifier::parse).orElse(null);
		state.deathX = tag.getInt("death_x").orElse(0);
		state.deathY = tag.getInt("death_y").orElse(0);
		state.deathZ = tag.getInt("death_z").orElse(0);
		if(tag.getCompound("current_quest").isPresent()) {
			state.currentQuest = QuestInstance.deserializeNBT(tag.getCompoundOrEmpty("current_quest"));
		}
		state.binding = CharacterBindingState.deserializeNBT(tag.getCompoundOrEmpty("binding"));
		tag.getString("follow_target_uuid").ifPresent(value -> state.followTargetUuid = parseUuidOrNull(value));
		state.followMode = tag.getString("follow_mode").map(CharacterWorldState::parseFollowModeOrStay).orElse(FollowMode.STAY);
		ListTag shelters = tag.getListOrEmpty("discovered_shelters");
		for (Tag shelter : shelters) {
			if (shelter instanceof CompoundTag shelterTag) {
				state.discoveredShelters.add(ShelterRecord.deserializeNBT(shelterTag));
			}
		}
		return state;
	}

	@Nullable
	private static Identifier parseIdentifierOrNull(String value) {
		try {
			return Identifier.parse(value);
		} catch(IdentifierException _) {
			return null;
		}
	}

	@Nullable
	private static UUID parseUuidOrNull(String value) {
		try {
			return UUID.fromString(value);
		} catch(IllegalArgumentException _) {
			return null;
		}
	}

	private static FollowMode parseFollowModeOrStay(String value) {
		try {
			return FollowMode.valueOf(value);
		} catch(IllegalArgumentException _) {
			return FollowMode.STAY;
		}
	}
}
