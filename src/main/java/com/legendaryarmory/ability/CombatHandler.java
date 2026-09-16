package com.legendaryarmory.ability;

import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Damage-time half of the set bonuses: immunities, mitigation and retaliation. */
public final class CombatHandler {

    private CombatHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        LegendarySet set = SetBonusHandler.activeSet(player);
        if (set == null) {
            return;
        }

        DamageSource source = event.getSource();

        if (isImmune(set, source)) {
            event.setCanceled(true);
            return;
        }

        float multiplier = mitigation(set, source);
        if (multiplier < 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }

        retaliate(set, player, source);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        LegendarySet set = SetBonusHandler.activeSet(player);
        if (set == LegendarySet.RAYQUAZA || set == LegendarySet.PALKIA || set == LegendarySet.ARCEUS) {
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------ immunities

    private static boolean isImmune(LegendarySet set, DamageSource source) {
        return switch (set) {
            // Air Lock - weather and altitude cannot touch you.
            case RAYQUAZA -> is(source, DamageTypes.LIGHTNING_BOLT)
                    || is(source, DamageTypes.FREEZE)
                    || is(source, DamageTypes.FALL)
                    || is(source, DamageTypes.FLY_INTO_WALL);
            // Drought - the set is already fire resistant, this covers the rest of the heat sources.
            case GROUDON -> source.is(DamageTypeTags.IS_FIRE)
                    || is(source, DamageTypes.HOT_FLOOR)
                    || is(source, DamageTypes.LAVA);
            // Drizzle - you cannot drown, and freezing water is just water.
            case KYOGRE -> is(source, DamageTypes.DROWN) || is(source, DamageTypes.FREEZE);
            // Spacial Rend - space folds instead of hurting you.
            case PALKIA -> is(source, DamageTypes.FALL) || is(source, DamageTypes.ENDER_PEARL);
            // Shadow Force - a ghost cannot be withered.
            case GIRATINA -> is(source, DamageTypes.WITHER) || is(source, DamageTypes.MAGIC);
            // Prismatic Laser - light burns clean.
            case NECROZMA -> source.is(DamageTypeTags.IS_FIRE) || is(source, DamageTypes.MAGIC);
            // Multitype - the plate answers for all of it.
            case ARCEUS -> is(source, DamageTypes.WITHER)
                    || is(source, DamageTypes.MAGIC)
                    || is(source, DamageTypes.STARVE)
                    || is(source, DamageTypes.FALL)
                    || source.is(DamageTypeTags.IS_FIRE);
            default -> false;
        };
    }

    // ------------------------------------------------------------ mitigation

    private static float mitigation(LegendarySet set, DamageSource source) {
        if (set == LegendarySet.DIALGA && source.is(DamageTypeTags.IS_PROJECTILE)) {
            // Roar of Time - projectiles arrive slowed.
            return 0.5F;
        }
        if (set == LegendarySet.GROUDON && source.is(DamageTypeTags.IS_EXPLOSION)) {
            return 0.5F;
        }
        if (set == LegendarySet.KYOGRE && is(source, DamageTypes.ON_FIRE)) {
            return 0.25F;
        }
        return 1.0F;
    }

    // ----------------------------------------------------------- retaliation

    private static void retaliate(LegendarySet set, Player player, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player) {
            return;
        }
        switch (set) {
            case DIALGA -> attacker.addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
            case GIRATINA -> {
                attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
                attacker.hurt(player.damageSources().indirectMagic(player, player), 2.0F);
            }
            case NECROZMA -> attacker.addEffect(
                    new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
            case RAYQUAZA -> attacker.addEffect(
                    new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, true, true));
            default -> {
            }
        }
    }

    private static boolean is(DamageSource source, ResourceKey<DamageType> type) {
        return source.is(type);
    }
}
