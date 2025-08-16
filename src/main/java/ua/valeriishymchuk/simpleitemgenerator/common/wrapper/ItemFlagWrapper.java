package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFlagWrapper {

    String itemFlag;

    @UsesMinecraft
    public ItemFlag toBukkit() {
        return ItemFlag.valueOf(itemFlag);
    }

    public static Option<ItemFlagWrapper> of(@NonNull String itemFlag, Predicate<String> validator) {
        if (!validator.test(itemFlag)) return Option.none();
        return Option.some(new ItemFlagWrapper(itemFlag));
    }

    @UsesMinecraft
    public static Option<ItemFlagWrapper> of(@NonNull String itemFlag) {
        return of(
                itemFlag,
                s -> Arrays.stream(ItemFlag.values())
                        .map(ItemFlag::name)
                        .anyMatch(rawItemFlag -> rawItemFlag.equals(s))
        );
    }

    @UsesMinecraft
    public static Option<ItemFlagWrapper> of(@NonNull ItemFlag itemFlag) {
        return of(itemFlag.name());
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ItemFlagWrapper that = (ItemFlagWrapper) o;
        return Objects.equals(itemFlag, that.itemFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(itemFlag);
    }
}
