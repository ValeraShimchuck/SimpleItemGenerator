package ua.valeriishymchuk.simpleitemgenerator.repository;

import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;

public interface IConfigRepository {

    MainConfigEntity getConfig();
    boolean doesMainConfigExist();
    LangEntity getLang();

    boolean reload();

}
