package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ParsingException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ConfigRepository implements IConfigRepository {

    final ConfigLoader configLoader;
    final Logger logger;
    ConfigEntity config;
    LangEntity lang;

    @Override
    public ConfigEntity getConfig() {
        return config;
    }

    @Override
    public LangEntity getLang() {
        return lang;
    }

    @Override
    public boolean reload() {
        String currentConfig = "config";
        ConfigEntity oldConfig = config;
        LangEntity oldLang = lang;
        try {
            config = configLoader.loadOrSave(ConfigEntity.class, "config");
            config.init();
            currentConfig = "lang";
            lang = configLoader.loadOrSave(LangEntity.class, "lang");
            return true;
        } catch (Throwable e) {
            currentConfig = currentConfig + ".yml";
            KyoriHelper.sendMessage(Bukkit.getConsoleSender(), String.format("<red>[SimpleItemGenerator] Error in configuration <white>%s</white></red>", currentConfig));
            visitError(e);
            config = oldConfig;
            lang = oldLang;
        }
        return false;
    }

    private void visitError(Throwable e) {
        if (e == null) return;
        List<Class<? extends Throwable>> silentExceptions = Arrays.asList(
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
            KyoriHelper.sendMessage(Bukkit.getConsoleSender(), String.format("<red>[SimpleItemGenerator] %s</red>", e.getMessage()));
            //logger.severe(e.getMessage());
        } else if (!isSilent) {
            logger.log(Level.SEVERE, "An unknown occurred", e);
            KyoriHelper.sendMessage(Bukkit.getConsoleSender(), "<red>[SimpleItemGenerator] Please report this error there: https://github.com/ValeraShimchuck/SimpleItemGenerator/issues</red>");
        }
        visitError(e.getCause());
    }


}
