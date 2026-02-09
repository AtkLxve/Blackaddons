package org.blackum.blackaddons.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PayloadHelper {
    public static CustomPacketPayload createRegisterPayload(CustomPacketPayload original, Set<String> channels) {
        try {
            Class<?> clazz = original.getClass();
            Object sampleId = original.type().id();
            Class<?> idClass = sampleId.getClass();

            Method tryParse = null;
            try {
                tryParse = idClass.getMethod("tryParse", String.class);
            } catch (NoSuchMethodException e) {
                org.blackum.blackaddons.Blackaddons.LOGGER
                        .error("[ModHider] Could not find tryParse in " + idClass.getName());
                ReflectionDump.dumpClass(idClass);
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
                    .filter(java.util.Objects::nonNull)
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
            org.blackum.blackaddons.Blackaddons.LOGGER.error("[ModHider] Failed to create payload via reflection", e);
        }
        return null;
    }
}
