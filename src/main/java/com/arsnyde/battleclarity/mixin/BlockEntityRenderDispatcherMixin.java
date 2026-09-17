package com.arsnyde.battleclarity.mixin;

import com.arsnyde.battleclarity.HiddenBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block entities (chests, signs, beds, banners) are drawn every frame outside the chunk geometry, so
 * they need hiding separately or they would float in the hole left by the wall around them.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void battleclarity$skipHiddenBlockEntity(BlockEntity blockEntity,
                                                     float partialTick,
                                                     PoseStack poseStack,
                                                     MultiBufferSource bufferSource,
                                                     CallbackInfo ci) {
        if (HiddenBlocks.isActive() && HiddenBlocks.isHidden(blockEntity.getBlockPos().asLong())) {
            ci.cancel();
        }
    }
}
