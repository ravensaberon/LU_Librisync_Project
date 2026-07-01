package com.lulibrisync.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YearLevelOptions {

    private static final List<String> OPTIONS = List.of(
            "1st Year",
            "2nd Year",
            "3rd Year",
            "4th Year"
    );

    private YearLevelOptions() {
    }

    public static List<String> getOptions() {
        return OPTIONS;
    }

    public static Map<String, Boolean> getOptionLookup() {
        Map<String, Boolean> lookup = new LinkedHashMap<>();
        for (String option : OPTIONS) {
            lookup.put(option, Boolean.TRUE);
        }
        return Map.copyOf(lookup);
    }

    public static boolean isSupported(String value) {
        return OPTIONS.contains(value);
    }
}
