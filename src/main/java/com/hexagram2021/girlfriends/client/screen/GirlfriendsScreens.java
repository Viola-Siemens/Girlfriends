package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Girlfriends Screen 注册与管理入口喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsScreens {
	/**
	 * 从缓存获取 InteractionSummary 并打开主交互界面喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 是否成功打开喵~
	 */
	public static boolean openMainInteractionScreen(Identifier girlfriendTypeId) {
		InteractionSummary summary = ClientInteractionStore.getSummary(girlfriendTypeId);
		if(summary == null) {
			return false;
		}
		Minecraft.getInstance().setScreen(new MainInteractionScreen(girlfriendTypeId, summary));
		return true;
	}

	private GirlfriendsScreens() {
	}
}
