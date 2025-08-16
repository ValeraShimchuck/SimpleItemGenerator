package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.potion.PotionEffectType;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PotionEffectTypeWrapper {

    @Getter
    String type;

    @UsesMinecraft
    public PotionEffectType toBukkit() {
        return PotionEffectType.getByName(type);
    }

    public static Option<PotionEffectTypeWrapper> of(@NonNull String type, Predicate<String> validator) {
        if (!validator.test(type)) return Option.none();
        return Option.some(new PotionEffectTypeWrapper(type));
    }

    @UsesMinecraft
    public static Option<PotionEffectTypeWrapper> of(@NonNull String type) {
        return of(
                type,
                s -> Arrays.stream(PotionEffectType.values())
                        .map(PotionEffectType::getName)
                        .anyMatch(rawType -> rawType.equals(s))
        );
    }

    @UsesMinecraft
    public static Option<PotionEffectTypeWrapper> of(@NonNull PotionEffectType type) {
        return of(type.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PotionEffectTypeWrapper that = (PotionEffectTypeWrapper) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

}
