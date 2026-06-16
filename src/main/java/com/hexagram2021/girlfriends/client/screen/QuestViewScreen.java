package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 委托查看界面（占位，将在 Task 8 中实现完整功能）喵~
 *
 * @author liudongyu
 */
public class QuestViewScreen extends Screen {
	public QuestViewScreen(Identifier girlfriendTypeId, QuestContentSummary quest) {
		super(Component.translatable(quest.titleKey()));
	}
}
