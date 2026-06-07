package com.hexagram2021.girlfriends.common.entity.ai;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * 角色日程时段定义喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsScheduleTypes {
	/** 清晨 (0~2000 tick) 喵~ */
	public static final int MORNING = 0;
	/** 上午工作时间 (2000~6000 tick) 喵~ */
	public static final int DAY_WORK = 1;
	/** 下午活动时间 (6000~11000 tick) 喵~ */
	public static final int AFTERNOON = 2;
	/** 傍晚收尾 (11000~13000 tick) 喵~ */
	public static final int SUNSET = 3;
	/** 夜晚休息 (13000~24000 tick) 喵~ */
	public static final int NIGHT_REST = 4;

	/** 日程名称查找表喵~ */
	public static final Int2ObjectMap<String> SCHEDULE_NAMES = new Int2ObjectOpenHashMap<>();

	static {
		SCHEDULE_NAMES.put(MORNING, "morning");
		SCHEDULE_NAMES.put(DAY_WORK, "day_work");
		SCHEDULE_NAMES.put(AFTERNOON, "afternoon");
		SCHEDULE_NAMES.put(SUNSET, "sunset");
		SCHEDULE_NAMES.put(NIGHT_REST, "night_rest");
	}

	/**
	 * 根据游戏 tick 获取当前时段喵~
	 *
	 * @param dayTime 当日游戏时间 (0~24000) 喵~
	 * @return 时段常量喵~
	 */
	public static int getSchedule(long dayTime) {
		if (dayTime < 2000) {
			return MORNING;
		} else if (dayTime < 6000) {
			return DAY_WORK;
		} else if (dayTime < 11000) {
			return AFTERNOON;
		} else if (dayTime < 13000) {
			return SUNSET;
		}
		return NIGHT_REST;
	}

	private GirlfriendsScheduleTypes() {
	}
}
