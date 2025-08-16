package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import ua.valeriishymchuk.libs.net.kyori.adventure.key.Key;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EnchantmentWrapper {

    Key key;

    @UsesMinecraft
    public Enchantment toBukkit() {
        return Objects.requireNonNull(Enchantment.getByKey(KyoriHelper.asMinecraftKey(key)));
    }

    public static Option<EnchantmentWrapper> of(@NonNull Key enchantment, Predicate<Key> validator) {
        if (!validator.test(enchantment)) return Option.none();
        return Option.some(new EnchantmentWrapper(enchantment));
    }

    @UsesMinecraft
    public static Option<EnchantmentWrapper> of(@NonNull Key enchantment) {
        return of(
                enchantment,
                s -> Arrays.stream(Enchantment.values())
                        .map(Enchantment::getKey)
                        .anyMatch(key -> key.equals(KyoriHelper.convert(s)))
        );
    }

    @UsesMinecraft
    public static Option<EnchantmentWrapper> of(@NonNull Enchantment material) {
        return of(KyoriHelper.convert(material.getKey()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EnchantmentWrapper that = (EnchantmentWrapper) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}
