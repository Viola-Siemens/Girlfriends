package com.hexagram2021.girlfriends.client;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.client.model.GirlfriendModel;
import com.hexagram2021.girlfriends.client.model.GirlfriendsModelLayers;
import com.hexagram2021.girlfriends.client.renderer.GirlfriendRenderer;
import com.hexagram2021.girlfriends.common.entity.GirlfriendsEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    /**
     * 注册菜单事件
     * @param event 注册菜单事件
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {

    }

    /**
     * 注册模型层
     * @param event 注册模型层事件
     */
    @SubscribeEvent
    public static void onRegisterModels(EntityRenderersEvent.RegisterLayerDefinitions event) {
    	event.registerLayerDefinition(GirlfriendsModelLayers.MOMO, GirlfriendModel::createBodyLayer);
        event.registerLayerDefinition(GirlfriendsModelLayers.YUXI, GirlfriendModel::createBodyLayer);
        event.registerLayerDefinition(GirlfriendsModelLayers.MEISHU, GirlfriendModel::createBodyLayer);
        event.registerLayerDefinition(GirlfriendsModelLayers.WANYING, GirlfriendModel::createBodyLayer);
        event.registerLayerDefinition(GirlfriendsModelLayers.YOURUO, GirlfriendModel::createBodyLayer);
    }

    /**
     * 注册实体渲染器喵~
     *
     * @param event 实体渲染器注册事件喵~
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                GirlfriendsEntities.MOMO.get(),
                ctx -> new GirlfriendRenderer(ctx, GirlfriendsModelLayers.MOMO, "momo")
        );
    }
}
