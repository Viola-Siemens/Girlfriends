package com.hexagram2021.girlfriends.common.persist;

import com.google.common.collect.Maps;
import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.character.CharacterWorldState;
import com.hexagram2021.girlfriends.common.home.HomeState;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationKey;
import com.mojang.serialization.Codec;
import net.minecraft.IdentifierException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Girlfriends 世界级持久化容器喵~
 *
 * @author liudongyu
 */
public class GirlfriendsWorldData extends SavedData {
	public static final int DATA_VERSION = 1;
	public static final String DATA_NAME = "girlfriends_world_data";
	public static final Codec<GirlfriendsWorldData> CODEC = CompoundTag.CODEC.xmap(GirlfriendsWorldData::load, GirlfriendsWorldData::saveToTag);
	public static final SavedDataType<GirlfriendsWorldData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, DATA_NAME),
			GirlfriendsWorldData::new,
			CODEC
	);

	private final Map<Identifier, CharacterWorldState> characters = Maps.newHashMap();
	private final Map<RelationKey, PlayerCharacterRelation> relations = Maps.newHashMap();
	private final Map<UUID, HomeState> homes = Maps.newHashMap();

	/**
	 * 获取或创建玩家与角色关系喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 关系状态喵~
	 */
	public PlayerCharacterRelation getOrCreateRelation(UUID playerUuid, Identifier girlfriendTypeId) {
		RelationKey key = new RelationKey(playerUuid, girlfriendTypeId);
		return this.relations.computeIfAbsent(key, ignored -> {
			PlayerCharacterRelation relation = new PlayerCharacterRelation(playerUuid, girlfriendTypeId);
			this.setDirty();
			return relation;
		});
	}

	/**
	 * 查询已存在的玩家与角色关系喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 已存在的关系状态喵~
	 */
	@Nullable
	public PlayerCharacterRelation getExistingRelation(UUID playerUuid, Identifier girlfriendTypeId) {
		return this.relations.get(new RelationKey(playerUuid, girlfriendTypeId));
	}

	/**
	 * 获取或创建角色世界状态喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 角色世界状态喵~
	 */
	public CharacterWorldState getCharacterState(Identifier girlfriendTypeId) {
		return this.characters.computeIfAbsent(girlfriendTypeId, ignored -> {
			CharacterWorldState state = new CharacterWorldState();
			state.setCharacterId(girlfriendTypeId);
			this.setDirty();
			return state;
		});
	}

	/**
	 * 查询已存在的角色世界状态喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 已存在的角色世界状态喵~
	 */
	@Nullable
	public CharacterWorldState getExistingCharacterState(Identifier girlfriendTypeId) {
		return this.characters.get(girlfriendTypeId);
	}

	/**
	 * 通过脏感知路径更新玩家与角色关系喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param updater 更新逻辑喵~
	 * @return 更新后的关系状态喵~
	 */
	public PlayerCharacterRelation updateRelation(UUID playerUuid, Identifier girlfriendTypeId, Consumer<PlayerCharacterRelation> updater) {
		PlayerCharacterRelation relation = this.getOrCreateRelation(playerUuid, girlfriendTypeId);
		updater.accept(relation);
		this.setDirty();
		return relation;
	}

	/**
	 * 通过脏感知路径更新角色世界状态喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param updater 更新逻辑喵~
	 * @return 更新后的角色世界状态喵~
	 */
	public CharacterWorldState updateCharacter(Identifier girlfriendTypeId, Consumer<CharacterWorldState> updater) {
		CharacterWorldState state = this.getCharacterState(girlfriendTypeId);
		updater.accept(state);
		this.setDirty();
		return state;
	}

	/**
	 * 获取或创建家园状态喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @return 家园状态喵~
	 */
	public HomeState getOrCreateHomeState(UUID playerUuid) {
		return this.homes.computeIfAbsent(playerUuid, ignored -> {
			HomeState state = new HomeState();
			state.setPlayerUuid(playerUuid);
			this.setDirty();
			return state;
		});
	}

	/**
	 * 通过脏感知路径更新家园状态喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param updater 更新逻辑喵~
	 * @return 更新后的家园状态喵~
	 */
	public HomeState updateHome(UUID playerUuid, Consumer<HomeState> updater) {
		HomeState state = this.getOrCreateHomeState(playerUuid);
		updater.accept(state);
		this.setDirty();
		return state;
	}

	/**
	 * 获取全部关系数据喵~
	 *
	 * @return 关系映射喵~
	 */
	public Map<RelationKey, PlayerCharacterRelation> getRelations() {
		return this.relations;
	}

	/**
	 * 获取全部角色世界状态喵~
	 *
	 * @return 角色世界状态映射喵~
	 */
	public Map<Identifier, CharacterWorldState> getCharacters() {
		return this.characters;
	}

	/**
	 * 获取全部家园数据喵~
	 *
	 * @return 家园映射喵~
	 */
	public Map<UUID, HomeState> getHomes() {
		return this.homes;
	}

	/**
	 * 读取世界数据喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 世界数据喵~
	 */
	public static GirlfriendsWorldData load(CompoundTag tag) {
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		CompoundTag charactersTag = tag.getCompoundOrEmpty("characters");
		for(String key : charactersTag.keySet()) {
			Identifier id = parseIdentifierOrNull(key);
			if(id != null) {
				data.characters.put(id, CharacterWorldState.deserializeNBT(charactersTag.getCompoundOrEmpty(key)));
			}
		}
		ListTag relationsTag = tag.getListOrEmpty("player_relations");
		for (Tag value : relationsTag) {
			if (value instanceof CompoundTag relationTag) {
				PlayerCharacterRelation relation = PlayerCharacterRelation.deserializeNBT(relationTag);
				if (relation.getPlayerUuid() != null && relation.getCharacterId() != null) {
					data.relations.put(new RelationKey(relation.getPlayerUuid(), relation.getCharacterId()), relation);
				}
			}
		}
		CompoundTag homesTag = tag.getCompoundOrEmpty("homes");
		for(String key : homesTag.keySet()) {
			UUID playerUuid = parseUuidOrNull(key);
			if(playerUuid != null) {
				data.homes.put(playerUuid, HomeState.deserializeNBT(homesTag.getCompoundOrEmpty(key)));
			}
		}
		data.setDirty(false);
		return data;
	}

	/**
	 * 保存世界数据喵~
	 *
	 * @param tag 目标 NBT 喵~
	 * @return 保存结果喵~
	 */
	public CompoundTag save(CompoundTag tag) {
		CompoundTag saved = this.saveToTag();
		for(String key : saved.keySet()) {
			tag.put(key, Objects.requireNonNull(saved.get(key)));
		}
		return tag;
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

	private CompoundTag saveToTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		CompoundTag charactersTag = new CompoundTag();
		for(Map.Entry<Identifier, CharacterWorldState> entry : this.characters.entrySet()) {
			charactersTag.put(entry.getKey().toString(), entry.getValue().serializeNBT());
		}
		tag.put("characters", charactersTag);
		ListTag relationsTag = new ListTag();
		for(PlayerCharacterRelation relation : this.relations.values()) {
			relationsTag.add(relation.serializeNBT());
		}
		tag.put("player_relations", relationsTag);
		CompoundTag homesTag = new CompoundTag();
		for(Map.Entry<UUID, HomeState> entry : this.homes.entrySet()) {
			homesTag.put(entry.getKey().toString(), entry.getValue().serializeNBT());
		}
		tag.put("homes", homesTag);
		return tag;
	}
}
