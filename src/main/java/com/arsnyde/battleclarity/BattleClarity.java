package com.arsnyde.battleclarity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only mod: while a Cobblemon battle is running, the blocks standing between the camera and the
 * Pokemon are skipped by the chunk renderer. Nothing is sent to the server and no blocks are changed,
 * so this works on any server running Cobblemon and cannot desync or grief a world.
 */
@Mod(value = BattleClarity.MOD_ID, dist = Dist.CLIENT)
public class BattleClarity {

    public static final String MOD_ID = "battleclarity";

    public BattleClarity(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClarityConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(BattleClarity::onClientTick);
        NeoForge.EVENT_BUS.addListener(BattleClarity::onLoggingOut);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        BattleVisionManager.onClientTick();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BattleVisionManager.forget();
    }
}
