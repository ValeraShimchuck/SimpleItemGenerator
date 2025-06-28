package ua.valeriishymchuk.simpleitemgenerator.common.config.tools;

import io.vavr.API;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.text.StringSimilarityUtils;

import java.util.Arrays;
import java.util.List;

public class ConfigParsingHelper {
    public static <E extends Enum<E>> Validation<InvalidConfigurationException, E> parseEnum(
            Class<E> eClass,
            String raw,
            String optionName
    ) {
        return parseEnum(eClass, raw, optionName, optionName);
    }

    public static <E extends Enum<E>> Validation<InvalidConfigurationException, E> parseEnum(
            Class<E> eClass,
            String raw,
            String optionName,
            String path

    ) {
        return parseEnumWithoutPath(eClass, raw, optionName)
                .mapError(InvalidConfigurationException.Lambda.path(path));
    }

    public static <E extends Enum<E>> Validation<InvalidConfigurationException, E> parseEnumWithoutPath(
            Class<E> eClass,
            String raw,
            String optionName
    ) {
        String upper = raw.toUpperCase();
        return Validation.fromTry(Try.ofSupplier(() -> Enum.valueOf(eClass, upper)))
                .mapError(e -> {
                    if (e instanceof IllegalArgumentException) {
                        List<String> suggestions = StringSimilarityUtils.getSuggestions(
                                upper,
                                Arrays.stream(eClass.getEnumConstants())
                                        .map(Enum<E>::name)
                        );
                        return InvalidConfigurationException.unknownOption(optionName, raw, suggestions);
                    }
                    return InvalidConfigurationException.unhandledException(e);
                });
    }

}
