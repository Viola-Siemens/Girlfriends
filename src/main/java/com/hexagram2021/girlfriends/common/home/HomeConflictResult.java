package com.hexagram2021.girlfriends.common.home;

/**
 * 家园争执结果喵~
 *
 * @param triggered 是否触发争执喵~
 * @param homePartnerDelta 家园伙伴好感变化喵~
 * @param visitorDelta 访客好感变化喵~
 * @author liudongyu
 */
public record HomeConflictResult(boolean triggered, int homePartnerDelta, int visitorDelta) {
}
