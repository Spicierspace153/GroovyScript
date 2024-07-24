package com.cleanroommc.groovyscript.core;

import com.cleanroommc.groovyscript.GroovyScript;
import com.cleanroommc.groovyscript.sandbox.MixinSandbox;
import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("GroovyScript-Core")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE + 100)
public class GroovyScriptCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static File source;

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "com.cleanroommc.groovyscript.sandbox.ScriptModContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        source = (File) data.getOrDefault("coremodLocation", null);
        // setup root path etc,
        GroovyScript.initializeRunConfig((File) FMLInjectionData.data()[6]);
        // called later than mixin booter, we can now try to compile groovy mixins
        // the groovy mixins need to be compiled to bytes manually first
        Collection<String> groovyMixins = MixinSandbox.compileMixinsSafe();
        String cfgName = "mixin.groovyscript.custom.json";
        Mixins.addConfiguration(cfgName);
        // obtain the just created config
        IMixinConfig config = Config.create(cfgName, MixinEnvironment.getDefaultEnvironment()).getConfig();
        List<String> mixinClasses;
        try {
            Class<?> mixinConfigClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
            Field field = mixinConfigClass.getDeclaredField("mixinClasses");
            field.setAccessible(true);
            mixinClasses = (List<String>) field.get(config);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        mixinClasses.addAll(groovyMixins);
    }

    @Override
    public String getAccessTransformerClass() {
        return "com.cleanroommc.groovyscript.core.GroovyScriptTransformer";
    }

    @Override
    public List<String> getMixinConfigs() {
        return ImmutableList.of("mixin.groovyscript.groovy.json", "mixin.groovyscript.json");
    }
}
