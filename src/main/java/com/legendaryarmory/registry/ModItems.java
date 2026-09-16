package com.legendaryarmory.registry;

import com.legendaryarmory.LegendaryArmory;
import com.legendaryarmory.item.LegendaryArmorItem;
import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, LegendaryArmory.MOD_ID);

    /** Crafting core required by the Arceus set; keeps the capstone recipes short. */
    public static final DeferredHolder<Item, Item> DOMINION_CORE = ITEMS.register("dominion_core",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(16)));

    private static final Map<LegendarySet, Map<ArmorItem.Type, DeferredHolder<Item, LegendaryArmorItem>>> PIECES =
            new EnumMap<>(LegendarySet.class);

    /** The four wearable slots, in the order a set is assembled. */
    public static final List<ArmorItem.Type> WEARABLE_TYPES =
            List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);

    static {
        for (LegendarySet set : LegendarySet.values()) {
            Map<ArmorItem.Type, DeferredHolder<Item, LegendaryArmorItem>> bySlot = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : WEARABLE_TYPES) {
                bySlot.put(type, registerPiece(set, type));
            }
            PIECES.put(set, bySlot);
        }
    }

    private ModItems() {
    }

    private static DeferredHolder<Item, LegendaryArmorItem> registerPiece(LegendarySet set, ArmorItem.Type type) {
        return ITEMS.register(set.itemPath(type), () -> new LegendaryArmorItem(
                ModArmorMaterials.get(set),
                type,
                set,
                new Item.Properties()
                        .durability(type.getDurability(set.stats().durabilityBase()))
                        .rarity(Rarity.EPIC)
                        .fireResistant()));
    }

    public static DeferredHolder<Item, LegendaryArmorItem> piece(LegendarySet set, ArmorItem.Type type) {
        return PIECES.get(set).get(type);
    }

    /** Every item this mod registers, in creative-tab order. */
    public static List<DeferredHolder<Item, ? extends Item>> displayOrder() {
        List<DeferredHolder<Item, ? extends Item>> out = new ArrayList<>();
        out.add(DOMINION_CORE);
        for (LegendarySet set : LegendarySet.values()) {
            for (ArmorItem.Type type : WEARABLE_TYPES) {
                out.add(piece(set, type));
            }
        }
        return out;
    }
}
