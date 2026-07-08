package com.hexagram2021.girlfriends.client;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.client.model.GirlfriendModel;
import com.hexagram2021.girlfriends.client.model.GirlfriendsModelLayers;
import com.hexagram2021.girlfriends.client.renderer.GirlfriendFishingHookRenderer;
import com.hexagram2021.girlfriends.client.renderer.GirlfriendRenderer;
import com.hexagram2021.girlfriends.client.screen.MainInteractionScreen;
import com.hexagram2021.girlfriends.common.entity.GirlfriendsEntities;
import com.hexagram2021.girlfriends.common.network.GirlfriendsNetwork;
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.registries.DeferredHolder;

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
     * 客户端初始化事件喵~
     *
     * @param event 客户端初始化事件喵~
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注入屏幕打开器，将客户端侧 UI 逻辑与 common 网络代码解耦，避免专用服务端加载客户端类喵~
        GirlfriendsNetwork.setScreenOpener(
                (id, summary) -> Minecraft.getInstance().setScreen(new MainInteractionScreen(id, summary))
        );
        // 注入语音播放 handler，将客户端侧声音播放与 common 网络代码解耦喵~
        GirlfriendsNetwork.setVoiceHandler(packet -> {
            DeferredHolder<SoundEvent, SoundEvent> holder = GirlfriendsVoiceManager.getVoice(packet.voiceKey());
            if (holder != null) {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                ClientLevel level = mc.level;
                if (player != null && level != null) {
                    level.playSound(
                            player,
                            packet.x(), packet.y(), packet.z(),
                            holder, SoundSource.NEUTRAL, 1.0F, 1.0F
                    );
                }
            }
        });
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
        event.registerLayerDefinition(GirlfriendsModelLayers.WANYING, GirlfriendModel::createShortBodyLayer);
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
        event.registerEntityRenderer(
                GirlfriendsEntities.YUXI.get(),
                ctx -> new GirlfriendRenderer(ctx, GirlfriendsModelLayers.YUXI, "yuxi")
        );
        event.registerEntityRenderer(
                GirlfriendsEntities.MEISHU.get(),
                ctx -> new GirlfriendRenderer(ctx, GirlfriendsModelLayers.MEISHU, "meishu")
        );
        event.registerEntityRenderer(
                GirlfriendsEntities.WANYING.get(),
                ctx -> new GirlfriendRenderer(ctx, GirlfriendsModelLayers.WANYING, "wanying")
        );
        event.registerEntityRenderer(
                GirlfriendsEntities.YOURUO.get(),
                ctx -> new GirlfriendRenderer(ctx, GirlfriendsModelLayers.YOURUO, "youruo")
        );
        event.registerEntityRenderer(
                GirlfriendsEntities.GIRLFRIEND_FISHING_HOOK.get(),
                GirlfriendFishingHookRenderer::new
        );
    }
}
