package com.github.rollingmetrics.util;

import java.util.regex.Pattern;

public class NamingUtils {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    public static String replaceAllWhitespaces(String originalName) {
        return WHITESPACE.matcher(originalName).replaceAll("-");
    }

}
