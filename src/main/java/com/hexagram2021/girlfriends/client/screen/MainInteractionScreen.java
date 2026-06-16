package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 玩家与角色交互主界面喵~
 *
 * @author liudongyu
 */
public class MainInteractionScreen extends Screen {
	private final Identifier girlfriendTypeId;
	private final InteractionSummary summary;

	public MainInteractionScreen(Identifier girlfriendTypeId, InteractionSummary summary) {
		super(Component.translatable("screen.girlfriends.interaction"));
		this.girlfriendTypeId = girlfriendTypeId;
		this.summary = summary;
	}
}
