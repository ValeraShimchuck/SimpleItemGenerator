package ua.valeriishymchuk.simpleitemgenerator.common.commands.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.context.CommandContext;
import org.apiguardian.api.API;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class CustomPlayerArgument<C> extends CommandArgument<C, Player> {

    private CustomPlayerArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @NonNull ArgumentParser<C, Player> parser,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, parser, defaultValue, Player.class, suggestionsProvider, defaultDescription);
    }

    /**
     * Create a new {@link PlayerArgument.Builder}.
     *
     * @param name argument name
     * @param <C>  sender type
     * @return new {@link PlayerArgument.Builder}
     * @since 1.8.0
     */
    @API(status = API.Status.STABLE, since = "1.8.0")
    public static <C> Builder<C> builder(final @NonNull String name) {
        return new Builder<>(name);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     * @deprecated prefer {@link #builder(String)}
     */
    @API(status = API.Status.DEPRECATED, since = "1.8.0")
    @Deprecated
    public static <C> Builder<C> newBuilder(final @NonNull String name) {
        return builder(name);
    }

    /**
     * Create a new required command component
     *
     * @param name Component name
     * @param <C>  Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Player> of(final @NonNull String name) {
        return PlayerArgument.<C>builder(name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param name Component name
     * @param <C>  Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Player> optional(final @NonNull String name) {
        return PlayerArgument.<C>builder(name).asOptional().build();
    }

    /**
     * Create a new required command component with a default value
     *
     * @param name          Component name
     * @param defaultPlayer Default player
     * @param <C>           Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Player> optional(
            final @NonNull String name,
            final @NonNull String defaultPlayer
    ) {
        return PlayerArgument.<C>builder(name).asOptionalWithDefault(defaultPlayer).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, Player> {

        private Builder(final @NonNull String name) {
            super(Player.class, name);
        }

        /**
         * Builder a new boolean component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull CustomPlayerArgument<C> build() {
            return new CustomPlayerArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getParser(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }
    }



}
