package ua.valeriishymchuk.itemgenerator.common.reflection;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Method;
import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ReflectionObject {

    Class<?> clazz;
    Object object;

    public static ReflectionObject of(Object object) {
        return new ReflectionObject(object.getClass(), object);
    }

    public static ReflectionObject[] ofArray(Object... objects) {
        return Arrays.stream(objects).map(ReflectionObject::of).toArray(ReflectionObject[]::new);
    }

    public static ReflectionObject ofStatic(Class<?> clazz) {
        return new ReflectionObject(clazz, null);
    }

    @SneakyThrows
    public static ReflectionObject newInstance(Class<?> clazz, Object... args) {
        return newInstance(clazz, ofArray(args));
    }

    @SneakyThrows
    public static ReflectionObject newInstance(Class<?> clazz, ReflectionObject... args) {
        return new ReflectionObject(clazz,
                clazz.getConstructor(toClassesArray(args))
                        .newInstance(toObjectsArray(args))
        );
    }

    public static Class<?>[] toClassesArray(ReflectionObject... args) {
        return Arrays.stream(args).map(ReflectionObject::getClazz).toArray(Class<?>[]::new);
    }

    public static Object[] toObjectsArray(ReflectionObject... args) {
        return Arrays.stream(args).map(ReflectionObject::getObject).toArray();
    }

    public <T> T cast() {
        return (T) object;
    }

    @SneakyThrows
    public Option<ReflectionObject> invokePublic(String methodName, Object... args) {
        return invokePublic(methodName, ofArray(args));
    }

    @SneakyThrows
    public Option<ReflectionObject> invokePublic(String methodName, ReflectionObject... args) {
        //Method method = clazz.getMethod(methodName, toClassesArray(args));
        Method method = Arrays.stream(clazz.getMethods())
                .filter(filterMethod -> filterMethod.getName().equals(methodName))
                .filter(filterMethod -> {
                    if (filterMethod.getParameterCount() != args.length) return false;
                    for (int i = 0; i < args.length; i++) {
                        if (!filterMethod.getParameterTypes()[i].isAssignableFrom(args[i].getClazz())) return false;
                    }
                    return true;
                }).findFirst().orElseThrow(() -> new NullPointerException(
                        "Method " + methodName + " with parameters " + Arrays.toString(toClassesArray(args)) + " not found"
                ));
        Class<?> returnClass = method.getReturnType();
        return Option.of(method.invoke(object, toObjectsArray(args)))
                .map(obj -> new ReflectionObject(returnClass, obj));
    }


}
