package com.easttown.text;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.easttown.text.command.TextCommands;
import com.easttown.text.net.TextNetwork;
import com.easttown.text.registry.AllBlockEntities;
import com.easttown.text.registry.AllBlocks;
import com.easttown.text.registry.AllCreativeTabs;
import com.easttown.text.registry.AllItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Text.MODID)
public class Text {
    public static final String MODID = "text";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 注册表按"一个概念一个类"拆分：Blocks / Items / BlockEntities / CreativeTabs。
     *
     * <p>书写顺序与依赖方向一致（Blocks → Items → BlockEntities → CreativeTabs）：
     * 后者的注册 lambda 才去 {@code get()} 前者的 holder，字段初始化阶段不互相 deref，
     * 因此实际注册顺序不影响结果。
     */
    public Text(@Nonnull IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        AllBlocks.register(modEventBus);
        AllItems.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllCreativeTabs.register(modEventBus);
        modEventBus.addListener(Config::onConfigEvent);
        // /textbox 指令与列车碰撞盒显示用的自定义包
        modEventBus.addListener(TextNetwork::register);
        NeoForge.EVENT_BUS.addListener(TextCommands::register);
    }
}
