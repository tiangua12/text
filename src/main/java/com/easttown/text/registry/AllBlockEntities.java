package com.easttown.text.registry;

import javax.annotation.Nonnull;

import com.easttown.text.Text;
import com.easttown.text.content.bogey.Track43kgBogeyBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体类型注册表：本类只做 BlockEntityType 一件事。
 *
 * <p>轨道方块**不在这里**：32kg/43kg 轨道复用 Create 的 {@code AllBlockEntityTypes.TRACK}
 * （见架构约束 C3），只有 43kg 转向架需要独立 BE 类型。
 *
 * <p>独立类型是必需的：复用 {@code create:bogey} 会因 {@code validBlocks} 校验抛异常。
 *
 * <p>{@link Track43kgBogeyBlockEntity#init(BlockEntityType)} 必须在本 lambda 内调用：
 * {@code BlockEntityType.Builder.of} 只接受两参工厂（pos, state），实体需要真实类型，
 * 因此用静态缓存把注册出来的类型回写进去。
 */
public final class AllBlockEntities {

    @SuppressWarnings("null")
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Text.MODID);

    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Track43kgBogeyBlockEntity>> TRACK_43KG_BOGEY =
            BLOCK_ENTITY_TYPES.register("track_43kg_bogey", () -> {
                BlockEntityType<Track43kgBogeyBlockEntity> type = BlockEntityType.Builder
                        .of(Track43kgBogeyBlockEntity::new,
                                AllBlocks.TRACK_43KG_BOGEY.get(),
                                AllBlocks.TRACK_43KG_LARGE_BOGEY.get())
                        .build(null);
                Track43kgBogeyBlockEntity.init(type);
                return type;
            });

    public static void register(@Nonnull     IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private AllBlockEntities() {
    }
}
