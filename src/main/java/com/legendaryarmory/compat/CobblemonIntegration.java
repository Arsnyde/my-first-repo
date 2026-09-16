package com.legendaryarmory.compat;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.legendaryarmory.LegendaryArmory;
import kotlin.jvm.functions.Function1;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Everything that talks to Cobblemon directly.
 *
 * <p>Kept in one class so the surface area against a mod that moves quickly between versions stays
 * small and obvious.</p>
 */
public final class CobblemonIntegration {

    private static boolean installed;

    private CobblemonIntegration() {
    }

    /**
     * Registers the armour spawn influence with Cobblemon's player spawner factory.
     *
     * <p>Cobblemon builds a fresh {@code PlayerSpawner} per player and runs every registered builder
     * against it, so adding a builder here is enough to cover current and future players.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void install() {
        if (installed) {
            return;
        }
        try {
            // Raw List avoids a capture conversion problem: Kotlin's (ServerPlayer) -> SpawningInfluence?
            // is seen from Java as Function1<? super ServerPlayer, ? extends SpawningInfluence>.
            List builders = PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders();
            Function1<ServerPlayer, Object> builder = LegendaryArmorSpawnInfluence::new;
            builders.add(builder);
            installed = true;
            LegendaryArmory.LOGGER.info("Registered the Legendary Armory spawn influence with Cobblemon.");
        } catch (Throwable t) {
            LegendaryArmory.LOGGER.error(
                    "Could not hook into Cobblemon's spawner; armour spawn boosts will be inactive.", t);
        }
    }

    /**
     * Tops up the HP of every non-fainted Pokemon in the player's party.
     *
     * @param amount HP restored per Pokemon
     */
    public static void healParty(ServerPlayer player, int amount) {
        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (Pokemon pokemon : party) {
                if (pokemon.isFainted()) {
                    continue;
                }
                int max = pokemon.getMaxHealth();
                int current = pokemon.getCurrentHealth();
                if (current < max) {
                    pokemon.setCurrentHealth(Math.min(max, current + amount));
                }
            }
        } catch (Throwable t) {
            LegendaryArmory.LOGGER.debug("Party healing skipped: {}", t.toString());
        }
    }
}
