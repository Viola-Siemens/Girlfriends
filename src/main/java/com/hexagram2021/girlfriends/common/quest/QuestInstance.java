package com.hexagram2021.girlfriends.common.quest;

import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * 委托实例喵~
 *
 * @author liudongyu
 */
public class QuestInstance {
	public static final int DATA_VERSION = 1;

	private UUID questInstanceId;
	private Identifier characterId;
	private QuestType questType = QuestType.FIXED;
	private String questId = "";
	private Integer fixedIndex;
	private AffectionStage requiredStage = AffectionStage.STRANGER;
	private UUID ownerPlayerUuid;
	private QuestState state = QuestState.AVAILABLE;
	private long createdDay;
	private Long expireDay;
	private CompoundTag progress = new CompoundTag();

	/**
	 * 获取委托实例 ID 喵~
	 *
	 * @return 委托实例 ID 喵~
	 */
	public UUID getQuestInstanceId() {
		return this.questInstanceId;
	}

	/**
	 * 设置委托实例 ID 喵~
	 *
	 * @param questInstanceId 委托实例 ID 喵~
	 */
	public void setQuestInstanceId(UUID questInstanceId) {
		this.questInstanceId = questInstanceId;
	}

	/**
	 * 获取角色 ID 喵~
	 *
	 * @return 角色 ID 喵~
	 */
	public Identifier getCharacterId() {
		return this.characterId;
	}

	/**
	 * 设置角色 ID 喵~
	 *
	 * @param characterId 角色 ID 喵~
	 */
	public void setCharacterId(Identifier characterId) {
		this.characterId = characterId;
	}

	/**
	 * 获取委托类型喵~
	 *
	 * @return 委托类型喵~
	 */
	public QuestType getQuestType() {
		return this.questType;
	}

	/**
	 * 设置委托类型喵~
	 *
	 * @param questType 委托类型喵~
	 */
	public void setQuestType(QuestType questType) {
		this.questType = questType;
	}

	/**
	 * 获取委托定义 ID 喵~
	 *
	 * @return 委托定义 ID 喵~
	 */
	public String getQuestId() {
		return this.questId;
	}

	/**
	 * 设置委托定义 ID 喵~
	 *
	 * @param questId 委托定义 ID 喵~
	 */
	public void setQuestId(String questId) {
		this.questId = questId;
	}

	/**
	 * 获取固定委托序号喵~
	 *
	 * @return 固定委托序号喵~
	 */
	public Integer getFixedIndex() {
		return this.fixedIndex;
	}

	/**
	 * 设置固定委托序号喵~
	 *
	 * @param fixedIndex 固定委托序号喵~
	 */
	public void setFixedIndex(Integer fixedIndex) {
		this.fixedIndex = fixedIndex;
	}

	/**
	 * 获取要求阶段喵~
	 *
	 * @return 要求阶段喵~
	 */
	public AffectionStage getRequiredStage() {
		return this.requiredStage;
	}

	/**
	 * 设置要求阶段喵~
	 *
	 * @param requiredStage 要求阶段喵~
	 */
	public void setRequiredStage(AffectionStage requiredStage) {
		this.requiredStage = requiredStage;
	}

	/**
	 * 获取接取者 UUID 喵~
	 *
	 * @return 接取者 UUID 喵~
	 */
	public UUID getOwnerPlayerUuid() {
		return this.ownerPlayerUuid;
	}

	/**
	 * 设置接取者 UUID 喵~
	 *
	 * @param ownerPlayerUuid 接取者 UUID 喵~
	 */
	public void setOwnerPlayerUuid(UUID ownerPlayerUuid) {
		this.ownerPlayerUuid = ownerPlayerUuid;
	}

	/**
	 * 获取委托状态喵~
	 *
	 * @return 委托状态喵~
	 */
	public QuestState getState() {
		return this.state;
	}

	/**
	 * 设置委托状态喵~
	 *
	 * @param state 委托状态喵~
	 */
	public void setState(QuestState state) {
		this.state = state;
	}

	/**
	 * 获取创建游戏日喵~
	 *
	 * @return 创建游戏日喵~
	 */
	public long getCreatedDay() {
		return this.createdDay;
	}

	/**
	 * 设置创建游戏日喵~
	 *
	 * @param createdDay 创建游戏日喵~
	 */
	public void setCreatedDay(long createdDay) {
		this.createdDay = createdDay;
	}

	/**
	 * 获取过期游戏日喵~
	 *
	 * @return 过期游戏日喵~
	 */
	public Long getExpireDay() {
		return this.expireDay;
	}

	/**
	 * 设置过期游戏日喵~
	 *
	 * @param expireDay 过期游戏日喵~
	 */
	public void setExpireDay(Long expireDay) {
		this.expireDay = expireDay;
	}

	/**
	 * 获取进度数据喵~
	 *
	 * @return 进度数据喵~
	 */
	public CompoundTag getProgress() {
		return this.progress;
	}

	/**
	 * 设置进度数据喵~
	 *
	 * @param progress 进度数据喵~
	 */
	public void setProgress(CompoundTag progress) {
		this.progress = progress;
	}

	/**
	 * 序列化委托实例喵~
	 *
	 * @return NBT 数据喵~
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		if(this.questInstanceId != null) {
			tag.putString("quest_instance_id", this.questInstanceId.toString());
		}
		if(this.characterId != null) {
			tag.putString("character_id", this.characterId.toString());
		}
		tag.putString("quest_type", this.questType.name());
		tag.putString("quest_id", this.questId);
		if(this.fixedIndex != null) {
			tag.putInt("fixed_index", this.fixedIndex);
		}
		tag.putString("required_stage", this.requiredStage.name());
		if(this.ownerPlayerUuid != null) {
			tag.putString("owner_player_uuid", this.ownerPlayerUuid.toString());
		}
		tag.putString("state", this.state.name());
		tag.putLong("created_day", this.createdDay);
		if(this.expireDay != null) {
			tag.putLong("expire_day", this.expireDay);
		}
		tag.put("progress", this.progress.copy());
		return tag;
	}

	/**
	 * 反序列化委托实例喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 委托实例喵~
	 */
	public static QuestInstance deserializeNBT(CompoundTag tag) {
		QuestInstance instance = new QuestInstance();
		tag.getString("quest_instance_id").ifPresent(value -> instance.questInstanceId = UUID.fromString(value));
		tag.getString("character_id").ifPresent(value -> instance.characterId = Identifier.parse(value));
		instance.questType = tag.getString("quest_type").map(QuestType::valueOf).orElse(QuestType.FIXED);
		instance.questId = tag.getString("quest_id").orElse("");
		instance.fixedIndex = tag.getInt("fixed_index").orElse(null);
		instance.requiredStage = tag.getString("required_stage").map(AffectionStage::valueOf).orElse(AffectionStage.STRANGER);
		tag.getString("owner_player_uuid").ifPresent(value -> instance.ownerPlayerUuid = UUID.fromString(value));
		instance.state = tag.getString("state").map(QuestState::valueOf).orElse(QuestState.AVAILABLE);
		instance.createdDay = tag.getLong("created_day").orElse(0L);
		instance.expireDay = tag.getLong("expire_day").orElse(null);
		instance.progress = tag.getCompound("progress").orElseGet(CompoundTag::new);
		return instance;
	}
}
