package ua.valeriishymchuk.simpleitemgenerator.common.version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum SigFeatureTag {
    ENHANCED_SLOT_PREDICATE(false);

    boolean enabledByDefault;

}
