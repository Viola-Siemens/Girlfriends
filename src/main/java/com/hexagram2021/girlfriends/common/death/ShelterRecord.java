package com.hexagram2021.girlfriends.common.death;

import net.minecraft.IdentifierException;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;

/**
 * 庇护所记录喵~
 *
 * @author liudongyu
 */
public class ShelterRecord {
	public static final int DATA_VERSION = 1;

	private Identifier structureId;
	private GlobalPos shelterPos;
	private String dimensionId;
	private int x;
	private int y;
	private int z;
	private long registeredDay;
	private boolean generated;
	private boolean discovered;

	/**
	 * 获取结构 ID 喵~
	 *
	 * @return 结构 ID 喵~
	 */
	public Identifier getStructureId() {
		return this.structureId;
	}

	/**
	 * 设置结构 ID 喵~
	 *
	 * @param structureId 结构 ID 喵~
	 */
	public void setStructureId(Identifier structureId) {
		this.structureId = structureId;
	}

	/**
	 * 获取庇护所位置喵~
	 *
	 * @return 庇护所位置喵~
	 */
	public GlobalPos getShelterPos() {
		return this.shelterPos;
	}

	/**
	 * 设置庇护所位置喵~
	 *
	 * @param shelterPos 庇护所位置喵~
	 */
	public void setShelterPos(GlobalPos shelterPos) {
		this.shelterPos = shelterPos;
	}

	/**
	 * 获取维度 ID 喵~
	 *
	 * @return 维度 ID 喵~
	 */
	public String getDimensionId() {
		return this.dimensionId;
	}

	/**
	 * 设置维度 ID 喵~
	 *
	 * @param dimensionId 维度 ID 喵~
	 */
	public void setDimensionId(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	/**
	 * 获取 X 坐标喵~
	 *
	 * @return X 坐标喵~
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * 设置 X 坐标喵~
	 *
	 * @param x X 坐标喵~
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * 获取 Y 坐标喵~
	 *
	 * @return Y 坐标喵~
	 */
	public int getY() {
		return this.y;
	}

	/**
	 * 设置 Y 坐标喵~
	 *
	 * @param y Y 坐标喵~
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * 获取 Z 坐标喵~
	 *
	 * @return Z 坐标喵~
	 */
	public int getZ() {
		return this.z;
	}

	/**
	 * 设置 Z 坐标喵~
	 *
	 * @param z Z 坐标喵~
	 */
	public void setZ(int z) {
		this.z = z;
	}

	/**
	 * 获取注册游戏日喵~
	 *
	 * @return 注册游戏日喵~
	 */
	public long getRegisteredDay() {
		return this.registeredDay;
	}

	/**
	 * 设置注册游戏日喵~
	 *
	 * @param registeredDay 注册游戏日喵~
	 */
	public void setRegisteredDay(long registeredDay) {
		this.registeredDay = registeredDay;
	}

	/**
	 * 判断是否已生成喵~
	 *
	 * @return 是否已生成喵~
	 */
	public boolean isGenerated() {
		return this.generated;
	}

	/**
	 * 设置是否已生成喵~
	 *
	 * @param generated 是否已生成喵~
	 */
	public void setGenerated(boolean generated) {
		this.generated = generated;
	}

	/**
	 * 判断是否已发现喵~
	 *
	 * @return 是否已发现喵~
	 */
	public boolean isDiscovered() {
		return this.discovered;
	}

	/**
	 * 设置是否已发现喵~
	 *
	 * @param discovered 是否已发现喵~
	 */
	public void setDiscovered(boolean discovered) {
		this.discovered = discovered;
	}

	/**
	 * 序列化庇护所记录喵~
	 *
	 * @return NBT 数据喵~
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		if(this.structureId != null) {
			tag.putString("structure_id", this.structureId.toString());
		}
		if(this.shelterPos != null) {
			GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.shelterPos).result().ifPresent(value -> tag.put("shelter_pos", value));
		}
		if(this.dimensionId != null) {
			tag.putString("dimension_id", this.dimensionId);
		}
		tag.putInt("x", this.x);
		tag.putInt("y", this.y);
		tag.putInt("z", this.z);
		tag.putLong("registered_day", this.registeredDay);
		tag.putBoolean("generated", this.generated);
		tag.putBoolean("discovered", this.discovered);
		return tag;
	}

	/**
	 * 反序列化庇护所记录喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 庇护所记录喵~
	 */
	public static ShelterRecord deserializeNBT(CompoundTag tag) {
		ShelterRecord record = new ShelterRecord();
		tag.getString("structure_id").ifPresent(value -> record.structureId = parseIdentifierOrNull(value));
		if(tag.get("shelter_pos") != null) {
			GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("shelter_pos")).result().ifPresent(pos -> record.shelterPos = pos);
		}
		record.dimensionId = tag.getString("dimension_id").orElse(null);
		record.x = tag.getInt("x").orElse(0);
		record.y = tag.getInt("y").orElse(0);
		record.z = tag.getInt("z").orElse(0);
		record.registeredDay = tag.getLong("registered_day").orElse(0L);
		record.generated = tag.getBoolean("generated").orElse(false);
		record.discovered = tag.getBoolean("discovered").orElse(false);
		return record;
	}

	private static Identifier parseIdentifierOrNull(String value) {
		try {
			return Identifier.parse(value);
		} catch(IdentifierException ignored) {
			return null;
		}
	}
}
