package ua.valeriishymchuk.simpleitemgenerator.common.config;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ConfigLoader {

    File folder;
    String extension;
    private IConfigLoaderConfigurator configurator;

    public <T, C> T applyContext(Class<C> clazz, String key, Function<ContextLoader<C>, T> mapper) {
        validateConfigClass(clazz);
        return mapper.apply(new ContextLoader<>(clazz, key));
    }

    public <C> C loadOrSave(Class<C> clazz, String key, C defaultConfig) {
        return applyContext(clazz, key, c -> c.load().getOrElse(() -> {
            c.save(defaultConfig);
            return defaultConfig;
        }));
    }

    public <C> C loadOrSave(Class<C> clazz, String key) {
        validateConfigClass(clazz);
        return loadOrSave(clazz, key, emptyConstructor(clazz).get());
    }

    @SneakyThrows
    public <C> Option<C> load(Class<C> clazz, String key) {
        validateConfigClass(clazz);
        File file = getFile(key);
        if (!file.exists()) return Option.none();
        ConfigurationLoader<?> loader = configurator.configure(file);
        ConfigurationNode rootNode = loader.load();
        return Option.of(rootNode.get(clazz));
    }

    public <C> C loadOrDefault(Class<C> clazz, String key, C defaultConfig) {
        return load(clazz, key).getOrElse(defaultConfig);
    }

    public <C> C loadOrDefault(Class<C> clazz, String key) {
        validateConfigClass(clazz);
        return loadOrDefault(clazz, key, emptyConstructor(clazz).get());
    }

    @SneakyThrows
    public <C> void save(Class<C> clazz, String key, C config) {
        validateConfigClass(clazz);
        File file = getFile(key);
        ConfigurationLoader<?> loader = configurator.configure(file);
        ConfigurationNode rootNode = loader.load();
        rootNode.set(clazz, config);
        loader.save(rootNode);
    }

    public <C> void saveDefault(Class<C> clazz, String key) {
        validateConfigClass(clazz);
        save(clazz, key, emptyConstructor(clazz).get());
    }

    private <C> Supplier<C> emptyConstructor(Class<C> clazz) {
        Constructor<?> constructor = Stream.of(
                        clazz.getConstructors(),
                        clazz.getDeclaredConstructors()
                ).flatMap(Arrays::stream)
                .filter(c -> c.getParameterCount() == 0)
                .peek(c -> c.setAccessible(true)).findFirst()
                .get();
        return () -> {
            try {
                return (C) constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private File getFile(String key){
        return new File(folder, key + extension);
    }

    private void validateConfigClass(Class<?> clazz) {
        validateAnnotation(clazz);
        validateEmptyConstructor(clazz);
    }

    private void validateAnnotation(Class<?> clazz) {
        validate(
                Arrays.stream(clazz.getAnnotations()).anyMatch(a -> a instanceof ConfigSerializable),
                "Your %s class doesn't have a %s annotation",
                clazz.getName(),
                ConfigSerializable.class.getName()
        );
    }

    private void validateEmptyConstructor(Class<?> clazz) {
        validate(
                Stream.of(clazz.getConstructors(), clazz.getDeclaredConstructors())
                        .flatMap(Arrays::stream)
                        .anyMatch(c -> c.getParameterCount() == 0),
                "Your %s class doesn't have an empty constructor",
                clazz.getName()
        );
    }

    private void validate(Boolean validationCondition, String errorMessage, Object... args) {
        if (validationCondition) return;
        throw new IllegalStateException(String.format(errorMessage, args));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public class ContextLoader<C> {
        Class<C> clazz;
        String key;

        public Option<C>  load() {
            return ConfigLoader.this.load(clazz, key);
        }

        public C loadOrDefault(C defaultConfig) {
            return ConfigLoader.this.loadOrDefault(clazz, key, defaultConfig);
        }

        public C loadOrDefault() {
            return ConfigLoader.this.loadOrDefault(clazz, key);
        }

        public void save(C config) {
            ConfigLoader.this.save(clazz, key, config);
        }

        public void saveDefault() {
            ConfigLoader.this.saveDefault(clazz, key);
        }

    }

}
