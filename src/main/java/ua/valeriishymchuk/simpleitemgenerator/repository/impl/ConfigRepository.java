package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ConfigRepository implements IConfigRepository {

    final ConfigLoader configLoader;
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
        try {
            config = configLoader.loadOrSave(ConfigEntity.class, "config");
            lang = configLoader.loadOrSave(LangEntity.class, "lang");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
