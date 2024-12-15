package ua.valeriishymchuk.itemgenerator.common.regex;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    public static String replaceAll(Matcher matcher, Function<MatchResult, String> replacer) {
        Objects.requireNonNull(replacer);
        matcher.reset();
        boolean result = matcher.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            do {

                String replacement =  replacer.apply(matcher);
                matcher.appendReplacement(sb, replacement);
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            return sb.toString();
        }
        return  matcher.replaceAll("");
    }

}
