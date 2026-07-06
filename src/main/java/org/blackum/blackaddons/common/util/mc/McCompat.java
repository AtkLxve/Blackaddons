package org.blackum.blackaddons.common.util.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
//? if >=26.2 {

/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
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
                for (String methodName : new String[] { "create", "method_24045", "method_24048", "method_75940" }) {
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
        Class<?> renderSetupClass = null;
        for (String cn : new String[] {
                "net.minecraft.client.renderer.rendertype.RenderSetup",
                "net.minecraft.class_12247"
        }) {
            renderSetupClass = findClass(cn);
            if (renderSetupClass != null) break;
        }
        if (renderSetupClass != null) {
            try {
                Object builder = invokeDeclaredStaticBest(renderSetupClass,
                        new String[] { "builder", "method_75927" }, pipeline);
                Object textured = invokeDeclaredBest(builder,
                        new String[] { "withTexture", "method_75934" }, "Sampler0", location);
                Object renderSetup = invokeDeclaredBest(textured,
                        new String[] { "createRenderSetup", "method_75938" });
                return createRenderType(name, renderSetup);
            } catch (IllegalStateException ignored) {
            }
        }

        Class<?> renderTypeClass = null;
        for (String cn : RENDER_TYPE_CLASSES) {
            renderTypeClass = findClass(cn);
            if (renderTypeClass != null) break;
        }

        Class<?> compositeStateClass = null;
        for (String cn : new String[] {
                "net.minecraft.client.renderer.RenderType$CompositeState",
                "net.minecraft.class_1921$class_4688"
        }) {
            compositeStateClass = findClass(cn);
            if (compositeStateClass != null) break;
        }

        Class<?> textureStateClass = null;
        for (String cn : new String[] {
                "net.minecraft.client.renderer.RenderStateShard$TextureStateShard",
                "net.minecraft.class_4668$class_4683"
        }) {
            textureStateClass = findClass(cn);
            if (textureStateClass != null) break;
        }

        if (renderTypeClass == null || compositeStateClass == null || textureStateClass == null) {
            throw new IllegalStateException("Unable to resolve RenderType internals for text rendering");
        }

        try {
            Object compositeBuilder = invokeDeclaredStaticBest(compositeStateClass,
                    new String[] { "builder", "method_23598" });
            java.lang.reflect.Constructor<?> textureStateCtor = null;
            for (java.lang.reflect.Constructor<?> ctor : textureStateClass.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[1] == boolean.class) {
                    textureStateCtor = ctor;
                    break;
                }
            }
            if (textureStateCtor == null) {
                throw new IllegalStateException("Unable to find TextureStateShard constructor");
            }
            textureStateCtor.setAccessible(true);
            Object textureState = textureStateCtor.newInstance(location, true);
            Object texturedBuilder = invokeDeclaredBest(compositeBuilder,
                    new String[] { "setTextureState", "method_34577" }, textureState);

            Object compositeState = null;
            for (String cn : new String[] {
                    "net.minecraft.client.renderer.RenderType$OutlineProperty",
                    "net.minecraft.class_1921$class_4750"
            }) {
                Class<?> outlinePropClass = findClass(cn);
                if (outlinePropClass != null) {
                    try {
                        Object none = outlinePropClass.getEnumConstants()[0];
                        compositeState = invokeDeclaredBest(texturedBuilder,
                                new String[] { "createCompositeState", "method_24297" }, none);
                        break;
                    } catch (IllegalStateException ignored) {
                    }
                }
            }
            if (compositeState == null) {
                compositeState = invokeDeclaredBest(texturedBuilder,
                        new String[] { "createCompositeState", "method_24297" }, false);
            }

            String[] createNames = { "create", "method_24045", "method_24048", "method_24049" };
            Object[][] argSets = {
                    { name, 1536, pipeline, compositeState },
                    { name, 1536, false, true, pipeline, compositeState }
            };
            for (Object[] args : argSets) {
                for (Method method : renderTypeClass.getDeclaredMethods()) {
                    if (!matchesName(method, createNames) || method.getParameterCount() != args.length) continue;
                    Class<?>[] paramTypes = method.getParameterTypes();
                    boolean matches = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (args[i] == null) continue;
                        if (!wrap(paramTypes[i]).isInstance(args[i])) { matches = false; break; }
                    }
                    if (!matches) continue;
                    try {
                        method.setAccessible(true);
                        return method.invoke(null, args);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed to invoke RenderType.create", e);
                    }
                }
            }
            throw new IllegalStateException("No matching RenderType.create method found");
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

    public static void blitGuiTexture(GuiGraphicsExtractor graphics, Object pipeline, Object location, int x, int y,
                                      float u, float v, int width, int height, int textureWidth, int textureHeight,
                                      int imageWidth, int imageHeight) {
        invokeBest(graphics, new String[] { "blit", "method_25293", "method_25302" }, pipeline, location, x, y, u, v, width, height,
                textureWidth, textureHeight, imageWidth, imageHeight);
    }

    public interface GeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }

//? if >=26.2 {

    /*public static void drawGeometry(SubmitNodeCollector bufferSource, PoseStack poseStack, Object renderType, GeometryRenderer renderer) {
        bufferSource.submitCustomGeometry(poseStack, (RenderType) renderType, renderer::render);
    }

    public static void drawString(SubmitNodeCollector collector, PoseStack poseStack, String text, float x, float y, int color, boolean dropShadow, Object font, boolean seeThrough) {
        Font.DisplayMode displayMode = seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
        collector.submitText(poseStack, x, y, FormattedCharSequence.forward(text, Style.EMPTY), dropShadow, displayMode, color, 0, 15728880, 0);
    }

    public static Screen getScreen(Minecraft client) {
        return client.gui.screen();
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    public static ChatComponent getChat(Minecraft client) {
        return client.gui.hud.getChat();
    }

    public static void setTimes(Minecraft client, int fadeIn, int stay, int fadeOut) {
        client.gui.hud.setTimes(fadeIn, stay, fadeOut);
    }

    public static void setTitle(Minecraft client, Component title) {
        client.gui.hud.setTitle(title);
    }

    public static void setSubtitle(Minecraft client, Component subtitle) {
        client.gui.hud.setSubtitle(subtitle);
    }

    public static Camera getCamera(GameRenderer renderer) {
        return renderer.mainCamera();
    }

    public static boolean isGuiHidden(Minecraft client) {
        return client.gui.hud.isHidden();
    }

*///?} else {
    public static void drawGeometry(MultiBufferSource bufferSource, PoseStack poseStack, Object renderType, GeometryRenderer renderer) {
        VertexConsumer consumer = (VertexConsumer) invokeBest(bufferSource, new String[] { "getBuffer", "method_73477" }, renderType);
        renderer.render(poseStack.last(), consumer);
    }

    public static void drawString(MultiBufferSource bufferSource, PoseStack poseStack, String text, float x, float y, int color, boolean dropShadow, Object font, boolean seeThrough) {
        Font.DisplayMode displayMode = seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
        Matrix4f matrix = poseStack.last().pose();
        ((Font) font).drawInBatch(text, x, y, color, dropShadow, matrix, bufferSource, displayMode, 0, 15728880);
    }

    public static Screen getScreen(Minecraft client) {
        return client.screen;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    public static ChatComponent getChat(Minecraft client) {
        return client.gui.getChat();
    }

    public static void setTimes(Minecraft client, int fadeIn, int stay, int fadeOut) {
        client.gui.setTimes(fadeIn, stay, fadeOut);
    }

    public static void setTitle(Minecraft client, Component title) {
        client.gui.setTitle(title);
    }

    public static void setSubtitle(Minecraft client, Component subtitle) {
        client.gui.setSubtitle(subtitle);
    }

    public static Camera getCamera(GameRenderer renderer) {
        return renderer.getMainCamera();
    }

    public static boolean isGuiHidden(Minecraft client) {
        return client.options.hideGui;
    }

    public static VertexConsumer getWaypointBuffer(MultiBufferSource bufferSource) {
        Object renderType = getWaypointRenderType();
        Object buffer = invokeBest(bufferSource, new String[] { "getBuffer", "method_73477" }, renderType);
        return (VertexConsumer) buffer;
    }
//?}

    public static float getFov(GameRenderer renderer, Camera camera, float partialTicks, boolean useFovSetting) {
        Object fov = invokeDeclaredBest(renderer, new String[] { "getFov", "method_3196", "a" },
                camera, partialTicks, useFovSetting);
        return ((Number) fov).floatValue();
    }

    public static Object getWaypointRenderType() {
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

    private static Object invokeDeclaredBest(Object target, String[] names, Object... args) {
        Class<?> cls = target.getClass();
        while (cls != null) {
            for (Method method : cls.getDeclaredMethods()) {
                if (!matchesName(method, names) || method.getParameterCount() != args.length) continue;
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (args[i] == null) continue;
                    if (!wrap(parameterTypes[i]).isInstance(args[i])) { matches = false; break; }
                }
                if (!matches) continue;
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to invoke " + Arrays.toString(names), e);
                }
            }
            cls = cls.getSuperclass();
        }
        throw new IllegalStateException("No matching declared method found: " + Arrays.toString(names));
    }

    private static Object invokeDeclaredStaticBest(Class<?> type, String[] names, Object... args) {
        for (Method method : type.getDeclaredMethods()) {
            if (!matchesName(method, names) || method.getParameterCount() != args.length) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] == null) continue;
                if (!wrap(parameterTypes[i]).isInstance(args[i])) { matches = false; break; }
            }
            if (!matches) continue;
            try {
                method.setAccessible(true);
                return method.invoke(null, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke " + Arrays.toString(names), e);
            }
        }
        throw new IllegalStateException("No matching declared method found: " + Arrays.toString(names));
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
