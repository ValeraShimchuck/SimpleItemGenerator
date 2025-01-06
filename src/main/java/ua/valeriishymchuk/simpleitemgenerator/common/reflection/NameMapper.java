package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NameMapper {

    Map<SemanticVersion, String> namePerVersion;

    public Option<String> get(SemanticVersion version) {
        return Option.ofOptional(namePerVersion.entrySet().stream()
                .filter(entry -> version.isAtLeast(entry.getKey()))
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue));
    }

    public Option<String> get() {
        return get(SemanticVersion.CURRENT_MINECRAFT);
    }

}
