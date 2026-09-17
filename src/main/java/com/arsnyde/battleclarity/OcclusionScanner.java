package com.arsnyde.battleclarity;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Works out which blocks have to disappear for the player to see a battle.
 *
 * <p>Two rules produce the hidden set:
 * <ol>
 *   <li><b>Body rule</b> - any block a battler's model occupies or phases through is hidden. This is
 *       unconditional; a Charizard wedged into a villager house's ceiling gets that ceiling removed.</li>
 *   <li><b>Sight rule</b> - a voxel ray is walked from the camera to a lattice of points spread over each
 *       battler's silhouette. Every view-blocking block those rays pass through is hidden.</li>
 * </ol>
 *
 * <p>Both rules are overridden by the floor protection: the block each battler stands on, and everything
 * below the lowest battler's feet, is never hidden. That keeps the ground the player and Pokemon are
 * standing on intact, so the battle never looks like it is floating over a void.
 */
public final class OcclusionScanner {

    /** One participant in the battle. */
    public record Battler(AABB hitbox, AABB modelBox) {
    }

    /** How far below a battler's feet we look for the block it is standing on. */
    private static final double SUPPORT_EPS = 0.0625D;

    private OcclusionScanner() {
    }

    public static LongOpenHashSet scan(Level level, Vec3 camera, List<Battler> battlers) {
        LongOpenHashSet out = new LongOpenHashSet();
        if (battlers.isEmpty()) {
            return out;
        }

        Ctx ctx = new Ctx();
        ctx.level = level;
        ctx.camera = camera;
        ctx.out = out;
        ctx.hideNonSolid = ClarityConfig.HIDE_NON_SOLID.get();
        ctx.maxBlocks = ClarityConfig.MAX_HIDDEN_BLOCKS.get();
        double maxDistance = ClarityConfig.MAX_DISTANCE.get();
        ctx.maxDistSq = maxDistance * maxDistance;
        ctx.minY = level.getMinBuildHeight();
        ctx.maxY = level.getMaxBuildHeight();

        // --- floor protection -------------------------------------------------------------------
        int floorCutoff = Integer.MAX_VALUE;
        for (Battler battler : battlers) {
            floorCutoff = Math.min(floorCutoff, Mth.floor(battler.hitbox().minY - SUPPORT_EPS));
        }
        ctx.floorCutoff = floorCutoff;

        LongOpenHashSet protectedPositions = new LongOpenHashSet();
        for (Battler battler : battlers) {
            collectSupportBlocks(battler.hitbox(), protectedPositions);
        }
        ctx.protectedPositions = protectedPositions;

        // --- rule 1: blocks the models are inside of --------------------------------------------
        for (Battler battler : battlers) {
            if (!ctx.hasBudget()) {
                break;
            }
            fillBox(ctx, battler.modelBox());
        }

        // --- rule 2: blocks between the camera and each silhouette -------------------------------
        double margin = ClarityConfig.SIGHT_MARGIN.get();
        int density = ClarityConfig.SAMPLE_DENSITY.get();
        for (Battler battler : battlers) {
            if (!ctx.hasBudget()) {
                break;
            }
            AABB sightBox = battler.modelBox().inflate(margin);
            // If the camera sits inside the battler there is nothing between them.
            if (sightBox.contains(camera)) {
                continue;
            }
            traceSilhouette(ctx, sightBox, density);
        }

        // --- rule 3: feather the edges so the opening reads as a window --------------------------
        int dilation = ClarityConfig.DILATION.get();
        if (dilation > 0 && ctx.hasBudget()) {
            dilate(ctx, dilation);
        }

        return out;
    }

    /** Adds every block position directly underneath a battler's footprint to the protected set. */
    private static void collectSupportBlocks(AABB hitbox, LongOpenHashSet protectedPositions) {
        int y = Mth.floor(hitbox.minY - SUPPORT_EPS);
        int minX = Mth.floor(hitbox.minX);
        int maxX = Mth.floor(hitbox.maxX - 1.0E-7D);
        int minZ = Mth.floor(hitbox.minZ);
        int maxZ = Mth.floor(hitbox.maxZ - 1.0E-7D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                protectedPositions.add(BlockPos.asLong(x, y, z));
            }
        }
    }

    /** Rule 1: hide every block the given box overlaps. */
    private static void fillBox(Ctx ctx, AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY - 1.0E-7D);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!ctx.hasBudget()) {
                        return;
                    }
                    ctx.tryAdd(x, y, z);
                }
            }
        }
    }

    /**
     * Rule 2: trace from the camera to a lattice of points on the surface of {@code box}. Interior
     * lattice points are skipped - they sit behind the surface points and would only cost time.
     */
    private static void traceSilhouette(Ctx ctx, AABB box, int density) {
        int n = Math.max(2, density);
        double last = n - 1.0D;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    boolean onSurface = i == 0 || i == n - 1
                            || j == 0 || j == n - 1
                            || k == 0 || k == n - 1;
                    if (!onSurface) {
                        continue;
                    }
                    if (!ctx.hasBudget()) {
                        return;
                    }
                    double x = Mth.lerp(i / last, box.minX, box.maxX);
                    double y = Mth.lerp(j / last, box.minY, box.maxY);
                    double z = Mth.lerp(k / last, box.minZ, box.maxZ);
                    traceRay(ctx, ctx.camera, x, y, z);
                }
            }
        }
    }

    /**
     * Amanatides &amp; Woo voxel traversal from {@code from} to the given point, hiding every
     * view-blocking block on the way.
     */
    private static void traceRay(Ctx ctx, Vec3 from, double toX, double toY, double toZ) {
        double dx = toX - from.x;
        double dy = toY - from.y;
        double dz = toZ - from.z;

        int x = Mth.floor(from.x);
        int y = Mth.floor(from.y);
        int z = Mth.floor(from.z);
        int endX = Mth.floor(toX);
        int endY = Mth.floor(toY);
        int endZ = Mth.floor(toZ);

        int stepX = Double.compare(dx, 0.0D);
        int stepY = Double.compare(dy, 0.0D);
        int stepZ = Double.compare(dz, 0.0D);

        double tDeltaX = stepX == 0 ? Double.MAX_VALUE : Math.abs(1.0D / dx);
        double tDeltaY = stepY == 0 ? Double.MAX_VALUE : Math.abs(1.0D / dy);
        double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : Math.abs(1.0D / dz);

        double tMaxX = stepX == 0 ? Double.MAX_VALUE
                : (stepX > 0 ? (x + 1 - from.x) : (from.x - x)) / Math.abs(dx);
        double tMaxY = stepY == 0 ? Double.MAX_VALUE
                : (stepY > 0 ? (y + 1 - from.y) : (from.y - y)) / Math.abs(dy);
        double tMaxZ = stepZ == 0 ? Double.MAX_VALUE
                : (stepZ > 0 ? (z + 1 - from.z) : (from.z - z)) / Math.abs(dz);

        // The voxel the camera itself sits in.
        ctx.tryAdd(x, y, z);

        // A ray can never cross more voxels than the Manhattan distance between its endpoints.
        int maxSteps = Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z);
        for (int step = 0; step < maxSteps; step++) {
            if (!ctx.hasBudget()) {
                return;
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                if (tMaxX > 1.0D) {
                    return;
                }
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                if (tMaxY > 1.0D) {
                    return;
                }
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                if (tMaxZ > 1.0D) {
                    return;
                }
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            ctx.tryAdd(x, y, z);
        }
    }

    /** Rule 3: grow the hidden set by {@code radius} blocks in every direction. */
    private static void dilate(Ctx ctx, int radius) {
        long[] seeds = ctx.out.toLongArray();
        for (long seed : seeds) {
            if (!ctx.hasBudget()) {
                return;
            }
            int bx = BlockPos.getX(seed);
            int by = BlockPos.getY(seed);
            int bz = BlockPos.getZ(seed);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        ctx.tryAdd(bx + dx, by + dy, bz + dz);
                    }
                }
            }
        }
    }

    /**
     * Whether a block actually stands in the way of seeing something. Air never does; fully solid blocks
     * always do; everything else (glass, leaves, doors, fluids) only when the player asked for it.
     */
    private static boolean blocksView(BlockState state, BlockGetter level, BlockPos pos, boolean hideNonSolid) {
        if (state.isAir()) {
            return false;
        }
        if (state.canOcclude()) {
            return true;
        }
        if (!hideNonSolid) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return false;
        }
        return !state.getShape(level, pos).isEmpty();
    }

    /** Scratch state shared by one scan, so the hot paths do not re-read config or reallocate. */
    private static final class Ctx {
        Level level;
        Vec3 camera;
        LongOpenHashSet out;
        LongOpenHashSet protectedPositions;
        int floorCutoff;
        int minY;
        int maxY;
        double maxDistSq;
        int maxBlocks;
        boolean hideNonSolid;

        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        boolean hasBudget() {
            return out.size() < maxBlocks;
        }

        void tryAdd(int x, int y, int z) {
            if (y < floorCutoff || y < minY || y >= maxY) {
                return;
            }
            long key = BlockPos.asLong(x, y, z);
            if (out.contains(key) || protectedPositions.contains(key)) {
                return;
            }
            double dx = x + 0.5D - camera.x;
            double dy = y + 0.5D - camera.y;
            double dz = z + 0.5D - camera.z;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) {
                return;
            }
            cursor.set(x, y, z);
            if (!level.isLoaded(cursor)) {
                return;
            }
            if (!blocksView(level.getBlockState(cursor), level, cursor, hideNonSolid)) {
                return;
            }
            out.add(key);
        }
    }
}
