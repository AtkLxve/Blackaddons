package org.blackum.blackaddons.common.module;

import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ModuleManager {
    private static final String BASE_PACKAGE = "org.blackum.blackaddons";
    private static final String BASE_PATH = BASE_PACKAGE.replace('.', '/');
    private static final String MIXIN_PACKAGE = BASE_PACKAGE + ".mixin.";
    private static final String MIXIN_PATH = BASE_PATH + "/mixin/";

    private ModuleManager() {
    }

    public static void registerAutoModules() {
        List<Class<?>> modules = findModuleClasses();
        modules.sort(Comparator
                .comparingInt((Class<?> type) -> type.getAnnotation(AutoModule.class).order())
                .thenComparing(Class::getName));

        for (Class<?> module : modules) {
            invokeRegister(module);
        }

        Blackaddons.LOGGER.info("Registered {} modules", modules.size());
        logModulesInDev(modules);
    }

    private static List<Class<?>> findModuleClasses() {
        List<Class<?>> modules = new ArrayList<>();
        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(BASE_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    scanDirectory(modules, Paths.get(resource.toURI()), BASE_PACKAGE);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanJar(modules, resource);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to scan modules", e);
        }
        return modules;
    }

    private static void scanDirectory(List<Class<?>> modules, Path root, String packageName) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (var paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .map(root::relativize)
                    .map(path -> toClassName(packageName, path))
                    .forEach(className -> addIfAutoModule(modules, className));
        }
    }

    private static void scanJar(List<Class<?>> modules, URL resource) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(BASE_PATH) && !name.startsWith(MIXIN_PATH) && name.endsWith(".class") && !name.contains("$")) {
                    addIfAutoModule(modules, name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                }
            }
        }
    }

    private static String toClassName(String packageName, Path classFile) {
        String relativeName = classFile.toString()
                .replace('\\', '.')
                .replace('/', '.');
        return packageName + "." + relativeName.substring(0, relativeName.length() - ".class".length());
    }

    private static void addIfAutoModule(List<Class<?>> modules, String className) {
        if (className.startsWith(MIXIN_PACKAGE)) {
            return;
        }

        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (type.isAnnotationPresent(AutoModule.class)) {
                modules.add(type);
            }
        } catch (ClassNotFoundException | LinkageError e) {
            Blackaddons.LOGGER.warn("Skipping module candidate {}", className, e);
        }
    }

    private static void invokeRegister(Class<?> module) {
        try {
            Method register = module.getDeclaredMethod("register");
            if (!Modifier.isStatic(register.getModifiers()) || register.getParameterCount() != 0) {
                throw new IllegalStateException(module.getName() + " must declare static register()");
            }

            register.setAccessible(true);
            register.invoke(null);
            Blackaddons.LOGGER.debug("Registered module {}", module.getName());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(module.getName() + " is marked @AutoModule but has no register()", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access register() on " + module.getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to register module " + module.getName(), e.getCause());
        }
    }

    private static void logModulesInDev(List<Class<?>> modules) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        Blackaddons.LOGGER.info("Module order:");
        for (Class<?> module : modules) {
            AutoModule annotation = module.getAnnotation(AutoModule.class);
            Blackaddons.LOGGER.info("  {} -> {}", annotation.order(), module.getName());
        }
    }
}
