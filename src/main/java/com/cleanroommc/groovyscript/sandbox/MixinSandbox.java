package com.cleanroommc.groovyscript.sandbox;

import com.cleanroommc.groovyscript.GroovyScript;
import groovy.util.GroovyScriptEngine;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.ClassWriter;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class MixinSandbox {

    // make sure to run groovy mixins first, then run mod mixins into mc and then run these

    public static final Logger LOG = LogManager.getLogger("GroovyScript-MixinSandbox");
    private static final ClassGenerator generator = new ClassGenerator();
    private static final Class<?>[] mixinImportClasses = {Mixin.class, Inject.class, At.class, CallbackInfo.class,
                                                          CallbackInfoReturnable.class};
    private static final String MIXIN_PKG = "mixins.";

    public static Collection<String> compileMixinsSafe() {
        try {
            return compileMixins();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Collection<String> compileMixins() throws IOException {
        File root = GroovyScript.getScriptFile();
        URL rootUrl = root.toURI().toURL();
        GroovyScriptEngine engine = new GroovyScriptEngine(new URL[]{rootUrl});
        CompilerConfiguration config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        config.setSourceEncoding("UTF-8");
        config.addCompilationCustomizers(new CallbackInjector());
        config.addCompilationCustomizers(
                new ImportCustomizer().addImports(Arrays.stream(mixinImportClasses).map(Class::getName).toArray(String[]::new)));
        engine.setConfig(config);
        Collection<String> mixinClasses = new ArrayList<>();
        for (File mixinScript : GroovyScript.getRunConfig().getMixinFiles(GroovyScript.getScriptFile())) {
            String path = root.toPath().relativize(mixinScript.toPath()).toString();
            String className = toClassName(path);
            if (className == null) continue;
            if (!className.startsWith(MIXIN_PKG)) {
                LOG.error("Groovy mixins must be inside 'groovy/mixins/', but was in '{}'", path);
                continue;
            }
            try {
                engine.loadScriptByName(path);
                mixinClasses.add(className.substring(MIXIN_PKG.length()));
            } catch (Exception e) {
                LOG.error("Error loading mixin class '{}'", path);
                LOG.throwing(e);
            }
        }
        return mixinClasses;
    }

    private static String toClassName(String path) {
        int i = path.lastIndexOf('.');
        if (i < 0) {
            LOG.error("Path must end with '.groovy', but was '{}'", path);
            return null;
        }
        return path.substring(0, i).replace(File.separatorChar, '.');
    }

    private static class ClassGenerator implements CompilationUnit.ClassgenCallback {

        private static final Map<String, byte[]> resourceCache;

        static {
            Map<String, byte[]> resourceCache1;
            try {
                Field field = LaunchClassLoader.class.getDeclaredField("resourceCache");
                field.setAccessible(true);
                resourceCache1 = (Map<String, byte[]>) field.get(Launch.classLoader);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            resourceCache = resourceCache1;
        }

        @Override
        public void call(ClassVisitor classVisitor, ClassNode classNode) throws CompilationFailedException {
            // mixin will try to find the bytes in that map, so we will put them there
            resourceCache.put(classNode.getName(), ((ClassWriter) classVisitor).toByteArray());
        }
    }

    private static class CallbackInjector extends CompilationCustomizer {

        public CallbackInjector() {
            super(CompilePhase.CANONICALIZATION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        }

        @Override
        public void doPhaseOperation(final CompilationUnit unit) throws CompilationFailedException {
            // this will prevent the class from being created and ensures the bytes are in the right place when mixin need it
            unit.setClassgenCallback(generator);
        }
    }
}
