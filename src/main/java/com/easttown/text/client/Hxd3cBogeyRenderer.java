package com.easttown.text.client;

import com.easttown.text.Text;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HXD3C 电力机车转向架渲染器（经典渲染路径，非 Flywheel）。
 *
 * <p>
 * 素材（Blockbench 导出，真实比例）：
 * <ul>
 * <li>{@code hxd3c_bogey_frame} —— 构架主体（静态）；</li>
 * <li>{@code hxd3c_bogey_top} —— 顶部摇枕/空气弹簧座（静态）；</li>
 * <li>{@code hxd3c_bogey_wheel} —— 轮对（一对轮子，模型原点在轮轴中心，
 * 绕 X 轴旋转 {@code wheelAngle} 模拟滚动）。</li>
 * </ul>
 * HXD3C 为 Co-Co 轴式：一台转向架 3 根车轴，轮距 2.22（{@link #WHEEL_ZS}）。
 * 渲染器把同一轮对模型在 3 个轴位各放一份，随 {@code wheelAngle} 同步旋转。
 *
 * <p>
 * 模型与纹理位于 {@code text:models/block/hxd3c/bogey/*}、
 * {@code text:textures/block/hxd3c/bogey/*}；命名空间已由 qr: 改写为 text:。
 */
public class Hxd3cBogeyRenderer implements BogeyRenderer {

        /** 三根车轴的 z 位置（相对转向架中心的模型坐标，轮距 2.22）。 */
        public static final float[] WHEEL_ZS = { -2.22f, 0f, 2.22f };

        /**
         * 轮对中心相对模型原点的 y 偏移。经典路径与 Flywheel 路径必须共用此值，
         * 否则放置状态与装配后状态轮对位置不一致。
         */
        public static final float WHEEL_Y = 1f;

        static final PartialModel FRAME = PartialModel.of(
                        ResourceLocation.fromNamespaceAndPath(Text.MODID, "block/hxd3c/bogey/hxd3c_bogey_frame"));
        static final PartialModel TOP = PartialModel.of(
                        ResourceLocation.fromNamespaceAndPath(Text.MODID, "block/hxd3c/bogey/hxd3c_bogey_top"));
        static final PartialModel WHEEL = PartialModel.of(
                        ResourceLocation.fromNamespaceAndPath(Text.MODID, "block/hxd3c/bogey/hxd3c_bogey_wheel"));

        @SuppressWarnings("null")
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int light, int overlay, boolean inContraption) {
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
                BlockState air = Blocks.AIR.defaultBlockState();

                CachedBuffers.partial(FRAME, air)
                                .light(light)
                                .overlay(overlay)
                                .renderInto(poseStack, buffer);

            //    CachedBuffers.partial(TOP, air)
            //                    .light(light)
            //                    .overlay(overlay)
            //                    .renderInto(poseStack, buffer);

                for (float z : WHEEL_ZS) {
                        CachedBuffers.partial(WHEEL, air)
                                        .translate(0, WHEEL_Y, z)
                                        .rotateXDegrees(wheelAngle)
                                        .light(light)
                                        .overlay(overlay)
                                        .renderInto(poseStack, buffer);
                }
        }
}
