package com.hexagram2021.girlfriends.client;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 模组客户端主类
 *
 * @author liudongyu
 */
@Mod(value = GirlfriendsMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GirlfriendsMod.MODID, value = Dist.CLIENT)
public class GirlfriendsModClient {
    /**
     * 模组客户端主类构造函数
     * @param container 模组容器
     */
    public GirlfriendsModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * 客户端初始化事件
     * @param event 客户端初始化事件
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
