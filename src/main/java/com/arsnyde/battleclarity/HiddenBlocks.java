package com.arsnyde.battleclarity;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The set of block positions that are currently being hidden, keyed by {@link net.minecraft.core.BlockPos#asLong()}.
 *
 * <p>This is read from chunk-build worker threads (via the render mixins) and written from the client
 * thread. We never mutate a set after publishing it: {@link #publish} swaps in a brand new set behind a
 * volatile field, so readers either see the whole old set or the whole new one and no locking is needed.
 */
public final class HiddenBlocks {

    /**
     * What the chunk mesher is told a hidden block is. Air makes the mesher skip the model, the fluid,
     * the block entity and the occlusion entry all at once, and lets neighbouring blocks render the faces
     * they would otherwise cull against it.
     */
    public static final BlockState HIDDEN_STATE = Blocks.AIR.defaultBlockState();

    private static volatile LongSet hidden = LongSets.emptySet();

    private HiddenBlocks() {
    }

    /**
     * @return true if the block at the given packed position should be skipped by the renderer.
     */
    public static boolean isHidden(long packedPos) {
        // Single volatile read, then a plain lookup on an effectively-immutable set.
        LongSet snapshot = hidden;
        return !snapshot.isEmpty() && snapshot.contains(packedPos);
    }

    /** @return true when at least one block is currently hidden. Cheap early-out for callers. */
    public static boolean isActive() {
        return !hidden.isEmpty();
    }

    /** Replaces the published set. The caller must not touch {@code next} afterwards. */
    static void publish(LongOpenHashSet next) {
        hidden = next.isEmpty() ? LongSets.emptySet() : next;
    }

    /** Drops every hidden block without scheduling a re-render. Used when the level goes away. */
    static void clearSilently() {
        hidden = LongSets.emptySet();
    }

    static LongSet current() {
        return hidden;
    }
}
