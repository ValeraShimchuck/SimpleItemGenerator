package ua.valeriishymchuk.simpleitemgenerator.entity.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ItemLoadResultEntity {

    Map<String, CustomItemEntity> validItems;
    Map<String, InvalidConfigurationException> invalidItems;

    public boolean isSuccess() {
        return invalidItems.isEmpty();
    }

}
