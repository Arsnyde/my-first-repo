package com.legendaryarmory.ability;

import com.legendaryarmory.Config;
import com.legendaryarmory.LegendaryArmory;
import com.legendaryarmory.compat.CobblemonIntegration;
import com.legendaryarmory.sets.LegendarySet;
import com.legendaryarmory.util.ArmorSets;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;

/**
 * Applies the "wearing all four pieces" bonus of each {@link LegendarySet}.
 *
 * <p>Effects are refreshed on a one second cadence with a five second duration, so they stay
 * seamless while the set is worn and lapse shortly after a piece comes off.</p>
 */
public final class SetBonusHandler {

    private static final int REFRESH_INTERVAL = 20;
    private static final int EFFECT_DURATION = 100;
    private static final int WEATHER_INTERVAL = 100;
    private static final int PARTY_HEAL_INTERVAL = 100;

    private static final ResourceLocation PALKIA_BLOCK_REACH = LegendaryArmory.id("palkia_block_reach");
    private static final ResourceLocation PALKIA_ENTITY_REACH = LegendaryArmory.id("palkia_entity_reach");
    private static final ResourceLocation ARCEUS_HEALTH = LegendaryArmory.id("arceus_health");

    private SetBonusHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player generic = event.getEntity();
        if (generic.level().isClientSide() || !(generic instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }

        LegendarySet set = ArmorSets.wornSet(player);
        boolean enabled = Config.flag(Config.ENABLE_SET_BONUSES, true);
        LegendarySet active = enabled ? set : null;

        // Attribute driven bonuses have to be torn down explicitly when the set comes off.
        boolean palkia = active == LegendarySet.PALKIA;
        setAttribute(player, Attributes.BLOCK_INTERACTION_RANGE, PALKIA_BLOCK_REACH, 3.0D, palkia);
        setAttribute(player, Attributes.ENTITY_INTERACTION_RANGE, PALKIA_ENTITY_REACH, 2.0D, palkia);
        setAttribute(player, Attributes.MAX_HEALTH, ARCEUS_HEALTH, 8.0D, active == LegendarySet.ARCEUS);

        if (active == null) {
            return;
        }

        switch (active) {
            case RAYQUAZA -> rayquaza(player);
            case GROUDON -> groudon(player);
            case KYOGRE -> kyogre(player);
            case DIALGA -> dialga(player);
            case PALKIA -> palkia(player);
            case GIRATINA -> giratina(player);
            case NECROZMA -> necrozma(player);
            case ARCEUS -> arceus(player);
        }
    }

    // ------------------------------------------------------------------ sets

    /** Delta Stream: the air itself gets out of your way. */
    private static void rayquaza(ServerPlayer player) {
        effect(player, MobEffects.SLOW_FALLING, 0);
        effect(player, MobEffects.MOVEMENT_SPEED, 0);
        effect(player, MobEffects.JUMP, 1);
        player.setTicksFrozen(0);
        // A gentle updraft while holding a glide, so high altitude travel actually feels like flight.
        if (!player.onGround() && player.getDeltaMovement().y < 0.0D && player.isSprinting()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 1.08D, Math.max(motion.y, -0.08D), motion.z * 1.08D);
            player.hurtMarked = true;
        }
    }

    /** Drought: heat, stone and an endless appetite for digging. */
    private static void groudon(ServerPlayer player) {
        effect(player, MobEffects.FIRE_RESISTANCE, 0);
        effect(player, MobEffects.DIG_SPEED, 1);
        effect(player, MobEffects.DAMAGE_BOOST, 0);
        player.setRemainingFireTicks(0);
        if (player.isInLava()) {
            // Magma affinity - you swim in lava instead of sinking through it.
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.12D), motion.z);
            player.hurtMarked = true;
        }
        setWeather(player, false);
    }

    /** Drizzle: the ocean treats you as one of its own. */
    private static void kyogre(ServerPlayer player) {
        effect(player, MobEffects.WATER_BREATHING, 0);
        effect(player, MobEffects.CONDUIT_POWER, 0);
        effect(player, MobEffects.DOLPHINS_GRACE, 0);
        player.setAirSupply(player.getMaxAirSupply());
        setWeather(player, true);
    }

    /** Roar of Time: everything around you runs a little slower than you do. */
    private static void dialga(ServerPlayer player) {
        effect(player, MobEffects.DIG_SPEED, 2);
        effect(player, MobEffects.DAMAGE_RESISTANCE, 0);
        effect(player, MobEffects.MOVEMENT_SPEED, 0);
    }

    /** Spacial Rend: distance is negotiable. Reach is handled by the attribute modifiers above. */
    private static void palkia(ServerPlayer player) {
        effect(player, MobEffects.MOVEMENT_SPEED, 1);
        effect(player, MobEffects.JUMP, 0);
    }

    /** Shadow Force: the Distortion World bleeds into yours. */
    private static void giratina(ServerPlayer player) {
        effect(player, MobEffects.NIGHT_VISION, 0);
        if (player.isCrouching()) {
            effect(player, MobEffects.INVISIBILITY, 0);
        }
    }

    /** Prismatic Laser: you run on light. */
    private static void necrozma(ServerPlayer player) {
        effect(player, MobEffects.NIGHT_VISION, 0);
        Level level = player.level();
        boolean inSunlight = level.isDay() && level.canSeeSky(player.blockPosition());
        if (inSunlight) {
            effect(player, MobEffects.DAMAGE_BOOST, 0);
            effect(player, MobEffects.REGENERATION, 0);
        }
    }

    /** Multitype: a bit of everything, plus a hand for your party. */
    private static void arceus(ServerPlayer player) {
        effect(player, MobEffects.REGENERATION, 0);
        effect(player, MobEffects.DAMAGE_RESISTANCE, 0);
        effect(player, MobEffects.SATURATION, 0);
        if (player.tickCount % PARTY_HEAL_INTERVAL == 0 && Config.flag(Config.ENABLE_PARTY_HEALING, true)) {
            CobblemonIntegration.healParty(player, 2);
        }
    }

    // --------------------------------------------------------------- helpers

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier && current.getDuration() > EFFECT_DURATION) {
            // Don't downgrade a stronger potion the player drank themselves.
            return;
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, true, false, true));
    }

    private static void setAttribute(ServerPlayer player, Holder<Attribute> attribute,
                                     ResourceLocation id, double amount, boolean shouldApply) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        boolean present = instance.getModifier(id) != null;
        if (shouldApply && !present) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        } else if (!shouldApply && present) {
            instance.removeModifier(id);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void setWeather(ServerPlayer player, boolean raining) {
        if (!Config.flag(Config.ENABLE_WEATHER_CONTROL, true)) {
            return;
        }
        if (player.tickCount % WEATHER_INTERVAL != 0) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel) || !level.dimensionType().natural()) {
            return;
        }
        if (serverLevel.isRaining() == raining && !serverLevel.isThundering()) {
            return;
        }
        if (raining) {
            serverLevel.setWeatherParameters(0, 12000, true, false);
        } else {
            serverLevel.setWeatherParameters(12000, 0, false, false);
        }
    }

    @Nullable
    public static LegendarySet activeSet(Player player) {
        return Config.flag(Config.ENABLE_SET_BONUSES, true) ? ArmorSets.wornSet(player) : null;
    }
}
