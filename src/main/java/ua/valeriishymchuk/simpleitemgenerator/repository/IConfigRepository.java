package ua.valeriishymchuk.simpleitemgenerator.repository;

import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;

public interface IConfigRepository {

    ConfigEntity getConfig();
    LangEntity getLang();

    boolean reload();

}
