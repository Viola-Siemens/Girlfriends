package com.hexagram2021.girlfriends.common.blessing;

/**
 * 角色跟随模式喵~
 *
 * @author liudongyu
 */
public enum FollowMode {
	STAY,
	FOLLOW,
	HOME;

	/**
	 * 根据序号获取跟随模式喵~
	 *
	 * @param id 序号喵~
	 * @return 对应的跟随模式，越界则返回 STAY 喵~
	 */
	public static FollowMode fromId(int id) {
		FollowMode[] values = values();
		if (id < 0 || id >= values.length) {
			return STAY;
		}
		return values[id];
	}
}
