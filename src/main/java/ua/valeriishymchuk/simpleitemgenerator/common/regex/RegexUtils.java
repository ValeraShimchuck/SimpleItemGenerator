package ua.valeriishymchuk.simpleitemgenerator.common.regex;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;

public class RegexUtils {

    public static String replaceAll(Matcher matcher, Function<Matcher, String> replacer) {
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
