package com.easttown.text.registry;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import com.simibubi.create.foundation.render.SpecialModels;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.nbt.CompoundTag;

/**
 * HXD3C 转向架的 Flywheel 视觉（列车装配进 contraption 后的渲染路径）。
 *
 * <p>与 {@link Hxd3cBogeyRenderer} 使用同一组 PartialModel（静态构架/顶板 +
 * 3 组轮对绕 X 轴旋转），仅实例化方式不同：这里用 {@code TransformedInstance}
 * 在 GPU 端变换，零拷贝逐帧更新。
 */
public class Hxd3cBogeyVisual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance top;
    private final TransformedInstance[] wheels =
            new TransformedInstance[Hxd3cBogeyRenderer.WHEEL_ZS.length];

    public Hxd3cBogeyVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(Hxd3cBogeyRenderer.FRAME))
                .createInstance();
        top = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(Hxd3cBogeyRenderer.TOP))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(Hxd3cBogeyRenderer.WHEEL));
        for (int i = 0; i < wheels.length; i++)
            wheels[i] = wheelInstancer.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).setChanged();
      //  top.setTransform(poseStack).setChanged();
        for (int i = 0; i < wheels.length; i++)
            wheels[i].setTransform(poseStack)
                    .translate(0, Hxd3cBogeyRenderer.WHEEL_Y, Hxd3cBogeyRenderer.WHEEL_ZS[i])
                    .rotateXDegrees(wheelAngle)
                    .setChanged();
    }

    @Override
    public void hide() {
        frame.setZeroTransform().setChanged();
        top.setZeroTransform().setChanged();
        for (TransformedInstance wheel : wheels)
            wheel.setZeroTransform().setChanged();
    }

    @Override
    public void updateLight(int packedLight) {
        frame.light(packedLight).setChanged();
        top.light(packedLight).setChanged();
        for (TransformedInstance wheel : wheels)
            wheel.light(packedLight).setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(frame);
        consumer.accept(top);
        for (TransformedInstance wheel : wheels)
            consumer.accept(wheel);
    }

    @Override
    public void delete() {
        frame.delete();
        top.delete();
        for (TransformedInstance wheel : wheels)
            wheel.delete();
    }
}
