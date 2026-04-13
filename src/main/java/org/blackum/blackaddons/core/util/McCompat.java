package org.blackum.blackaddons.core.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

public final class McCompat {
    private static final String[] RESOURCE_CLASSES = {
            "net.minecraft.resources.Identifier",
            "net.minecraft.resources.ResourceLocation",
            "net.minecraft.class_2960"
    };
    private static final String[] UTIL_CLASSES = {
            "net.minecraft.util.Util",
            "net.minecraft.Util",
            "net.minecraft.class_156"
    };
    private static final String[] RENDER_TYPE_CLASSES = {
            "net.minecraft.client.renderer.rendertype.RenderTypes",
            "net.minecraft.client.renderer.rendertype.RenderType",
            "net.minecraft.client.renderer.RenderType",
            "net.minecraft.class_12249",
            "net.minecraft.class_1921"
    };

    private McCompat() {
    }

    public static Object tryParseResource(String value) {
        for (String name : RESOURCE_CLASSES) {
            Class<?> type = findClass(name);
            if (type == null) continue;
            try {
                Object parsed = invokeStaticBest(type, new String[] { "tryParse", "method_12829" }, value);
                if (parsed != null) return parsed;
            } catch (IllegalStateException ignored) {
            }
        }
        return null;
    }

    public static Object resource(String namespace, String path) {
        for (String name : RESOURCE_CLASSES) {
            Class<?> type = findClass(name);
            if (type == null) continue;
            try {
                return invokeStaticBest(type, new String[] { "fromNamespaceAndPath", "of", "method_60655", "method_12829" }, namespace, path);
            } catch (IllegalStateException ignored) {
            }
            try {
                java.lang.reflect.Constructor<?> constr = type.getConstructor(String.class, String.class);
                return constr.newInstance(namespace, path);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Unable to construct Minecraft resource identifier");
    }

    public static Object createRenderType(String name, Object renderSetup) {
        Class<?> concreteClass = renderSetup.getClass();
        java.util.List<Class<?>> paramTypes = new java.util.ArrayList<>();
        paramTypes.add(concreteClass);
        Class<?> superClass = concreteClass.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            paramTypes.add(superClass);
            superClass = superClass.getSuperclass();
        }
        for (Class<?> iface : concreteClass.getInterfaces()) {
            paramTypes.add(iface);
        }

        for (String className : RENDER_TYPE_CLASSES) {
            Class<?> type = findClass(className);
            if (type == null) continue;
            for (Class<?> paramType : paramTypes) {
                for (String methodName : new String[] { "create", "method_24045" }) {
                    try {
                        Method create = type.getDeclaredMethod(methodName, String.class, paramType);
                        create.setAccessible(true);
                        return create.invoke(null, name, renderSetup);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to create RenderType for renderSetup type: " + concreteClass.getName());
    }

    public static Object createTextRenderType(String name, Object pipeline, Object location) {
        Class<?> renderSetupClass = findClass("net.minecraft.client.renderer.rendertype.RenderSetup");
        if (renderSetupClass != null) {
            try {
                Object builder = invokeStaticBest(renderSetupClass, new String[] { "builder" }, pipeline);
                Object textured = invokeBest(builder, new String[] { "withTexture" }, "Sampler0", location);
                Object renderSetup = invokeBest(textured, new String[] { "createRenderSetup" });
                return createRenderType(name, renderSetup);
            } catch (IllegalStateException ignored) {
            }
        }

        Class<?> renderTypeClass = findClass("net.minecraft.client.renderer.RenderType");
        Class<?> compositeStateClass = findClass("net.minecraft.client.renderer.RenderType$CompositeState");
        Class<?> textureStateClass = findClass("net.minecraft.client.renderer.RenderStateShard$TextureStateShard");
        if (renderTypeClass == null || compositeStateClass == null || textureStateClass == null) {
            throw new IllegalStateException("Unable to resolve RenderType internals for text rendering");
        }

        try {
            Object compositeBuilder = invokeStaticBest(compositeStateClass, new String[] { "builder" });
            java.lang.reflect.Constructor<?> textureStateCtor = textureStateClass.getDeclaredConstructor(location.getClass(), boolean.class);
            textureStateCtor.setAccessible(true);
            Object textureState = textureStateCtor.newInstance(location, true);
            Object texturedBuilder = invokeBest(compositeBuilder, new String[] { "setTextureState" }, textureState);
            Object compositeState = invokeBest(texturedBuilder, new String[] { "createCompositeState" }, false);
            try {
                return invokeStaticBest(renderTypeClass, new String[] { "create", "method_24045" }, name, 1536, pipeline, compositeState);
            } catch (IllegalStateException ignored) {
                return invokeStaticBest(renderTypeClass, new String[] { "create", "method_24045" }, name, 1536, false, true, pipeline, compositeState);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create text RenderType", e);
        }
    }

    public static String dimensionId(ResourceKey<Level> key) {
        for (String methodName : new String[] { "identifier", "location", "method_29177" }) {
            try {
                Method method = key.getClass().getMethod(methodName);
                Object value = method.invoke(key);
                if (value != null) return value.toString();
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve dimension id");
    }

    public static void openUri(String url) {
        for (String name : UTIL_CLASSES) {
            Class<?> type = findClass(name);
            if (type == null) continue;
            try {
                Object platform = invokeStaticBest(type, new String[] { "getPlatform", "method_668" });
                invokeBest(platform, new String[] { "openUri", "method_674", "method_66818" }, url);
                return;
            } catch (IllegalStateException ignored) {
            }
        }
        throw new IllegalStateException("Unable to open URI");
    }

    public static SoundEvent createVariableRangeEvent(Object location) {
        try {
            for (Method method : SoundEvent.class.getMethods()) {
                if (!matchesName(method, "createVariableRangeEvent", "method_47908") || method.getParameterCount() != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter.isInstance(location)) {
                    return (SoundEvent) method.invoke(null, location);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create sound event", e);
        }
        throw new IllegalStateException("Unable to find createVariableRangeEvent overload");
    }

    public static Object registerTexture(DynamicTexture texture, String namespace, String path) {
        Object location = resource(namespace, path);
        Object textureManager = Minecraft.getInstance().getTextureManager();
        invokeBest(textureManager, new String[] { "register", "method_4616" }, location, texture);
        return location;
    }

    public static void enableLinearFiltering(DynamicTexture texture) {
        if (tryInvokeTextureFilter(texture)) {
            return;
        }

        try {
            Object sampler = invokeBest(texture, new String[] { "getSampler" });
            if (tryInvokeSamplerFilter(sampler)) {
                return;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void releaseTexture(Object location) {
        invokeBest(Minecraft.getInstance().getTextureManager(), new String[] { "release", "method_4615" }, location);
    }

    public static java.util.Optional<Resource> findResource(ResourceManager manager, String namespace, String path) {
        Object location = resource(namespace, path);
        Object result = invokeBest(manager, new String[] { "getResource", "method_14486" }, location);
        @SuppressWarnings("unchecked")
        java.util.Optional<Resource> resource = (java.util.Optional<Resource>) result;
        return resource;
    }

    public static void blitGuiTexture(GuiGraphics graphics, Object pipeline, Object location, int x, int y,
                                      float u, float v, int width, int height, int textureWidth, int textureHeight,
                                      int imageWidth, int imageHeight) {
        invokeBest(graphics, new String[] { "blit", "method_25293", "method_25302" }, pipeline, location, x, y, u, v, width, height,
                textureWidth, textureHeight, imageWidth, imageHeight);
    }

    public static VertexConsumer getWaypointBuffer(MultiBufferSource bufferSource) {
        Object renderType = getWaypointRenderType();
        Object buffer = invokeBest(bufferSource, new String[] { "getBuffer", "method_73477" }, renderType);
        return (VertexConsumer) buffer;
    }

    private static Object getWaypointRenderType() {
        Object whiteTexture = Objects.requireNonNullElseGet(
                tryParseResource("minecraft:textures/block/white_concrete.png"),
                () -> resource("minecraft", "textures/block/white_concrete.png"));

        for (String name : RENDER_TYPE_CLASSES) {
            Class<?> type = findClass(name);
            if (type == null) continue;
            try {
                return invokeStaticBest(type, new String[] { "entityTranslucent", "method_23580", "method_76000" }, whiteTexture);
            } catch (IllegalStateException ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve waypoint render type");
    }

    public static Object invokeBest(Object target, String[] names, Object... args) {
        Class<?> type = target.getClass();
        for (Method method : type.getMethods()) {
            if (!matchesName(method, names) || method.getParameterCount() != args.length) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = args[i];
                if (arg == null) continue;
                if (!wrap(parameterTypes[i]).isInstance(arg)) {
                    matches = false;
                    break;
                }
            }
            if (!matches) continue;
            try {
                return method.invoke(target, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke " + Arrays.toString(names) + " with " + Arrays.toString(args), e);
            }
        }
        throw new IllegalStateException("No matching method found: " + Arrays.toString(names));
    }

    public static Object invokeStaticBest(Class<?> type, String[] names, Object... args) {
        for (Method method : type.getMethods()) {
            if (!matchesName(method, names) || method.getParameterCount() != args.length) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = args[i];
                if (arg == null) continue;
                if (!wrap(parameterTypes[i]).isInstance(arg)) {
                    matches = false;
                    break;
                }
            }
            if (!matches) continue;
            try {
                return method.invoke(null, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke " + Arrays.toString(names) + " with " + Arrays.toString(args), e);
            }
        }
        throw new IllegalStateException("No matching method found: " + Arrays.toString(names));
    }

    private static boolean matchesName(Method method, String... names) {
        for (String name : names) {
            if (method.getName().equals(name)) return true;
        }
        return false;
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static boolean tryInvokeTextureFilter(DynamicTexture texture) {
        for (Method method : texture.getClass().getMethods()) {
            if (!matchesName(method, "setFilter", "setBlurMipmap")) continue;
            try {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                    method.invoke(texture, true);
                    return true;
                }
                if (method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == boolean.class
                        && method.getParameterTypes()[1] == boolean.class) {
                    method.invoke(texture, true, false);
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }

    private static boolean tryInvokeSamplerFilter(Object sampler) {
        for (Method method : sampler.getClass().getMethods()) {
            if (!matchesName(method, "setFilter")) continue;
            try {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                    method.invoke(sampler, true);
                    return true;
                }
                if (method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == boolean.class
                        && method.getParameterTypes()[1] == boolean.class) {
                    method.invoke(sampler, true, false);
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }
}
