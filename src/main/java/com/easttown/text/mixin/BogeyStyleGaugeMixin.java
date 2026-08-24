package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeyStyle;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 扳手切换转向架样式时禁止跨轨距：
 *
 * <p>Create 的扳手交互（{@code useItemOn}）会在 cycle group 内切换转向架样式。
 * 若当前样式轨距与目标样式轨距不同（43kg ↔ 标准），禁止切换——不同轨距的
 * 转向架不通用，不能用扳手把 43kg 转向架改成普通转向架（或反之）。
 *
 * <p>在 {@code getNextStyle(currentStyle)} 之后、{@code setBogeyStyle(style)} 之前
 * 拦截：若两者 {@code getTrackType} 不同，直接返回 PASS（不切换）。
 */
@Mixin(AbstractBogeyBlock.class)
public abstract class BogeyStyleGaugeMixin {

    @SuppressWarnings("null")
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void text$rejectCrossGaugeStyleSwitch(ItemStack stack, BlockState state, Level level,
                                                  net.minecraft.core.BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide)
            return;
        if (player.isShiftKeyDown() || !stack.is(com.simibubi.create.AllItems.WRENCH.get()))
            return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity sbbe))
            return;

        AbstractBogeyBlock<?> self = (AbstractBogeyBlock<?>) (Object) this;
        BogeyStyle current = sbbe.getStyle();
        BogeyStyle next = self.getNextStyle(current);
        if (next == null || next == current)
            return;

        var currentType = self.getTrackType(current);
        var nextType = self.getTrackType(next);
        if (currentType != null && nextType != null && currentType != nextType) {
            // 跨轨距 → 禁止切换
            cir.setReturnValue(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        }
    }
}
