package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.lang.reflect.Method;

public class ClassHelper {

    public static Option<Class<?>> tryGetClass(String name) {
        return Try.of(() -> Class.forName(name)).toOption()
                .map(c -> c);
    }

    public static Option<Method> tryGetMethod(Class<?> clazz, String name, Class<?>... args) {
        return Try.of(() -> clazz.getMethod(name, args)).toOption();
    }

}
