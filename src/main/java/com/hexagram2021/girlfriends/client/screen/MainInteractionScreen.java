package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundConfirmIntimacyPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundDeliverQuestPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundInviteHomePacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundSetFollowModePacket;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 玩家与角色交互主界面喵~
 *
 * @author liudongyu
 */
public class MainInteractionScreen extends Screen {
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;
	private static final int CENTER_X_OFFSET = 60;

	private final Identifier girlfriendTypeId;
	private final InteractionSummary summary;
	private FollowMode selectedFollowMode;

	/**
	 * 角色交互主界面
	 * @param girlfriendTypeId 角色类型 ID
	 * @param summary 交互摘要
	 */
	public MainInteractionScreen(Identifier girlfriendTypeId, InteractionSummary summary) {
		super(Component.translatable("screen.girlfriends.interaction"));
		this.girlfriendTypeId = girlfriendTypeId;
		this.summary = summary;
		// 初始化为当前角色跟随模式，HOME 模式则退化为 NONE 喵~
		this.selectedFollowMode = summary.followMode() == FollowMode.HOME ? FollowMode.NONE : summary.followMode();
	}

	@Override
	protected void init() {
		super.init();
		int leftX = this.width / 20;
		int rightX = this.width * 19 / 20 - BUTTON_WIDTH;
		int centerX = this.width / 2 - CENTER_X_OFFSET;
		int y = this.height / 16;

		// 查看委托 / 交付委托按钮（同位置）喵~
		QuestContentSummary quest = this.summary.currentQuest();
		if(quest != null && quest.questState() != QuestState.AVAILABLE) {
			// 已接取 → 显示交付委托喵~
			Button deliverBtn = this.addRenderableWidget(Button.builder(
					Component.translatable("button.girlfriends.deliver_quest"),
					_ -> {
						ClientPacketDistributor.sendToServer(new ServerboundDeliverQuestPacket(this.girlfriendTypeId));
						this.onClose();
					}
			).bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			deliverBtn.active = quest.questCompleted();
		} else {
			// 未接取 → 显示查看委托喵~
			Button viewQuestBtn = this.addRenderableWidget(Button.builder(
					Component.translatable("button.girlfriends.view_quest"),
					_ -> this.minecraft.setScreen(new QuestViewScreen(this.girlfriendTypeId, quest))
			).bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			viewQuestBtn.active = quest != null;
		}

		// 跟随模式切换按钮喵~
		Component followText = Component.translatable("button.girlfriends.follow_mode",
				Component.translatable("follow." + this.selectedFollowMode.getSerializedName()));
		this.addRenderableWidget(Button.builder(
				followText,
				btn -> {
					if(this.summary.canFollow()) {
						this.selectedFollowMode = switch(this.selectedFollowMode) {
							case NONE -> FollowMode.FOLLOW;
							case FOLLOW -> FollowMode.STAY;
							default -> FollowMode.NONE;
						};
						btn.setMessage(Component.translatable("button.girlfriends.follow_mode",
								Component.translatable("follow." + this.selectedFollowMode.getSerializedName())));
					}
				}
		).bounds(rightX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += 25;

		// 确立关系 / 邀请同居按钮（同位置，条件性显示）喵~
		if(this.summary.needsIntimacyConfirmation()) {
			this.addRenderableWidget(Button.builder(
					Component.translatable("button.girlfriends.confirm_intimacy"),
					_ -> {
						ClientPacketDistributor.sendToServer(new ServerboundConfirmIntimacyPacket(this.girlfriendTypeId));
						this.onClose();
					}
			).bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			y += 25;
		} else if(this.summary.canInviteHome()) {
			this.addRenderableWidget(Button.builder(
					Component.translatable("button.girlfriends.invite_home"), _ -> {
						ClientPacketDistributor.sendToServer(new ServerboundInviteHomePacket(this.girlfriendTypeId));
						this.onClose();
					}
			).bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			y += 25;
		}

		// 赠送礼物按钮喵~
		this.addRenderableWidget(Button.builder(
				Component.translatable("button.girlfriends.give_gift"),
				_ -> this.minecraft.setScreen(new GiftScreen(this.girlfriendTypeId))
		).bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		// 关闭按钮喵~
		this.addRenderableWidget(Button.builder(
				Component.translatable("button.girlfriends.close"),
				_ -> this.onClose()
		).bounds(centerX, this.height * 15 / 16 - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		// 标题 - 角色名喵~
		graphics.centeredText(this.font,
				Component.translatable("girlfriends.girlfriend_type." + this.girlfriendTypeId.getPath()),
				centerX, 20, 0xFFFFFF);
		// 好感阶段 + 进度喵~
		AffectionStage stage = this.summary.stage();
		String progressText = String.format("  %.0f/%.0f",
				stage.getMinAffection() + this.summary.stageProgress() * (stage.getMaxAffection() - stage.getMinAffection()),
				(double) stage.getMaxAffection()
		);
		Component stageComponent = Component.translatable("affection." + stage.name().toLowerCase())
				.append(progressText);
		graphics.centeredText(this.font, stageComponent, centerX, 40, 0xAAAAAA);
	}

	@Override
	public void onClose() {
		// Only send follow mode packet if the player can follow (has confirmed intimacy)喵~
		if(this.summary.canFollow()) {
			ClientPacketDistributor.sendToServer(new ServerboundSetFollowModePacket(
					this.girlfriendTypeId, this.selectedFollowMode
			));
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
