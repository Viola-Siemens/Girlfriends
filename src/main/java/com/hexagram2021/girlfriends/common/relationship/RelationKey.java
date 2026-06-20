package com.hexagram2021.girlfriends.common.relationship;

import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * 玩家与角色关系键喵~
 *
 * @param playerUuid 玩家 UUID 喵~
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record RelationKey(UUID playerUuid, Identifier girlfriendTypeId) {
}
