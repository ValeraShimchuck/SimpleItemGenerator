package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemMaterialWrapper {

    @Getter
    String material;

    @UsesMinecraft
    public Material toBukkit() {
        return Material.valueOf(material);
    }

    public static Option<ItemMaterialWrapper> of(@NonNull String material, Predicate<String> validator) {
        if (!validator.test(material)) return Option.none();
        return Option.some(new ItemMaterialWrapper(material));
    }

    @UsesMinecraft
    public static Option<ItemMaterialWrapper> of(@NonNull String material) {
        return of(
                material,
                s -> Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .map(Material::name)
                        .filter(m -> !m.startsWith("LEGACY_"))
                        .anyMatch(rawMaterial -> rawMaterial.equals(s))
        );
    }

    @UsesMinecraft
    public static Option<ItemMaterialWrapper> of(@NonNull Material material) {
        return of(material.name());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ItemMaterialWrapper that = (ItemMaterialWrapper) o;
        return Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(material);
    }
}
