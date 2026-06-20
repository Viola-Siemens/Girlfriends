package com.hexagram2021.girlfriends.common.gift;

import javax.annotation.Nullable;

/**
 * 赠礼处理结果喵~
 *
 * @param rejected 是否被拒收喵~
 * @param affectionDelta 实际好感变更值喵~
 * @param level 礼物偏好等级喵~
 * @param messageKey 结果消息键（用于 subtitle）喵~
 * @param quoteKey 角色回复台词 i18n key，为 null 时不发送角色台词（聊天栏）喵~
 *
 * @author liudongyu
 */
public record GiftResult(boolean rejected, float affectionDelta, GiftPreferenceLevel level,
                         String messageKey, @Nullable String quoteKey) {
	/**
	 * 创建拒收结果喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param messageKey 结果消息键喵~
	 * @return 拒收结果喵~
	 */
	public static GiftResult rejected(GiftPreferenceLevel level, String messageKey) {
		return new GiftResult(true, 0, level, messageKey, null);
	}

	/**
	 * 创建拒收结果（含 quoteKey）喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param messageKey 结果消息键喵~
	 * @param quoteKey 角色台词 i18n key，可为 null 喵~
	 * @return 拒收结果喵~
	 */
	public static GiftResult rejected(GiftPreferenceLevel level, String messageKey, @Nullable String quoteKey) {
		return new GiftResult(true, 0, level, messageKey, quoteKey);
	}

	/**
	 * 创建已处理结果喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param affectionDelta 实际好感变更值喵~
	 * @param messageKey 结果消息键喵~
	 * @return 已处理结果喵~
	 */
	public static GiftResult accepted(GiftPreferenceLevel level, float affectionDelta, String messageKey) {
		return new GiftResult(false, affectionDelta, level, messageKey, null);
	}

	/**
	 * 创建已处理结果（含 quoteKey）喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param affectionDelta 实际好感变更值喵~
	 * @param messageKey 结果消息键喵~
	 * @param quoteKey 角色台词 i18n key，可为 null 喵~
	 * @return 已处理结果喵~
	 */
	public static GiftResult accepted(GiftPreferenceLevel level, float affectionDelta, String messageKey,
	                                  @Nullable String quoteKey) {
		return new GiftResult(false, affectionDelta, level, messageKey, quoteKey);
	}
}
