package com.easttown.text;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.track.TrackModel;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Text.MODID, dist = Dist.CLIENT)
public class TextClient {
    public TextClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        registerTrackModelSwaps();
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
