package com.hexagram2021.girlfriends.common.persist;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.death.ShelterRecord;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * GirlfriendsWorldData 持久化测试喵~
 *
 * @author liudongyu
 */
class GirlfriendsWorldDataTest {
	/**
	 * 验证关系数据可以完成往返序列化喵~
	 */
	@Test
	void serializeAndDeserializeRelation() {
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
		Identifier momoId = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
		GirlfriendsWorldData data = new GirlfriendsWorldData();
		data.updateRelation(playerUuid, momoId, relation -> {
			relation.setAffection(321.0F);
			relation.setConfirmedIntimacy(true);
		});

		CompoundTag tag = data.save(new CompoundTag());
		GirlfriendsWorldData restored = GirlfriendsWorldData.load(tag);
		PlayerCharacterRelation restoredRelation = restored.getRelations().get(new RelationKey(playerUuid, momoId));

		Assertions.assertNotNull(restoredRelation);
		Assertions.assertEquals(321.0F, restoredRelation.getAffection());
		Assertions.assertTrue(restoredRelation.isConfirmedIntimacy());
	}

	/**
	 * 验证已加载关系通过受支持更新路径修改后会标记脏数据喵~
	 */
	@Test
	void loadedRelationMutationMarksDataDirty() {
		UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Identifier momoId = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
		GirlfriendsWorldData original = new GirlfriendsWorldData();
		original.updateRelation(playerUuid, momoId, relation -> relation.setAffection(100));

		CompoundTag tag = original.save(new CompoundTag());
		GirlfriendsWorldData restored = GirlfriendsWorldData.load(tag);
		Assertions.assertFalse(restored.isDirty());

		restored.updateRelation(playerUuid, momoId, relation -> relation.setAffection(200.0F));

		Assertions.assertTrue(restored.isDirty());
		Assertions.assertEquals(200.0F, restored.getRelations().get(new RelationKey(playerUuid, momoId)).getAffection());
	}

	/**
	 * 验证世界数据加载时会跳过非法顶层键喵~
	 */
	@Test
	void invalidTopLevelKeysAreSkippedDuringLoad() {
		CompoundTag tag = new CompoundTag();
		CompoundTag charactersTag = new CompoundTag();
		CompoundTag characterTag = new CompoundTag();
		CompoundTag shelterTag = new CompoundTag();
		ListTag shelterListTag = new ListTag();
		shelterTag.putString("structure_id", "bad id");
		shelterListTag.add(shelterTag);
		characterTag.put("discovered_shelters", shelterListTag);
		charactersTag.put("bad id", characterTag);
		charactersTag.put("girlfriends:momo", characterTag);
		tag.put("characters", charactersTag);
		CompoundTag homesTag = new CompoundTag();
		homesTag.put("bad uuid", new CompoundTag());
		tag.put("homes", homesTag);

		GirlfriendsWorldData restored = GirlfriendsWorldData.load(tag);
		ShelterRecord shelterRecord = restored.getCharacterState(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo")).getDiscoveredShelters().getFirst();

		Assertions.assertEquals(1, restored.getCharacterState(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo")).getDiscoveredShelters().size());
		Assertions.assertNull(shelterRecord.getStructureId());
		Assertions.assertTrue(restored.getHomes().isEmpty());
	}
}
