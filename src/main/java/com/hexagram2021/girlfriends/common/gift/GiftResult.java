package com.hexagram2021.girlfriends.common.gift;

/**
 * 赠礼处理结果喵~
 *
 * @param rejected 是否被拒收喵~
 * @param affectionDelta 实际好感变更值喵~
 * @param level 礼物偏好等级喵~
 * @param messageKey 结果消息键喵~
 *
 * @author liudongyu
 */
public record GiftResult(boolean rejected, int affectionDelta, GiftPreferenceLevel level, String messageKey) {
	/**
	 * 创建拒收结果喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param messageKey 结果消息键喵~
	 * @return 拒收结果喵~
	 */
	public static GiftResult rejected(GiftPreferenceLevel level, String messageKey) {
		return new GiftResult(true, 0, level, messageKey);
	}

	/**
	 * 创建已处理结果喵~
	 *
	 * @param level 礼物偏好等级喵~
	 * @param affectionDelta 实际好感变更值喵~
	 * @param messageKey 结果消息键喵~
	 * @return 已处理结果喵~
	 */
	public static GiftResult accepted(GiftPreferenceLevel level, int affectionDelta, String messageKey) {
		return new GiftResult(false, affectionDelta, level, messageKey);
	}
}
