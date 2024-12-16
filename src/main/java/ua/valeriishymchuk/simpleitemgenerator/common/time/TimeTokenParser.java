package ua.valeriishymchuk.simpleitemgenerator.common.time;

import io.vavr.control.Option;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeTokenParser {

    private static final Pattern TIME_TOKEN_PATTERN = Pattern.compile(
            "((?<value>\\d+)(?<timeunit>[a-z]+)?)( +)?"
    );

    public static long parse(String time) {
        Matcher matcher = TIME_TOKEN_PATTERN.matcher(time);
        long timeMillis = 0L;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group("value"));
            long timeUnitMillis = Option.of(matcher.group("timeunit")).map(unit -> {
                switch (unit) {
                    case "s":
                        return 1000L;
                    case "m":
                        return 1000L * 60L;
                    case "h":
                        return 1000L * 60L * 60L;
                    case "t":
                        return 50L;
                    default:
                        throw new IllegalStateException("Unknown time unit: " + unit);
                }
            }).getOrElse(1L);
            timeMillis += value * timeUnitMillis;
        }
        return timeMillis;
    }

    public static String parse(long timeMillis) {
        Duration duration = Duration.ofMillis(timeMillis);
        long seconds = duration.getSeconds() % 60;
        long minutes = duration.toMinutes() % 60;
        long hours = duration.toHours() % 24;
        long millis = duration.toMillis() % 1000;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");
        if (millis > 0) sb.append(millis);
        return sb.toString().trim();
    }

}
