package org.blackum.blackaddons.common.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 100-199 - cheats/common runtime
 * 200-399 - dungeon/solver/map handlers
 * 400-499 - HUD
 * 900-999 - commands/late registrations
 * Lower values are first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoModule {
    int order() default 0;
}
