package ua.valeriishymchuk.simpleitemgenerator.common.error;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ParsingException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ErrorVisitor {

    Logger logger;
    Consumer<String> errorConsumer;


    public void visitError(Throwable e) {
        if (e == null) return;
        List<Class<? extends Throwable>> silentExceptions = List.of(
                ParsingException.class
        );
        List<Class<? extends Throwable>> knownExceptions = Arrays.asList(
                ConfigurateException.class,
                InvalidConfigurationException.class,
                ScannerException.class,
                NumberFormatException.class,
                ParserException.class
        );
        boolean isKnownException = knownExceptions.stream().anyMatch(c -> c.isAssignableFrom(e.getClass()));
        boolean isSilent = silentExceptions.stream().anyMatch(c -> c.isAssignableFrom(e.getClass()));
        if (isKnownException && !isSilent) {
            errorConsumer.accept(String.format("<red>[SimpleItemGenerator] %s</red>", e.getMessage()));
            //logger.severe(e.getMessage());
        } else if (!isSilent) {
            logger.log(Level.SEVERE, "An unknown error occurred", e);
            errorConsumer.accept("<red>[SimpleItemGenerator] Please report this error there: https://github.com/ValeraShimchuck/SimpleItemGenerator/issues</red>");
        }
        visitError(e.getCause());
    }

}
