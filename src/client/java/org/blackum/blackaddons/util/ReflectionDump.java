package org.blackum.blackaddons.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.blackum.blackaddons.Blackaddons;

public class ReflectionDump {
    public static void dumpClass(Class<?> clazz) {
        Blackaddons.LOGGER.info("=== Reflection Dump: " + clazz.getName() + " ===");

        Blackaddons.LOGGER.info("--- Methods ---");
        for (Method m : clazz.getDeclaredMethods()) {
            String params = Arrays.stream(m.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
            Blackaddons.LOGGER
                    .info(m.getName() + "(" + params + ") -> " + m.getReturnType().getSimpleName());
        }

        Blackaddons.LOGGER.info("--- Fields ---");
        for (Field f : clazz.getDeclaredFields()) {
            Blackaddons.LOGGER.info(f.getName() + " : " + f.getType().getSimpleName());
        }

        Blackaddons.LOGGER.info("===========================================");
    }
}
