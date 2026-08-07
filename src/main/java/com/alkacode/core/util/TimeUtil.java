package com.alkacode.core.util;

public final class TimeUtil {
    private TimeUtil() {}

    public static String formatSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString();
    }
}
