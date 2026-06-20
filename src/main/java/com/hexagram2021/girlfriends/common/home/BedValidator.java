package com.hexagram2021.girlfriends.common.home;

/**
 * 双人床校验器喵~
 *
 * @author liudongyu
 */
@FunctionalInterface
public interface BedValidator {
	/**
	 * 判断床位锚点是否有效喵~
	 *
	 * @param homeAnchor 家园床位锚点喵~
	 * @return 是否有效喵~
	 */
	boolean isValid(HomeAnchor homeAnchor);
}
