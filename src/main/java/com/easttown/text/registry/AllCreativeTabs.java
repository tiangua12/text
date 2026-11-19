package com.easttown.text.registry;

import com.easttown.text.Text;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册表：本类只做 CreativeModeTab 一件事。
 *
 * <p>依赖方向：{@code AllCreativeTabs → AllItems}（图标与展示项在 lambda 内取，
 * 此时物品注册已完成）。
 *
 * <p>{@code displayItems} 里只放物品：方块物品本身即可放置，同时再放 block 会产生重复条目。
 */
@SuppressWarnings("null")
public final class AllCreativeTabs {


    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Text.MODID);


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEXT = TABS.register("text",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.text"))
                    .icon(() -> new ItemStack(AllItems.TRACK_43KG.get()))
                    .displayItems((params, output) -> {
                        output.accept(AllItems.TRACK_32KG.get());
                        output.accept(AllItems.TRACK_43KG.get());
                        output.accept(AllItems.TRACK_43KG_BOGEY.get());
                        output.accept(AllItems.TRACK_43KG_LARGE_BOGEY.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private AllCreativeTabs() {
    }
}
