package ua.valeriishymchuk.simpleitemgenerator.entity.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ConfigLoadResultEntity {

    ItemLoadResultEntity itemLoad;
    List<InvalidConfigurationException> exceptions;

}
