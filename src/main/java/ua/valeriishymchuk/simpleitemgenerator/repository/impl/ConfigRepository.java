package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Bukkit;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ParsingException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.error.ConfigurationError;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemsStorageEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ItemLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ConfigRepository implements IConfigRepository {

    final ConfigLoader configLoader;
    MainConfigEntity config = new MainConfigEntity();
    LangEntity lang = new LangEntity();
    final ErrorVisitor errorVisitor;

    @Override
    public MainConfigEntity getConfig() {
        return config;
    }

    @Override
    public boolean doesMainConfigExist() {
        return configLoader.exists("config");
    }


    @Override
    public LangEntity getLang() {
        return lang;
    }


    private void handleError(ConfigurationError error, String config) {
        config = config + ".yml";
        KyoriHelper.sendMessage(
                Bukkit.getConsoleSender(),
                String.format("<red>[SimpleItemGenerator] Error in configuration <white>%s</white></red>", config)
        );
        errorVisitor.visitError(error.asConfigException());
    }

    @Override
    public boolean reload() {
        AtomicBoolean isSuccessful = new AtomicBoolean(true);
        final Validation<ConfigurationError, MainConfigEntity> configLoadResult;
        configLoadResult = configLoader.loadOrSave(MainConfigEntity.class, "config");
        configLoadResult.peek(newConfig -> config = newConfig)
                .toEither().swap()
                .peek(e -> handleError(e, "config"))
                .peek(e -> isSuccessful.set(false));
        final Validation<ConfigurationError, LangEntity> langLoadResult;
        langLoadResult = configLoader.loadOrSave(LangEntity.class, "lang");
        langLoadResult.peek(newConfig -> lang = newConfig)
                .toEither().swap()
                .peek(e -> handleError(e, "lang"))
                .peek(e -> isSuccessful.set(false));
        return isSuccessful.get();
    }




}
