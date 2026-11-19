package com.easttown.text.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.easttown.text.content.train.CarriageOffsets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

/**
 * 渲染侧的竖直偏移：抬 PoseStack，不动实体坐标。
 *
 * <p>Create 6.x 的两条渲染路径最终都收敛到 {@code applyLocalTransforms}：
 * <ul>
 * <li>经典路径：{@code ContraptionEntityRenderer#render} → {@code ContraptionMatrices#setup}
 * → {@code entity.applyLocalTransforms(model, pt)}；</li>
 * <li>Flywheel 路径：{@code ContraptionVisual#setEmbeddingMatrices} 先 translate 到实体位置，
 * 再 {@code entity.applyLocalTransforms(contraptionMatrix, pt)}，整个可视化 embedding
 * （方块、方块实体视觉、actors）都乘这个矩阵。</li>
 * </ul>
 * 在 HEAD 插一句 translate，两条路径同时生效，且只影响 contraption 主体——
 * 转向架是 {@code CarriageContraptionEntityRenderer} 在 {@code super.render} 之后
 * 用 {@code translateBogey} 单独定位的，不吃这个矩阵，所以轮对不会跟着浮起来。
 *
 * <p>必须放在 HEAD：方法第一句是 {@code translate(-.5f, 0, -.5f)}，把它当成
 * 局部坐标系的重新对中，我们的偏移要落在它之前（世界方向的竖直平移）。
 */
@Mixin(OrientedContraptionEntity.class)
public abstract class ContraptionRenderOffsetMixin {

    @Inject(method = "applyLocalTransforms", at = @At("HEAD"))
    private void text$offsetRender(PoseStack matrixStack, float partialTicks, CallbackInfo ci) {
        float offset = CarriageOffsets.of((AbstractContraptionEntity) (Object) this);
        if (offset != 0)
            matrixStack.translate(0, offset, 0);
    }
}
