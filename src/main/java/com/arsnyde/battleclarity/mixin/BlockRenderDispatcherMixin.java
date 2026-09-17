package com.arsnyde.battleclarity.mixin;

import com.arsnyde.battleclarity.HiddenBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops hidden blocks out of the chunk geometry as it is built.
 *
 * <p>These methods run on the chunk-build worker threads, so the lookup has to be thread safe - see
 * {@link HiddenBlocks}. The set is empty outside of battle, which makes this a single volatile read.
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void battleclarity$skipHiddenBlock(BlockState state,
                                               BlockPos pos,
                                               BlockAndTintGetter level,
                                               PoseStack poseStack,
                                               VertexConsumer consumer,
                                               boolean checkSides,
                                               RandomSource random,
                                               CallbackInfo ci) {
        if (HiddenBlocks.isHidden(pos.asLong())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    private void battleclarity$skipHiddenLiquid(BlockPos pos,
                                                BlockAndTintGetter level,
                                                VertexConsumer consumer,
                                                BlockState state,
                                                FluidState fluidState,
                                                CallbackInfo ci) {
        if (HiddenBlocks.isHidden(pos.asLong())) {
            ci.cancel();
        }
    }
}
