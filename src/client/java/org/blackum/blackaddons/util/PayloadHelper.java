package org.blackum.blackaddons.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;

public class PayloadHelper {

    private static final String TRY_PARSE_METHOD = "tryParse";
    private static final String ERROR_TRY_PARSE_NOT_FOUND = "[ModHider] Could not find tryParse in ";
    private static final String ERROR_REFLECTION_FAILED = "[ModHider] Failed to create payload via reflection";

    public static CustomPacketPayload createRegisterPayload(CustomPacketPayload original, Set<String> channels) {
        try {
            Class<?> clazz = original.getClass();
            Object sampleId = original.type().id();
            Class<?> idClass = sampleId.getClass();

            Method tryParse = null;
            try {
                tryParse = idClass.getMethod(TRY_PARSE_METHOD, String.class);
            } catch (NoSuchMethodException e) {
                Blackaddons.LOGGER.error(ERROR_TRY_PARSE_NOT_FOUND + idClass.getName());
                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    ReflectionDump.dumpClass(idClass);
                }

                return null;
            }

            final Method parseMethod = tryParse;

            List<Object> idList = channels.stream()
                    .map(s -> {
                        try {
                            return parseMethod.invoke(null, s);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] paramTypes = c.getParameterTypes();

                if (c.getParameterCount() == 1) {
                    Class<?> paramType = paramTypes[0];
                    if (List.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(idList);
                    } else if (Set.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(new HashSet<>(idList));
                    } else if (Collection.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(idList);
                    }
                } else if (c.getParameterCount() == 2) {
                    if (CustomPacketPayload.Type.class.isAssignableFrom(paramTypes[0])
                            && Collection.class.isAssignableFrom(paramTypes[1])) {
                        return (CustomPacketPayload) c.newInstance(original.type(), idList);
                    }
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error(ERROR_REFLECTION_FAILED, e);
        }
        return null;
    }
}
