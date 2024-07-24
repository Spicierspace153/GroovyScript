package com.cleanroommc.groovyscript.sandbox;

import com.cleanroommc.groovyscript.GroovyScript;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.ClassWriter;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

public class MixinSandbox {

    // make sure to run groovy mixins first, then run mod mixins into mc and then run these

    private static final ClassGenerator generator = new ClassGenerator();
    private static final Class<?>[] mixinImportClasses = {Mixin.class, Inject.class, At.class};

    public static void compileMixinsSafe() {
        try {
            compileMixins();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void compileMixins() throws IOException, ScriptException, ResourceException {
        File root = GroovyScript.getScriptFile();
        URL rootUrl = root.toURI().toURL();
        GroovyScriptEngine engine = new GroovyScriptEngine(new URL[]{rootUrl});
        CompilerConfiguration config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        config.setSourceEncoding("UTF-8");
        config.addCompilationCustomizers(new CallbackInjector());
        config.addCompilationCustomizers(
                new ImportCustomizer().addImports(Arrays.stream(mixinImportClasses).map(Class::getName).toArray(String[]::new)));
        engine.setConfig(config);
        for (File mixinScript : GroovyScript.getRunConfig().getMixinFiles(GroovyScript.getScriptFile())) {
            String path = root.toPath().relativize(mixinScript.toPath()).toString();
            engine.loadScriptByName(path);
        }
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
