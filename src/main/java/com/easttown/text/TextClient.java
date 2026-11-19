package com.easttown.text;

import com.easttown.text.client.NodeTrackRenderer;
import com.easttown.text.client.TrainBoxRenderer;
import com.easttown.text.content.bogey.Track43kgBogeyBlockEntity;
import com.easttown.text.registry.AllBlockEntities;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.bogey.BogeyBlockEntityRenderer;
import com.simibubi.create.content.trains.bogey.BogeyBlockEntityVisual;
import com.simibubi.create.content.trains.track.TrackModel;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Text.MODID, dist = Dist.CLIENT)
public class TextClient {
    public TextClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        registerTrackModelSwaps();
        IEventBus modBus = container.getEventBus();
        if (modBus != null)
            modBus.addListener(TextClient::registerBogeyRenderers);
        // /textbox 的线框走游戏总线（RenderLevelStageEvent 在 NeoForge.EVENT_BUS 上）
        NeoForge.EVENT_BUS.addListener(TrainBoxRenderer::onRenderLevel);
    }

    /**
     * 43kg 转向架 BE 渲染器与 Flywheel visual（复用 create 的实现）。
     * 该事件在注册阶段之后触发，此时 {@code text:track_43kg_bogey} BE 类型已绑定。
     */
    @SuppressWarnings("null")
    private static void registerBogeyRenderers(EntityRenderersEvent.RegisterRenderers event) {
        var bogeyType = AllBlockEntities.TRACK_43KG_BOGEY.get();
        event.registerBlockEntityRenderer(bogeyType, BogeyBlockEntityRenderer::new);

        // 节点轨道：曲线与轴向箭头都由它画（LOCKED 后方块模型指向 air，没有别的渲染）
        event.registerBlockEntityRenderer(AllBlockEntities.NODE_TRACK.get(), NodeTrackRenderer::new);

        VisualizerRegistry.setVisualizer(bogeyType,
                new BlockEntityVisualizer<>() {
                    @Override
                    public BlockEntityVisual<? super Track43kgBogeyBlockEntity> createVisual(
                            VisualizationContext ctx, Track43kgBogeyBlockEntity blockEntity, float partialTick) {
                        return new BogeyBlockEntityVisual(ctx, blockEntity, partialTick);
                    }

                    @Override
                    public boolean skipVanillaRender(Track43kgBogeyBlockEntity blockEntity) {
                        // 必须返回 true：否则经典 BE 渲染器（BogeyBlockEntityRenderer）会与
                        // Flywheel 视觉同时渲染，转向架被画两次（HXD3C 三轴变"六轴"）。
                        return true;
                    }
                });
    }

    /**
     * Registers the TrackModel wrapper for the track_32kg / track_43kg blocks, so
     * ascending track states render with the tilt transform. Key = block registry id
     * (single key), matching create's CreateRegistrate.blockModel(...) usage
     * (CreateRegistrate.java L298-302: register(getKeyOrThrow(entry), func.get())).
     */
    private static void registerTrackModelSwaps() {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_32kg"),
                        TrackModel::new);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"),
                        TrackModel::new);
    }
}
