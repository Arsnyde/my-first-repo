package com.arsnyde.battleclarity.mixin.sodium;

import com.arsnyde.battleclarity.HiddenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium chunk meshing: the exact counterpart of
 * {@link com.arsnyde.battleclarity.mixin.RenderChunkRegionMixin}.
 *
 * <p>Sodium replaces vanilla's chunk mesher wholesale, so the vanilla hook never fires when it is
 * installed. Its mesher reads every block through {@code LevelSlice#getBlockState(int, int, int)} -
 * blocks, fluids, block entities and the occlusion graph all come from that one accessor, and the
 * {@code BlockPos} overload delegates to it - so a single hook gives the same behaviour as vanilla.
 *
 * <p>Targeted by name, and the signature is entirely vanilla types, so this compiles with no dependency
 * on Sodium at all. It is only applied when Sodium is actually present - see
 * {@link com.arsnyde.battleclarity.compat.SodiumMixinPlugin}.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class LevelSliceMixin {

    @Inject(
            method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void battleclarity$hideBlock(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        if (HiddenBlocks.isHidden(BlockPos.asLong(x, y, z))) {
            cir.setReturnValue(HiddenBlocks.HIDDEN_STATE);
        }
    }
}
