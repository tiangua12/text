package com.easttown.text;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.easttown.text.registry.AllTracks;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(Text.MODID)
public class Text {
    public static final String MODID = "text";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Text(@Nonnull IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        AllTracks.BLOCKS.register(modEventBus);
        AllTracks.ITEMS.register(modEventBus);
        AllTracks.BLOCK_ENTITY_TYPES.register(modEventBus);
        AllTracks.TABS.register(modEventBus);
        modEventBus.addListener(Config::onConfigEvent);
    }
}
