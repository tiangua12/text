package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.easttown.text.registry.AllTrackMaterials;
import com.simibubi.create.content.trains.track.BezierConnection;

import net.minecraft.world.phys.Vec3;

/**
 * Create 弯道渲染把左右铁轨（segment_left / segment_right）硬编码摆放在
 * 轨道中心线两侧 ±0.965 格（BezierConnection.SegmentAngles 构造器），与
 * 原版"宽轨"几何配套（原版轨距约 1.93 格 ≈ 1.93 米）。
 *
 * <p>
 * 32kg 轨道采用国际标准轨距 1435mm（1 格 = 1 米）：轨距为两轨头
 * 内侧距离，铁轨中心偏移 = 1435/2 mm + 轨头半宽补偿 0.045 ≈ 0.7625 格/侧
 * （中心距约 1.525 格，内距 1435mm）。因此按材料把弯道偏移缩小，
 * 使直道与弯道的铁轨横向位置一致。
 *
 * <p>
 * 注入点：SegmentAngles 构造器内两处 {@code segment.normal.scale(.965f)}。
 * handler 按 scale 值过滤（避免误伤同构造器内其他 Vec3.scale 调用），
 * 并按 BezierConnection 的材料判断只影响本模组的轨道。
 */
@Mixin(targets = "com.simibubi.create.content.trains.track.BezierConnection$SegmentAngles")
public class BezierSegmentAnglesMixin {

	/**
	 * 32kg 轨道弯道铁轨相对轨道中心线的横向偏移（格）：
	 * 标准轨距 1435mm = 1.435 格内距，加轨头半宽补偿 0.045 → 每侧 0.7625 格。
	 */
	private static final double RAIL_OFFSET_32KG = 1.435 / 2 + 0.04;

	@Redirect(method = "<init>", expect = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 text$scaleRailOffset(Vec3 normal, double scale, BezierConnection bc) {
		if (scale > 0.9 && scale < 1.0 && (bc.getMaterial() == AllTrackMaterials.TRACK_32KG
				|| bc.getMaterial() == AllTrackMaterials.TRACK_43KG))
			return normal.scale(RAIL_OFFSET_32KG);
		return normal.scale(scale);
	}
}
