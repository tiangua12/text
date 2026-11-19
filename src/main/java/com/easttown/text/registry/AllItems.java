package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.content.trains.track.TrackBlockItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册表：本类只做 Item 一件事。
 *
 * <p>依赖方向：{@code AllItems → AllBlocks}（构造器里取方块 holder，全部在注册 lambda 内求值，
 * 此时方块注册已完成；字段初始化阶段不 deref）。
 *
 * <p>轨道物品必须是 {@link TrackBlockItem}（Create 的放置逻辑 {@code useOn} 依赖它），
 * 转向架物品用普通 {@link BlockItem}。
 */
public final class AllItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Text.MODID);

    public static final DeferredHolder<Item, TrackBlockItem> TRACK_32KG = ITEMS.register(
            "track_32kg", () -> new TrackBlockItem(AllBlocks.TRACK_32KG.get(), new Item.Properties()));

    public static final DeferredHolder<Item, TrackBlockItem> TRACK_43KG = ITEMS.register(
            "track_43kg", () -> new TrackBlockItem(AllBlocks.TRACK_43KG.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> TRACK_43KG_BOGEY = ITEMS.register(
            "track_43kg_bogey", () -> new BlockItem(AllBlocks.TRACK_43KG_BOGEY.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> TRACK_43KG_LARGE_BOGEY = ITEMS.register(
            "track_43kg_large_bogey",
            () -> new BlockItem(AllBlocks.TRACK_43KG_LARGE_BOGEY.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private AllItems() {
    }
}
