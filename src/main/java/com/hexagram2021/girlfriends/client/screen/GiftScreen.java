package com.hexagram2021.girlfriends.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 赠礼界面（占位，将在 Task 9 中实现完整功能）喵~
 *
 * @author liudongyu
 */
public class GiftScreen extends Screen {
	public GiftScreen(Identifier girlfriendTypeId) {
		super(Component.translatable("screen.girlfriends.gift"));
	}
}
