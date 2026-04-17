package org.blackum.blackaddons.common.util;

import java.text.DecimalFormat;

public class NumbersUtils {
    private static final DecimalFormat df = new DecimalFormat("#.#");

    public static String toFixed(double value, int places) {
        return df.format(value);
    }
}
