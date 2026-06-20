package com.hexagram2021.girlfriends.common.blessing;

import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * 祝福判定上下文喵~
 *
 * @param playerUuid 玩家 UUID 喵~
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param blessingTypeId 祝福类型 ID 喵~
 * @param sameDimension 是否同维度喵~
 * @param distance 与角色距离喵~
 * @author liudongyu
 */
public record BlessingContext(UUID playerUuid, Identifier girlfriendTypeId, Identifier blessingTypeId, boolean sameDimension, double distance) {
}
