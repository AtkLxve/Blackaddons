package org.blackum.blackaddons.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ReflectionDump {
    public static void dumpClass(Class<?> clazz) {
        org.blackum.blackaddons.Blackaddons.LOGGER.info("=== Reflection Dump: " + clazz.getName() + " ===");

        org.blackum.blackaddons.Blackaddons.LOGGER.info("--- Methods ---");
        for (Method m : clazz.getDeclaredMethods()) {
            String params = Arrays.stream(m.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
            org.blackum.blackaddons.Blackaddons.LOGGER
                    .info(m.getName() + "(" + params + ") -> " + m.getReturnType().getSimpleName());
        }

        org.blackum.blackaddons.Blackaddons.LOGGER.info("--- Fields ---");
        for (Field f : clazz.getDeclaredFields()) {
            org.blackum.blackaddons.Blackaddons.LOGGER.info(f.getName() + " : " + f.getType().getSimpleName());
        }

        org.blackum.blackaddons.Blackaddons.LOGGER.info("===========================================");
    }
}
