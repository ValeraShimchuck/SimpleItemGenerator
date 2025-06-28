package ua.valeriishymchuk.simpleitemgenerator.common.config.error;

import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.loader.ParsingException;
import org.yaml.snakeyaml.scanner.ScannerException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;

import java.nio.charset.MalformedInputException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ConfigurationError {

    private ConfigurationError() {}

    public boolean isFileNotPresent() {
        return this instanceof FileNotPresent;
    }

    public abstract InvalidConfigurationException asConfigException();

    public static <T> Validation<ConfigurationError, T> handleValidationException(Throwable e) {
        return Validation.invalid(handleException(e));
    }

    public static ConfigurationError handleException(Throwable e) {
        if (e instanceof MalformedInputException) {
            return new ConfigurationError.MalformedInput(e.getMessage());
        } else if (e instanceof ScannerException) {
            return new ConfigurationError.InvalidSyntax(e.getMessage());
        } else if (e instanceof ParsingException) {
            if (e.getCause() == null) return new UnknownException(e);
            return handleException(e.getCause());
        } else return new ConfigurationError.UnknownException(e);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @Getter
    public static final class UnknownException extends ConfigurationError {
        Throwable originalError;
        public UnknownException(Throwable err) {
            this.originalError = err;
        }

        @Override
        public InvalidConfigurationException asConfigException() {
            return InvalidConfigurationException.unhandledException(originalError);
        }
    }

    public static final class FileNotPresent extends ConfigurationError {
        public static final FileNotPresent INSTANCE = new FileNotPresent();

        @Override
        public InvalidConfigurationException asConfigException() {
            return InvalidConfigurationException.format("File is not present.");
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static final class MalformedInput extends ConfigurationError {
        String message;

        @Override
        public InvalidConfigurationException asConfigException() {
            return InvalidConfigurationException.format("An invalid character were found." +
                    " Make sure you saved your configuration in UTF-8 encoding! " +
                    "\nMalformedInput message: %s", message);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static final class InvalidSyntax extends ConfigurationError {
        String message;

        @Override
        public InvalidConfigurationException asConfigException() {
            return InvalidConfigurationException.format("Check syntax in your configuration: \n%s", message);
        }
    }

}
