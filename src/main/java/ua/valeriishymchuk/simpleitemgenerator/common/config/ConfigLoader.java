package ua.valeriishymchuk.simpleitemgenerator.common.config;

import io.vavr.CheckedFunction0;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.error.ConfigurationError;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ConfigLoader {

    @Getter
    File folder;
    String extension;
    private IConfigLoaderConfigurator configurator;


    public <T, C> T applyContext(Class<C> clazz, String key, Function<ContextLoader<C>, T> mapper) {
        validateConfigClass(clazz);
        return mapper.apply(new ContextLoader<>(clazz, key));
    }

    public <C> Validation<ConfigurationError, C> loadOrSave(Class<C> clazz, String key, C defaultConfig) {
        return applyContext(clazz, key, c -> {
            final Validation<ConfigurationError, C> loadResult = c.load();
            if (loadResult.isValid()) return loadResult;
            if (loadResult.getError().isFileNotPresent()) {
                return c.save(defaultConfig).map(v -> defaultConfig);
            }
            return loadResult;
        });
    }

    public <C> Validation<ConfigurationError, C> loadOrSave(Class<C> clazz, String key) {
        try {
            validateConfigClass(clazz);
            return loadOrSave(clazz, key, emptyConstructor(clazz).apply());
        } catch (Throwable e) {
            return ConfigurationError.handleValidationException(e);
        }
    }

    public <C> Validation<ConfigurationError, Option<C>> safeLoad(Class<C> clazz, String key) {
        Validation<ConfigurationError, C> rawResult = load(clazz, key);
        if (rawResult.isValid()) return Validation.valid(Option.some(rawResult.get()));
        ConfigurationError err = rawResult.getError();
        if (err instanceof ConfigurationError.FileNotPresent) return Validation.valid(Option.none());
        return Validation.invalid(err);
    }

    @SneakyThrows
    public <C> Validation<ConfigurationError, C> load(Class<C> clazz, String key) {
        try {
            validateConfigClass(clazz);
            File file = getFile(key);
            if (!file.exists()) return Validation.invalid(ConfigurationError.FileNotPresent.INSTANCE);
            ConfigurationLoader<?> loader = configurator.configure(file);
            ConfigurationNode rootNode = loader.load();
            return Validation.valid(Objects.requireNonNull(rootNode.get(clazz)));
        } catch (Throwable e) {
            return ConfigurationError.handleValidationException(e);
        }
    }

    public boolean exists(String key) {
        File file = getFile(key);
        return file.exists() && file.isFile();
    }

    public <C> Validation<ConfigurationError, C> loadOrDefault(Class<C> clazz, String key, C defaultConfig) {
        final Validation<ConfigurationError, C> loadResult = load(clazz, key);
        if (loadResult.isValid()) return loadResult;
        if (loadResult.getError().isFileNotPresent()) return Validation.valid(defaultConfig);
        return loadResult;
    }

    public <C> Validation<ConfigurationError, C> loadOrDefault(Class<C> clazz, String key) {
        try {
            validateConfigClass(clazz);
            return loadOrDefault(clazz, key, emptyConstructor(clazz).apply());
        } catch (Throwable e) {
            return ConfigurationError.handleValidationException(e);
        }
    }

    @SneakyThrows
    public <C> Validation<ConfigurationError, Void> save(Class<C> clazz, String key, C config) {
        try {
            validateConfigClass(clazz);
            File file = getFile(key);
            ConfigurationLoader<?> loader = configurator.configure(file);
            ConfigurationNode rootNode = loader.load();
            rootNode.set(clazz, config);
            loader.save(rootNode);
            return Validation.valid(null);
        } catch (Throwable e) {
            return ConfigurationError.handleValidationException(e);
        }
    }

    public <C> Validation<ConfigurationError, Void> saveDefault(Class<C> clazz, String key) {
        try {
            validateConfigClass(clazz);
            return save(clazz, key, emptyConstructor(clazz).apply());
        } catch (Throwable e) {
            return ConfigurationError.handleValidationException(e);
        }
    }

    private <C> CheckedFunction0<C> emptyConstructor(Class<C> clazz) {
        Constructor<?> constructor = Stream.of(
                        clazz.getConstructors(),
                        clazz.getDeclaredConstructors()
                ).flatMap(Arrays::stream)
                .filter(c -> c.getParameterCount() == 0)
                .peek(c -> c.setAccessible(true)).findFirst()
                .get();
        return () -> (C) constructor.newInstance();
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

        public Validation<ConfigurationError, C>  load() {
            return ConfigLoader.this.load(clazz, key);
        }

        public Validation<ConfigurationError, C> loadOrDefault(C defaultConfig) {
            return ConfigLoader.this.loadOrDefault(clazz, key, defaultConfig);
        }

        public Validation<ConfigurationError, C> loadOrDefault() {
            return ConfigLoader.this.loadOrDefault(clazz, key);
        }

        public Validation<ConfigurationError, Void> save(C config) {
            return ConfigLoader.this.save(clazz, key, config);
        }

        public Validation<ConfigurationError, Void> saveDefault() {
            return ConfigLoader.this.saveDefault(clazz, key);
        }

    }

}
