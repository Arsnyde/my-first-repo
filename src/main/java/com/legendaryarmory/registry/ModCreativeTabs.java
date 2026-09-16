package com.legendaryarmory.registry;

import com.legendaryarmory.LegendaryArmory;
import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LegendaryArmory.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LEGENDARY_ARMORY_TAB =
            CREATIVE_MODE_TABS.register("legendary_armory", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.legendary_armory"))
                    .icon(() -> new ItemStack(ModItems.piece(LegendarySet.RAYQUAZA, ArmorItem.Type.CHESTPLATE).get()))
                    .displayItems((params, output) ->
                            ModItems.displayOrder().forEach(holder -> output.accept(holder.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
