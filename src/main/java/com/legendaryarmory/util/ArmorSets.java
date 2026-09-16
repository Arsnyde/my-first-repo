package com.legendaryarmory.util;

import com.legendaryarmory.item.LegendaryArmorItem;
import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Helpers for asking "what legendary armour is this entity wearing?". */
public final class ArmorSets {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorSets() {
    }

    /**
     * @return the set worn in every one of the four armour slots, or {@code null} if the entity is
     *         wearing a mixed loadout or is missing a piece.
     */
    @Nullable
    public static LegendarySet wornSet(LivingEntity entity) {
        LegendarySet found = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            LegendarySet set = setOf(entity.getItemBySlot(slot));
            if (set == null) {
                return null;
            }
            if (found == null) {
                found = set;
            } else if (found != set) {
                return null;
            }
        }
        return found;
    }

    /** How many pieces of the given set the entity is wearing, 0-4. */
    public static int piecesWorn(LivingEntity entity, LegendarySet set) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (setOf(entity.getItemBySlot(slot)) == set) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWearingFullSet(LivingEntity entity, LegendarySet set) {
        return wornSet(entity) == set;
    }

    @Nullable
    public static LegendarySet setOf(ItemStack stack) {
        return stack.getItem() instanceof LegendaryArmorItem armor ? armor.getSet() : null;
    }
}
