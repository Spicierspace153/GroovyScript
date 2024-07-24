package com.cleanroommc.groovyscript.core;

import com.cleanroommc.groovyscript.GroovyScript;
import com.cleanroommc.groovyscript.sandbox.MixinSandbox;
import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("GroovyScript-Core")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
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
        MixinSandbox.compileMixinsSafe();
        Mixins.addConfiguration("mixin.groovyscript.custom.json");
    }

    @Override
    public String getAccessTransformerClass() {
        return "com.cleanroommc.groovyscript.core.GroovyScriptTransformer";
    }

    @Override
    public List<String> getMixinConfigs() {
        return ImmutableList.of("mixin.groovyscript.json");
    }
}
