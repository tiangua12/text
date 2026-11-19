package com.easttown.text.client;

import com.easttown.text.net.TrainBoxPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 客户端侧的"列车碰撞体显示"开关（位掩码）。
 *
 * <p>位定义见 {@link TrainBoxPayload}：{@code FLAG_BOXES} 不规则碰撞体、
 * {@code FLAG_HULL} 外框 OBB、{@code FLAG_AABB} 实体碰撞箱。
 *
 * <p>默认全关：{@code /textbox} 一个命令就能打开最常用的那一层（不规则碰撞体）。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientTrainBoxState {

    private static int flags;

    public static int flags() {
        return flags;
    }

    public static boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static void accept(int op) {
        if (op == TrainBoxPayload.OP_ALL_OFF)
            flags = 0;
        else
            flags ^= op;
    }

    private ClientTrainBoxState() {
    }
}
