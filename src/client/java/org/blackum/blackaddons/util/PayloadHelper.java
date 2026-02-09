package org.blackum.blackaddons.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PayloadHelper {
    public static CustomPacketPayload createRegisterPayload(CustomPacketPayload original, Set<String> channels) {
        try {
            Class<?> clazz = original.getClass();

            List<Identifier> idList = channels.stream()
                    .map(s -> Identifier.tryParse(s))
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
                        return (CustomPacketPayload) c.newInstance(new java.util.HashSet<>(idList));
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
