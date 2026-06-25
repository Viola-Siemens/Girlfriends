package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundAcceptQuestPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 委托查看界面喵~
 *
 * @author liudongyu
 */
public class QuestViewScreen extends Screen {
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;

	private final Identifier girlfriendTypeId;
	private final QuestContentSummary quest;

	/**
	 * 构造委托查看界面
	 * @param girlfriendTypeId 角色类型 ID
	 * @param quest 委托内容
	 */
	public QuestViewScreen(Identifier girlfriendTypeId, QuestContentSummary quest) {
		super(Component.translatable(quest.titleKey()));
		this.girlfriendTypeId = girlfriendTypeId;
		this.quest = quest;
	}

	@Override
	protected void init() {
		super.init();
		int centerX = this.width / 2;

		// 接受委托按钮喵~
		this.addRenderableWidget(
				Button.builder(
						Component.translatable("button.girlfriends.accept_quest"),
						_ -> {
							ClientPacketDistributor.sendToServer(new ServerboundAcceptQuestPacket(this.girlfriendTypeId));
							// 关闭当前界面和主界面喵~
							this.minecraft.setScreen(null);
						}
				).bounds(centerX - BUTTON_WIDTH - 10, this.height - 50, BUTTON_WIDTH, BUTTON_HEIGHT).build()
		);

		// 我再想想按钮喵~
		this.addRenderableWidget(
				Button.builder(Component.translatable("button.girlfriends.think_again"), _ -> this.onClose())
						.bounds(centerX + 10, this.height - 50, BUTTON_WIDTH, BUTTON_HEIGHT)
						.build()
		);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		int y = 20;

		// 标题喵~
		graphics.centeredText(this.font, Component.translatable(this.quest.titleKey()), centerX, y, 0xFFFFFF);
		y += 15;

		// 委托类型副标题喵~
		graphics.centeredText(this.font,
				Component.translatable("quest.girlfriends.type." + this.quest.questType().name().toLowerCase()),
				centerX, y, 0xAAAAAA);
		y += 25;

		// 委托描述喵~
		Component description = Component.translatable(this.quest.descriptionKey());
		for(FormattedCharSequence line : this.font.split(description, this.width - 40)) {
			graphics.text(this.font, line, 20, y, 0xCCCCCC);
			y += this.font.lineHeight + 2;
		}
		y += 10;

		// 目标列表喵~
		if(!this.quest.objectiveSummaryKeys().isEmpty()) {
			graphics.text(this.font, Component.translatable("label.girlfriends.objectives"), 20, y, 0xFFFFFF);
			y += this.font.lineHeight + 2;
			for(String key : this.quest.objectiveSummaryKeys()) {
				graphics.text(this.font, Component.literal("  ").append(Component.translatable(key)),
						20, y, 0xCCCCCC);
				y += this.font.lineHeight + 2;
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
