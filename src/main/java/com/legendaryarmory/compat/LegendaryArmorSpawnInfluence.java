package com.legendaryarmory.compat;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.legendaryarmory.Config;
import com.legendaryarmory.sets.LegendarySet;
import com.legendaryarmory.util.ArmorSets;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A per-player {@link SpawningInfluence} that multiplies the spawn weight of the Pokemon associated
 * with whichever full legendary armour set the player is wearing.
 *
 * <p>One instance is attached to each {@code PlayerSpawner} when the player joins (see
 * {@link CobblemonIntegration}); it then reads the player's live equipment every time the spawner
 * weighs a candidate, so swapping armour takes effect immediately.</p>
 *
 * <p>Every method of the Kotlin {@code SpawningInfluence} interface is implemented explicitly rather
 * than inherited as a default, because Kotlin interface defaults are not guaranteed to be visible as
 * Java default methods.</p>
 */
public class LegendaryArmorSpawnInfluence implements SpawningInfluence {

    private static final String LEGENDARY_LABEL = "legendary";
    private static final String MYTHICAL_LABEL = "mythical";

    private final UUID playerId;
    private final MinecraftServer server;

    public LegendaryArmorSpawnInfluence(ServerPlayer player) {
        this.playerId = player.getUUID();
        this.server = player.getServer();
    }

    // ----------------------------------------------------------- the actual bonus

    @Override
    public float affectWeight(SpawnDetail detail, SpawnablePosition spawnablePosition, float weight) {
        if (weight <= 0.0F || !Config.flag(Config.ENABLE_SPAWN_BOOSTS, true)) {
            return weight;
        }
        if (!(detail instanceof PokemonSpawnDetail pokemonDetail)) {
            return weight;
        }
        LegendarySet set = wornSet();
        if (set == null) {
            return weight;
        }

        FormData form = resolveForm(pokemonDetail);
        if (form == null) {
            return weight;
        }

        float multiplier = 1.0F;

        String speciesPath = speciesPath(pokemonDetail.getPokemon());
        if (speciesPath != null && speciesPath.equals(set.signatureSpecies())) {
            multiplier = Math.max(multiplier, set.signatureMultiplier());
        }

        if (!set.boostedTypes().isEmpty()) {
            for (ElementalType type : form.getTypes()) {
                if (set.boostedTypes().contains(type.showdownId())) {
                    multiplier = Math.max(multiplier, set.spawnMultiplier());
                    break;
                }
            }
        }

        Set<String> labels = form.getLabels();
        if (!set.boostedLabels().isEmpty()) {
            for (String label : set.boostedLabels()) {
                if (labels.contains(label)) {
                    multiplier = Math.max(multiplier, set.spawnMultiplier());
                    break;
                }
            }
        }

        // Any complete set gives a smaller across-the-board nudge to legendaries and mythicals.
        if (labels.contains(LEGENDARY_LABEL) || labels.contains(MYTHICAL_LABEL)) {
            multiplier *= (float) Config.number(Config.GLOBAL_LEGENDARY_BOOST, 1.25D);
        }

        if (multiplier <= 1.0F) {
            return weight;
        }

        // The config scale interpolates between "no bonus" (0) and the configured multiplier (1).
        float scale = (float) Config.number(Config.SPAWN_BOOST_SCALE, 1.0D);
        return weight * (1.0F + (multiplier - 1.0F) * scale);
    }

    @Nullable
    private LegendarySet wornSet() {
        if (server == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player == null ? null : ArmorSets.wornSet(player);
    }

    @Nullable
    private static String speciesPath(PokemonProperties properties) {
        String species = properties.getSpecies();
        if (species == null || species.isEmpty()) {
            return null;
        }
        String lower = species.toLowerCase(Locale.ROOT);
        int colon = lower.indexOf(':');
        return colon >= 0 ? lower.substring(colon + 1) : lower;
    }

    @Nullable
    private static FormData resolveForm(PokemonSpawnDetail detail) {
        PokemonProperties properties = detail.getPokemon();
        String path = speciesPath(properties);
        if (path == null || "random".equals(path)) {
            return null;
        }
        Species species = PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath("cobblemon", path));
        if (species == null) {
            return null;
        }
        Set<String> aspects = properties.getAspects();
        return species.getForm(aspects == null ? Set.of() : aspects);
    }

    // -------------------------------------------------- unused parts of the interface

    @Override
    public boolean isExpired() {
        // Lives as long as the player's spawner does.
        return false;
    }

    @Override
    public void affectSpawnablePosition(SpawnablePosition spawnablePosition) {
    }

    @Override
    public boolean affectSpawnable(SpawnDetail detail, SpawnablePosition spawnablePosition) {
        return true;
    }

    @Override
    public void affectAction(SpawnAction<?> action) {
    }

    @Override
    public void affectSpawn(SpawnAction<?> action, Entity entity) {
    }

    @Override
    public void affectBucketWeights(Map<String, Float> bucketWeights) {
    }

    @Override
    public void normalizeBucketWeights(Map<String, Float> bucketWeights) {
        float sum = 0.0F;
        for (Float value : bucketWeights.values()) {
            if (value != null) {
                sum += value;
            }
        }
        if (sum <= 0.0F) {
            return;
        }
        float to100 = 100.0F / sum;
        for (Map.Entry<String, Float> entry : bucketWeights.entrySet()) {
            Float value = entry.getValue();
            if (value != null) {
                entry.setValue(value * to100);
            }
        }
    }

    @Override
    public boolean isAllowedPosition(ServerLevel world, BlockPos pos, SpawnablePositionCalculator<?, ?> calculator) {
        return true;
    }

    @Nullable
    @Override
    public List<SpawnDetail> injectSpawns(String bucket, SpawnablePosition spawnablePosition) {
        return null;
    }
}
