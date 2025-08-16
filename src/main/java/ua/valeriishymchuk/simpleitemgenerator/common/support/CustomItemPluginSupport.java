package ua.valeriishymchuk.simpleitemgenerator.common.support;

import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ItemRepository;

import java.util.List;

public interface CustomItemPluginSupport {

    List<ItemRepository.ItemPatch> getItemPatches(String key);

}
