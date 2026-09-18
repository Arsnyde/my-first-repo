package com.arsnyde.battleclarity.mixin;

import com.arsnyde.battleclarity.HiddenBlocks;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla chunk meshing: report hidden blocks as air to the mesher.
 *
 * <p>Hooking the block accessor rather than the draw call means one hook covers everything the mesher
 * derives from the block state - the model, the fluid, the block entity and the occlusion graph all fall
 * away on their own. It also fixes face culling: the surrounding blocks now see air where the hidden
 * block was and render their inner faces, instead of culling against a block that is no longer drawn and
 * leaving a see-through gap.
 *
 * <p>This is scoped to the chunk-build region, so it only affects rendering. The real world state is
 * untouched - collision, raycasts, redstone and the server all still see the block.
 *
 * <p>Runs on chunk-build worker threads; see {@link HiddenBlocks} for why the lookup is safe there.
 */
@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin {

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void battleclarity$hideBlock(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (HiddenBlocks.isHidden(pos.asLong())) {
            cir.setReturnValue(HiddenBlocks.HIDDEN_STATE);
        }
    }
}
