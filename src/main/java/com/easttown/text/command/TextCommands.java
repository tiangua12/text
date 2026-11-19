package com.easttown.text.command;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.easttown.text.net.TrainBoxPayload;
import com.easttown.text.util.TrainBoxGeometry;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /textbox} —— 显示列车车厢的碰撞体。
 *
 * <pre>
 * /textbox          切换「不规则碰撞体」（逐方块碰撞形状，青色）—— 默认目标
 * /textbox boxes    同上（显式）
 * /textbox hull     切换「整体外框 OBB」（黄色，仅定位用）
 * /textbox aabb     切换「实体碰撞箱」（绿色）
 * /textbox off      全关
 * /textbox dump     打印附近每节车厢的方块数 / 碰撞盒数 / 包围盒数值
 * </pre>
 *
 * <p><b>为什么主显示是"不规则"的</b>：Create 的真实碰撞体由
 * {@code Contraption#invalidateColliders()} 逐方块生成——每个方块取
 * {@code getCollisionShape(...)} 再 {@code forAllBoxes} 拆成若干盒子。楼梯、台阶、栅栏
 * 因此保留本来的形状。外框 OBB / AABB 都只是它的包围盒，画出来是为了定位方便，
 * 并不参与实际碰撞判定（详见 {@link TrainBoxGeometry} 类注释）。
 *
 * <p>开关状态存在客户端（位掩码），本指令只把"要切哪一层"发下去；
 * {@code dump} 才需要服务端数据，直接用服务端那边的车厢实体算。
 * 限权限等级 2（OP），避免普通玩家误触。
 */
public final class TextCommands {

    /** dump 时搜索车厢的半径（格）。 */
    private static final double SCAN_RADIUS = 96;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("textbox")
                // 调试指令：限 OP（权限等级 2），不给普通玩家开放
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> request(ctx.getSource(), TrainBoxPayload.FLAG_BOXES))
                .then(Commands.literal("boxes")
                        .executes(ctx -> request(ctx.getSource(), TrainBoxPayload.FLAG_BOXES)))
                .then(Commands.literal("hull")
                        .executes(ctx -> request(ctx.getSource(), TrainBoxPayload.FLAG_HULL)))
                .then(Commands.literal("aabb")
                        .executes(ctx -> request(ctx.getSource(), TrainBoxPayload.FLAG_AABB)))
                .then(Commands.literal("off")
                        .executes(ctx -> request(ctx.getSource(), TrainBoxPayload.OP_ALL_OFF)))
                .then(Commands.literal("dump")
                        .executes(ctx -> dump(ctx.getSource()))));
    }

    private static int request(CommandSourceStack source, int op) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/textbox 需要由玩家执行"));
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new TrainBoxPayload(op));
        String action = switch (op) {
            case TrainBoxPayload.FLAG_AABB -> "切换实体碰撞箱（绿）";
            case TrainBoxPayload.FLAG_HULL -> "切换整体外框 OBB（黄）";
            case TrainBoxPayload.FLAG_BOXES -> "切换不规则碰撞体（青）";
            default -> "全关";
        };
        source.sendSuccess(() -> Component.literal("列车碰撞体显示：" + action), false);
        return 1;
    }

    private static int dump(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        List<CarriageContraptionEntity> carriages = level.getEntitiesOfClass(
                CarriageContraptionEntity.class,
                AABB.ofSize(origin, SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS));
        if (carriages.isEmpty()) {
            source.sendFailure(Component.literal("半径 " + (int) SCAN_RADIUS + " 格内没有车厢实体"));
            return 0;
        }
        carriages.sort(Comparator.comparingDouble(cce -> cce.position().distanceToSqr(origin)));

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "=== 车厢碰撞体 %d 节（按距离排序）===", carriages.size())), false);

        int index = 0;
        for (CarriageContraptionEntity cce : carriages) {
            Contraption contraption = cce.getContraption();
            if (contraption == null)
                continue;

            int id = index++;
            int blockCount = contraption.getBlocks().size();
            int colliderCount = TrainBoxGeometry.collisionBoxes(cce, 1.0F).size();
            AABB aabb = cce.getBoundingBox();
            TrainBoxGeometry.CarriageHull hull = TrainBoxGeometry.hull(cce, 1.0F);
            Vec3 extents = hull == null ? Vec3.ZERO : hull.obbExtents();

            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "#%d 方块=%d 碰撞盒=%d | AABB[%.3f, %.3f, %.3f]~[%.3f, %.3f, %.3f] | 外壳 e=(%.3f, %.3f, %.3f)",
                    id, blockCount, colliderCount,
                    aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                    extents.x, extents.y, extents.z)), false);
        }
        if (index == 0) {
            source.sendFailure(Component.literal("这些车厢实体都还没有 contraption 数据"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "青=不规则碰撞体（逐方块真实形状） 黄=外框OBB 绿=实体AABB；/textbox off 关闭"), false);
        return index;
    }

    private TextCommands() {
    }
}
