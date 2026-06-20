package com.hexagram2021.girlfriends.common.home;

import net.minecraft.resources.Identifier;

/**
 * 家园床位锚点喵~
 *
 * @param dimension 维度 ID 喵~
 * @param x X 坐标喵~
 * @param y Y 坐标喵~
 * @param z Z 坐标喵~
 * @author liudongyu
 */
public record HomeAnchor(Identifier dimension, int x, int y, int z) {
}
