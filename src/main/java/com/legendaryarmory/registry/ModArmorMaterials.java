package com.legendaryarmory.registry;

import com.legendaryarmory.LegendaryArmory;
import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One registered {@link ArmorMaterial} per {@link LegendarySet}. */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, LegendaryArmory.MOD_ID);

    private static final Map<LegendarySet, Holder<ArmorMaterial>> MATERIALS = new EnumMap<>(LegendarySet.class);

    static {
        for (LegendarySet set : LegendarySet.values()) {
            MATERIALS.put(set, register(set));
        }
    }

    private ModArmorMaterials() {
    }

    private static Holder<ArmorMaterial> register(LegendarySet set) {
        LegendarySet.Stats stats = set.stats();
        return ARMOR_MATERIALS.register(set.id(), () -> {
            EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                defense.put(type, stats.defenseFor(type));
            }
            return new ArmorMaterial(
                    defense,
                    stats.enchantability(),
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.NETHERITE_INGOT),
                    // assets/legendary_armory/textures/models/armor/<set>_layer_1.png (and _layer_2)
                    List.of(new ArmorMaterial.Layer(LegendaryArmory.id(set.id()))),
                    stats.toughness(),
                    stats.knockback());
        });
    }

    public static Holder<ArmorMaterial> get(LegendarySet set) {
        return MATERIALS.get(set);
    }
}
