package com.hexagram2021.girlfriends.common.death;

/**
 * 角色重生处理结果喵~
 *
 * @param respawned 是否已重生喵~
 * @param pendingRespawn 是否等待重生喵~
 * @param shelterRecord 使用的庇护所记录喵~
 * @author liudongyu
 */
public record RespawnResult(boolean respawned, boolean pendingRespawn, ShelterRecord shelterRecord) {
}
