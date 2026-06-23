package com.hexagram2021.girlfriends.client.screen;

import com.google.common.collect.Maps;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.KnownGiftPreferenceSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundGiveGiftFromSlotPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 送礼界面喵~
 * 渲染 9 列 x 4 行玩家背包网格，高亮已知偏好物品喵~
 *
 * @author liudongyu
 */
public class GiftScreen extends Screen {
	private static final int SLOT_SIZE = 18;
	private static final int COLS = 9;
	private static final int ROWS = 4;

	private final Identifier girlfriendTypeId;
	private final Map<Integer, GiftPreferenceLevel> slotPreferenceMap = Maps.newHashMap();

	public GiftScreen(Identifier girlfriendTypeId) {
		super(Component.translatable("screen.girlfriends.gift"));
		this.girlfriendTypeId = girlfriendTypeId;
	}

	@Override
	protected void init() {
		super.init();
		InteractionSummary summary = ClientInteractionStore.getSummary(this.girlfriendTypeId);
		if(summary != null) {
			List<KnownGiftPreferenceSummary> preferences = summary.knownGiftPreferences();
			Inventory inventory = this.minecraft.player.getInventory();
			for(int i = 0; i < 36; i++) {
				ItemStack stack = inventory.getItem(i);
				if(!stack.isEmpty()) {
					GiftPreferenceLevel level = resolvePreferenceLevel(preferences, stack);
					if(level != null) {
						this.slotPreferenceMap.put(i, level);
					}
				}
			}
		}
	}

	@Nullable
	private static GiftPreferenceLevel resolvePreferenceLevel(
			List<KnownGiftPreferenceSummary> preferences, ItemStack stack) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		for(KnownGiftPreferenceSummary pref : preferences) {
			if(pref.tag()) {
				if(stack.is(ItemTags.create(pref.itemOrTagId()))) {
					return pref.level();
				}
			} else if(pref.itemOrTagId().equals(itemId)) {
				return pref.level();
			}
		}
		return null;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int leftPos = (this.width - COLS * SLOT_SIZE) / 2;
		int topPos = (this.height - ROWS * SLOT_SIZE) / 2;

		Inventory inventory = this.minecraft.player.getInventory();
		for(int row = 0; row < ROWS; row++) {
			for(int col = 0; col < COLS; col++) {
				int slotIndex = row * COLS + col;
				int x = leftPos + col * SLOT_SIZE;
				int y = topPos + row * SLOT_SIZE;

				ItemStack stack = inventory.getItem(slotIndex);
				// 绘制槽位背景喵~
				graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x8B8B8B8B);
				if(!stack.isEmpty()) {
					graphics.item(stack, x + 1, y + 1);
					graphics.itemDecorations(this.font, stack, x + 1, y + 1);
				}

				// 绘制偏好高亮边框喵~
				GiftPreferenceLevel level = this.slotPreferenceMap.get(slotIndex);
				if(level != null) {
					int color = switch(level) {
						case FAVORITE -> 0xFFFFD700;
						case LIKED -> 0xFF00FF00;
						case ACCEPTED -> 0xFF4169E1;
						case DISLIKED -> 0xFFFF4444;
						default -> 0xFFFFFFFF;
					};
					graphics.fill(x, y, x + SLOT_SIZE, y + 1, color);
					graphics.fill(x, y, x + 1, y + SLOT_SIZE, color);
					graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
					graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
				}
			}
		}

		// 标题喵~
		graphics.centeredText(this.font, this.title, this.width / 2, topPos - 12, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if(event.button() == 0) {
			int leftPos = (this.width - COLS * SLOT_SIZE) / 2;
			int topPos = (this.height - ROWS * SLOT_SIZE) / 2;
			int col = (int)((event.x() - leftPos) / SLOT_SIZE);
			int row = (int)((event.y() - topPos) / SLOT_SIZE);
			if(col >= 0 && col < COLS && row >= 0 && row < ROWS) {
				int slotIndex = row * COLS + col;
				Inventory inventory = this.minecraft.player.getInventory();
				if(!inventory.getItem(slotIndex).isEmpty()) {
					ClientPacketDistributor.sendToServer(
							new ServerboundGiveGiftFromSlotPacket(this.girlfriendTypeId, slotIndex));
					this.onClose();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
