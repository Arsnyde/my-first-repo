package com.legendaryarmory.item;

import com.legendaryarmory.sets.LegendarySet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An armour piece that belongs to a {@link LegendarySet}. */
public class LegendaryArmorItem extends ArmorItem {

    private final LegendarySet set;

    public LegendaryArmorItem(Holder<ArmorMaterial> material, Type type, LegendarySet set, Properties properties) {
        super(material, type, properties);
        this.set = set;
    }

    public LegendarySet getSet() {
        return set;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable(set.translationRoot() + ".title")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("legendary_armory.tooltip.full_set",
                        Component.translatable(set.translationRoot() + ".bonus")
                                .withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(set.translationRoot() + ".description")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(set.translationRoot() + ".spawns")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
