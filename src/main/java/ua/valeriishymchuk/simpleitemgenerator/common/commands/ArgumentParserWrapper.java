package ua.valeriishymchuk.simpleitemgenerator.common.commands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ArgumentParserWrapper<C, R> implements ArgumentParser<C, R> {

    ArgumentParser<C, R> parser;
    Function<Throwable,Component> errorMessage;

    @Override
    public @NonNull ArgumentParseResult<@NonNull R> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        return parser.parse(commandContext, inputQueue).mapFailure(e -> new CommandException(errorMessage.apply(e)));
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> commandContext, @NonNull String input) {
        return parser.suggestions(commandContext, input);
    }

    @Override
    public boolean isContextFree() {
        return parser.isContextFree();
    }

    @Override
    public int getRequestedArgumentCount() {
        return parser.getRequestedArgumentCount();
    }
}
