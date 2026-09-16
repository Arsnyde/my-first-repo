package com.legendaryarmory;

import com.legendaryarmory.ability.CombatHandler;
import com.legendaryarmory.ability.SetBonusHandler;
import com.legendaryarmory.compat.CobblemonIntegration;
import com.legendaryarmory.registry.ModArmorMaterials;
import com.legendaryarmory.registry.ModCreativeTabs;
import com.legendaryarmory.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LegendaryArmory.MOD_ID)
public class LegendaryArmory {

    public static final String MOD_ID = "legendary_armory";
    public static final Logger LOGGER = LoggerFactory.getLogger("Legendary Armory");

    public LegendaryArmory(IEventBus modBus, ModContainer container) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(SetBonusHandler.class);
        NeoForge.EVENT_BUS.register(CombatHandler.class);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Cobblemon's PlayerSpawnerFactory is a plain object, not a registry, so the hook has to be
        // installed on the main thread after every mod has constructed.
        event.enqueueWork(CobblemonIntegration::install);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
