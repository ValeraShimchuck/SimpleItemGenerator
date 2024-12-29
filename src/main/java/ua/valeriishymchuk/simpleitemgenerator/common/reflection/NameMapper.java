package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.version.MinecraftVersion;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NameMapper {

    Map<MinecraftVersion, String> namePerVersion;

    public Option<String> get(MinecraftVersion version) {
        return Option.ofOptional(namePerVersion.entrySet().stream()
                .filter(entry -> version.isAtLeast(entry.getKey()))
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue));
    }

    public Option<String> get() {
        return get(MinecraftVersion.CURRENT);
    }

}
