package com.arsnyde.battleclarity.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Applies the Sodium mixins only when Sodium is installed, so the mod loads cleanly either way.
 *
 * <p>Presence is decided by asking Mixin's own bytecode provider for the class rather than by mod id.
 * That reads the class without loading or initialising it, and it keys off the thing the mixin actually
 * needs to exist.
 */
public class SodiumMixinPlugin implements IMixinConfigPlugin {

    private static final String SODIUM_LEVEL_SLICE = "net.caffeinemc.mods.sodium.client.world.LevelSlice";

    private boolean sodiumPresent;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodiumPresent = classExists(SODIUM_LEVEL_SLICE);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.sodiumPresent;
    }

    private static boolean classExists(String className) {
        try {
            return MixinService.getService().getBytecodeProvider().getClassNode(className) != null;
        } catch (Throwable ignored) {
            // ClassNotFoundException when Sodium is absent, which is the normal no-Sodium case.
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
