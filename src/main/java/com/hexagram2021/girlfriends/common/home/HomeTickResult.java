package com.hexagram2021.girlfriends.common.home;

/**
 * 家园日常收益结果喵~
 *
 * @param healed 是否触发回血喵~
 * @param affectionDelta 好感变化喵~
 * @author liudongyu
 */
public record HomeTickResult(boolean healed, float affectionDelta) {
}
