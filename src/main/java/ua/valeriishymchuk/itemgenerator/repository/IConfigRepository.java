package ua.valeriishymchuk.itemgenerator.repository;

import ua.valeriishymchuk.itemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.itemgenerator.entity.LangEntity;

public interface IConfigRepository {

    ConfigEntity getConfig();
    LangEntity getLang();

    boolean reload();

}
