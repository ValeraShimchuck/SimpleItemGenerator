package ua.valeriishymchuk.simpleitemgenerator.common.registry;


import io.vavr.control.Option;

import java.util.Set;
import java.util.function.Function;

public interface WrappedRegistry<T> {

    Set<String> getPossibleValues();

    Option<T> wrapSafely(String type);

    T wrap(String type);

    default void ensureValueExistence(String type) {
        if (!getPossibleValues().contains(type)) throw new IllegalArgumentException(type + " is not present within possible values");
    }

    default boolean isValid(String type) {
        return getPossibleValues().contains(type);
    }

    static <T> WrappedRegistry<T> of(Set<String> possibleValues, Function<String, T> wrapper) {
        return  new WrappedRegistry<T>() {
            @Override
            public Set<String> getPossibleValues() {
                return possibleValues;
            }

            @Override
            public Option<T> wrapSafely(String type) {
                if (!getPossibleValues().contains(type)) return Option.none();
                return Option.of(wrap(type));
            }

            @Override
            public T wrap(String type) {
                ensureValueExistence(type);
                return wrapper.apply(type);
            }
        };
    }

}
