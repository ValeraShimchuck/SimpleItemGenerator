package ua.valeriishymchuk.simpleitemgenerator.common.config.exception;

import java.util.List;

public class InvalidConfigurationException extends RuntimeException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidConfigurationException path(String path, Throwable cause) {
        return new InvalidConfigurationException("Error in <white>[" + path + "]</white>", cause);
    }

    public static InvalidConfigurationException format(Throwable cause, String message, Object... args) {
        return new InvalidConfigurationException(String.format(message, args), cause);
    }
    public static InvalidConfigurationException format(String message, Object... args) {
        try {
            return new InvalidConfigurationException(String.format(message, args));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid format string: " + message, e);
        }

    }

    public static InvalidConfigurationException unknownOption(String optionName, String chosenOption, List<String> suggestions) {
        return InvalidConfigurationException.format(
                "Unknown <white>%s</white> option: <white>%s</white>. %s",
                optionName,
                chosenOption,
                (!suggestions.isEmpty() ? "Did you mean: <white>" + suggestions + "</white>" : "")
        );
    }

}
