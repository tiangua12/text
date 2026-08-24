package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.easttown.text.registry.AllTrackMaterials;
import com.simibubi.create.content.trains.track.BezierConnection;

import net.minecraft.world.phys.Vec3;

/**
 * 与 BezierSegmentAnglesMixin 配套：弯道上方桁架梁（girder）的横向偏移
 * 同样硬编码 ±0.965（BezierConnection.GirderAngles 构造器）。32kg 轨道
 * 铁轨按现有模型对齐到 ±0.25；梁按本模组设定为 ±1.0 格（略宽于铁轨，
 * 梁架在轨枕两侧），与原版轨道区分。
 *
 * <p>
 * 注入点：GirderAngles 构造器内共 4 处 Vec3.scale 调用（±0.965 两处 +
 * 梁的 -8/16、-10/16 两处），handler 按 scale 值过滤只改 0.965 附近的。
 */
@Mixin(targets = "com.simibubi.create.content.trains.track.BezierConnection$GirderAngles")
public class BezierGirderAnglesMixin {

	/** 32kg 轨道弯道梁相对轨道中心线的横向偏移（格），本模组设定为 1.0。 */
	private static final double GIRDER_OFFSET_32KG = 1.435 / 2 + 0.04;

	@Redirect(method = "<init>", expect = 4, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 text$scaleGirderOffset(Vec3 normal, double scale, BezierConnection bc) {
		if (scale > 0.9 && scale < 1.0 &&
				(bc.getMaterial() == AllTrackMaterials.TRACK_32KG ||
						bc.getMaterial() == AllTrackMaterials.TRACK_43KG)) {
			return normal.scale(GIRDER_OFFSET_32KG); // 目前两者都返回相同的偏移量
		}
		return normal.scale(scale);
	}
}
