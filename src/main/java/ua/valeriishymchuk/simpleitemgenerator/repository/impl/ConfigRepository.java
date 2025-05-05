package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import io.vavr.Tuple;
import io.vavr.control.Option;
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
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemsStorageEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ItemLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

import java.util.*;
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



    @Override
    public boolean reload() {
        String currentConfig = "config";
        MainConfigEntity oldConfig = config;
        LangEntity oldLang = lang;
        try {
            config = configLoader.loadOrSave(MainConfigEntity.class, "config");
            currentConfig = "lang";
            lang = configLoader.loadOrSave(LangEntity.class, "lang");
            return true;
        } catch (Throwable e) {
            currentConfig = currentConfig + ".yml";
            KyoriHelper.sendMessage(Bukkit.getConsoleSender(), String.format("<red>[SimpleItemGenerator] Error in configuration <white>%s</white></red>", currentConfig));
            errorVisitor.visitError(e);
            config = oldConfig;
            lang = oldLang;
        }
        return false;
    }




}
