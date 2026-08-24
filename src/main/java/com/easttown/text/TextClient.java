package com.easttown.text;

import com.easttown.text.registry.AllTracks;
import com.easttown.text.registry.Track43kgBogeyBlockEntity;
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
        // Flywheel visual：43kg 转向架 BE 复用 create 的 BogeyBlockEntityVisual
        VisualizerRegistry.setVisualizer(AllTracks.TRACK_43KG_BOGEY_BE.get(),
                new BlockEntityVisualizer<>() {
                    @Override
                    public BlockEntityVisual<? super Track43kgBogeyBlockEntity> createVisual(
                            VisualizationContext ctx, Track43kgBogeyBlockEntity blockEntity, float partialTick) {
                        return new BogeyBlockEntityVisual(ctx, blockEntity, partialTick);
                    }

                    @Override
                    public boolean skipVanillaRender(Track43kgBogeyBlockEntity blockEntity) {
                        return false;
                    }
                });
    }

    /** 43kg 转向架 BE 渲染器（复用 create 的 BogeyBlockEntityRenderer）。 */
    private static void registerBogeyRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AllTracks.TRACK_43KG_BOGEY_BE.get(),
                BogeyBlockEntityRenderer::new);
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
