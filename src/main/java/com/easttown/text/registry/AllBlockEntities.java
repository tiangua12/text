package com.easttown.text.registry;

import javax.annotation.Nonnull;

import com.easttown.text.Text;
import com.easttown.text.content.bogey.Track43kgBogeyBlockEntity;
import com.easttown.text.content.track.node.NodeTrackBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体类型注册表：本类只做 BlockEntityType 一件事。
 *
 * <p>普通轨道（32kg / 43kg）**不在这里**：它们复用 Create 的
 * {@code AllBlockEntityTypes.TRACK}（见架构约束 C3）。这里只有两种自有类型：
 * <ul>
 *   <li>{@code track_43kg_bogey} —— 转向架。复用 {@code create:bogey} 会因
 *       {@code validBlocks} 校验抛异常，必须独立。</li>
 *   <li>{@code node_track} —— 节点轨道。角度是连续浮点，blockstate 装不下，
 *       只能存在自有 BE 里（C3 的这个例外已核对过安全性，见 NodeTrackBlockEntity 的类注释）。</li>
 * </ul>
 *
 * <p><b>不需要在这里配 ticker</b>：{@code IBE#getTicker} 的默认实现会检测
 * {@code getBlockEntityClass()} 是否属于 {@code SmartBlockEntity}，是则返回
 * {@code SmartBlockEntityTicker}。两个自有 BE 都继承 Create 的 {@code SmartBlockEntity} 系
 * （{@code StandardBogeyBlockEntity} / {@code TrackBlockEntity}），tick 自动到位。
 *
 * <p>{@link Track43kgBogeyBlockEntity#init(BlockEntityType)} 必须在本 lambda 内调用：
 * {@code BlockEntityType.Builder.of} 只接受两参工厂（pos, state），实体需要真实类型，
 * 因此用静态缓存把注册出来的类型回写进去。节点 BE 的构造器直接收 {@code BlockEntityType}，
 * 不需要这个手法。
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

    /**
     * 节点轨道 BE 类型。**当前节点系统已禁用，这里注册的是一个孤儿类型。**
     *
     * <p>为什么还注册：{@code NodeTrackBlock#getBlockEntityType()} 要返回它，删了字段整个类编译不过。
     *
     * <p>为什么 {@code validBlocks} 是空的：节点方块（{@code AllBlocks.NODE_TRACK}）已不再注册，
     * 这里传不了方块；空 {@code validBlocks} 意味着任何方块都通不过
     * {@code BlockEntity.validateBlockState}，所以就算有人把方块塞回注册表，这个 BE 也用不起来
     * ——正好是「代码保留、功能禁用」想要的效果。
     *
     * <p>恢复方法：把 {@code AllBlocks.NODE_TRACK.get()} 加回 {@code of(...)} 的参数。
     */
    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NodeTrackBlockEntity>> NODE_TRACK =
            BLOCK_ENTITY_TYPES.register("node_track", () -> {
                BlockEntityType<NodeTrackBlockEntity> type = BlockEntityType.Builder
                        .of(NodeTrackBlockEntity::new)
                        .build(null);
                NodeTrackBlockEntity.init(type);
                return type;
            });

    public static void register(@Nonnull IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private AllBlockEntities() {
    }
}
