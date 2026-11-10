package de.creativedm.ultra.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Durations like "30m", "1d12h" or "2w". */
public final class Durations {

    private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])");

    private Durations() {}

    /** @return milliseconds, or -1 if the text is not a duration */
    public static long parse(String text) {
        if (text == null || text.isEmpty()) return -1;
        Matcher matcher = PART.matcher(text.toLowerCase());
        long total = 0;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) return -1;
            end = matcher.end();
            long amount = Long.parseLong(matcher.group(1));
            total += amount * switch (matcher.group(2).charAt(0)) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                default -> 604_800_000L;
            };
        }
        return end == text.length() && total > 0 ? total : -1;
    }

    /** The two largest units, e.g. "1d 2h". */
    public static String format(long millis) {
        long seconds = Math.max(1, (millis + 999) / 1000);
        long[] sizes = {86_400, 3_600, 60, 1};
        String[] names = {"d", "h", "m", "s"};
        StringBuilder out = new StringBuilder();
        int parts = 0;
        for (int i = 0; i < sizes.length && parts < 2; i++) {
            long amount = seconds / sizes[i];
            if (amount == 0) continue;
            seconds -= amount * sizes[i];
            if (out.length() > 0) out.append(' ');
            out.append(amount).append(names[i]);
            parts++;
        }
        return out.toString();
    }
}
