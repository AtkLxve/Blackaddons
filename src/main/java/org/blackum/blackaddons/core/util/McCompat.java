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
                return invokeStaticBest(type, new String[] { "fromNamespaceAndPath", "method_60655" }, namespace, path);
            } catch (IllegalStateException ignored) {
            }
        }
        throw new IllegalStateException("Unable to construct Minecraft resource identifier");
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

    public static void releaseTexture(Object location) {
        invokeBest(Minecraft.getInstance().getTextureManager(), new String[] { "release", "method_4615" }, location);
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

    private static Object invokeBest(Object target, String[] names, Object... args) {
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

    private static Object invokeStaticBest(Class<?> type, String[] names, Object... args) {
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
}
