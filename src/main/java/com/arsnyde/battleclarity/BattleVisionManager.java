package com.arsnyde.battleclarity;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Drives the effect: watches Cobblemon's client-side battle state, works out the hidden set and asks the
 * chunk renderer to rebuild only when that set actually changed.
 */
public final class BattleVisionManager {

    /** How far around the player we look for other battle participants. */
    private static final double SEARCH_RADIUS = 48.0D;

    /** Re-scan early when the camera or the battlers have moved at least this far. */
    private static final double MOVE_EPSILON = 0.4D;

    /** Re-scan at least this often even when nothing moved, so world edits are picked up. */
    private static final int MAX_STALE_TICKS = 20;

    /** pos -> the tick at which it may stop being hidden. Gives the opening hysteresis. */
    private static final Long2IntOpenHashMap expiry = new Long2IntOpenHashMap();

    private static int tick;
    private static int lastScanTick = Integer.MIN_VALUE;
    private static Vec3 lastCamera = Vec3.ZERO;
    private static Vec3 lastCentroid = Vec3.ZERO;

    private BattleVisionManager() {
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (level == null || mc.player == null) {
            forget();
            return;
        }

        if (!ClarityConfig.ENABLED.get()) {
            restore(mc, level);
            return;
        }

        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) {
            // Battle over - put everything back straight away, ignoring the sticky timer.
            restore(mc, level);
            return;
        }

        tick++;

        List<OcclusionScanner.Battler> battlers = collectBattlers(mc, level, battle);
        if (battlers.isEmpty()) {
            restore(mc, level);
            return;
        }

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 centroid = centroidOf(battlers);
        if (!shouldScan(camera, centroid)) {
            return;
        }
        lastScanTick = tick;
        lastCamera = camera;
        lastCentroid = centroid;

        LongOpenHashSet fresh = OcclusionScanner.scan(level, camera, battlers);
        applySticky(fresh);
        publish(mc, level, keysOf(expiry));
    }

    /** Called when the player leaves the world; drops state without touching the renderer. */
    public static void forget() {
        expiry.clear();
        HiddenBlocks.clearSilently();
        lastScanTick = Integer.MIN_VALUE;
        tick = 0;
    }

    // ---------------------------------------------------------------------------------------------

    private static boolean shouldScan(Vec3 camera, Vec3 centroid) {
        int sinceLast = tick - lastScanTick;
        if (sinceLast < ClarityConfig.RECOMPUTE_INTERVAL.get()) {
            return false;
        }
        if (sinceLast >= MAX_STALE_TICKS) {
            return true;
        }
        // Note: only translation matters. Rays are aimed at the battlers rather than along the view
        // direction, so simply looking around never triggers a rebuild.
        return camera.distanceToSqr(lastCamera) > MOVE_EPSILON * MOVE_EPSILON
                || centroid.distanceToSqr(lastCentroid) > MOVE_EPSILON * MOVE_EPSILON;
    }

    /** Refreshes the timer for freshly-occluding blocks and drops the ones that have timed out. */
    private static void applySticky(LongOpenHashSet fresh) {
        int stickyTicks = ClarityConfig.STICKY_TICKS.get();
        LongIterator it = fresh.iterator();
        while (it.hasNext()) {
            expiry.put(it.nextLong(), tick + stickyTicks);
        }
        ObjectIterator<Long2IntMap.Entry> entries = expiry.long2IntEntrySet().fastIterator();
        while (entries.hasNext()) {
            if (entries.next().getIntValue() < tick) {
                entries.remove();
            }
        }
    }

    private static LongOpenHashSet keysOf(Long2IntOpenHashMap map) {
        return new LongOpenHashSet(map.keySet());
    }

    /**
     * Publishes a new hidden set and rebuilds only the chunk sections whose contents changed. When the
     * set is identical to the one already on screen this does nothing at all.
     */
    private static void publish(Minecraft mc, ClientLevel level, LongOpenHashSet next) {
        LongSet previous = HiddenBlocks.current();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean changed = false;

        LongIterator it = next.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            if (!previous.contains(key)) {
                changed = true;
                minX = Math.min(minX, BlockPos.getX(key));
                minY = Math.min(minY, BlockPos.getY(key));
                minZ = Math.min(minZ, BlockPos.getZ(key));
                maxX = Math.max(maxX, BlockPos.getX(key));
                maxY = Math.max(maxY, BlockPos.getY(key));
                maxZ = Math.max(maxZ, BlockPos.getZ(key));
            }
        }
        it = previous.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            if (!next.contains(key)) {
                changed = true;
                minX = Math.min(minX, BlockPos.getX(key));
                minY = Math.min(minY, BlockPos.getY(key));
                minZ = Math.min(minZ, BlockPos.getZ(key));
                maxX = Math.max(maxX, BlockPos.getX(key));
                maxY = Math.max(maxY, BlockPos.getY(key));
                maxZ = Math.max(maxZ, BlockPos.getZ(key));
            }
        }

        if (!changed) {
            return;
        }

        HiddenBlocks.publish(next);
        // setBlocksDirty marks every chunk section overlapping the range for a rebuild, which is what
        // actually makes the blocks vanish or come back.
        mc.levelRenderer.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Puts every hidden block back immediately. */
    private static void restore(Minecraft mc, ClientLevel level) {
        if (expiry.isEmpty() && !HiddenBlocks.isActive()) {
            return;
        }
        expiry.clear();
        publish(mc, level, new LongOpenHashSet());
        lastScanTick = Integer.MIN_VALUE;
    }

    // ---------------------------------------------------------------------------------------------

    private static List<OcclusionScanner.Battler> collectBattlers(Minecraft mc, ClientLevel level, ClientBattle battle) {
        UUID battleId = battle.getBattleId();
        AABB search = mc.player.getBoundingBox().inflate(SEARCH_RADIUS);

        List<OcclusionScanner.Battler> battlers = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, search, e -> isBattler(e, battle, battleId))) {
            battlers.add(toBattler(entity));
        }
        return battlers;
    }

    private static boolean isBattler(Entity entity, ClientBattle battle, UUID battleId) {
        if (entity instanceof PokemonEntity pokemon) {
            return battleId.equals(pokemon.getBattleId());
        }
        if (entity instanceof Player player) {
            return battle.getParticipatingActor(player.getUUID()) != null;
        }
        return false;
    }

    /**
     * Turns an entity into a scan target. The model box is deliberately larger than the hitbox because
     * Cobblemon models routinely overhang it - wings, tails and horns all render outside the collision
     * box, and those parts poking into a wall are exactly what the player is complaining about.
     */
    private static OcclusionScanner.Battler toBattler(Entity entity) {
        AABB hitbox = entity.getBoundingBox();
        AABB modelBox = hitbox.minmax(entity.getBoundingBoxForCulling());
        double span = Math.max(Math.max(modelBox.getXsize(), modelBox.getZsize()), modelBox.getYsize());
        double pad = ClarityConfig.MODEL_PADDING.get() + ClarityConfig.MODEL_PADDING_SCALE.get() * span;
        return new OcclusionScanner.Battler(hitbox, modelBox.inflate(pad));
    }

    private static Vec3 centroidOf(List<OcclusionScanner.Battler> battlers) {
        double x = 0.0D, y = 0.0D, z = 0.0D;
        for (OcclusionScanner.Battler battler : battlers) {
            Vec3 center = battler.modelBox().getCenter();
            x += center.x;
            y += center.y;
            z += center.z;
        }
        int n = battlers.size();
        return new Vec3(x / n, y / n, z / n);
    }
}
