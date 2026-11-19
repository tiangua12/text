package com.easttown.text.net;

import com.easttown.text.client.ClientTrainBoxState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 网络包注册。
 *
 * <p>只有一个客户端向的包（{@link TrainBoxPayload}）。{@link RegisterPayloadHandlersEvent} 的
 * javadoc 明确写了 {@code PayloadRegistrar} 会把手动包一层"在主线程执行"，
 * 所以这里不需要（{@code IPayloadContext} 也没有）{@code enqueueWork} 之类的调度。
 *
 * <p>handler 里对客户端类的引用是安全的：专用服务端不会收到 clientbound 包，
 * 该方法体永远不会执行，JVM 也就不会去加载 {@link ClientTrainBoxState}（链接是懒的）。
 * {@link FMLEnvironment#dist} 判断只是多一层保险。
 */
public final class TextNetwork {

    /** 网络版本号：协议不兼容时双方会断开而不是解析出错误数据。 */
    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToClient(TrainBoxPayload.TYPE, TrainBoxPayload.STREAM_CODEC, TextNetwork::handleToggle);
    }

    private static void handleToggle(TrainBoxPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientTrainBoxState.accept(payload.op());
    }

    private TextNetwork() {
    }
}
