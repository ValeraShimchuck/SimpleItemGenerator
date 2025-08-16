package ua.valeriishymchuk.simpleitemgenerator.common.registry;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.ItemMaterialWrapper;

import java.util.Set;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemMaterialRegistry implements WrappedRegistry<ItemMaterialWrapper> {

    @Getter
    Set<String> possibleValues;
    Function<String, Integer> maxDurabilityGetter;


    public int getMaxDurability(String material) {
        ensureValueExistence(material);
        return maxDurabilityGetter.apply(material);
    }

    public int getMaxDurability(ItemMaterialWrapper wrapper) {
        return getMaxDurability(wrapper.getMaterial());
    }

    @Override
    public Option<ItemMaterialWrapper> wrapSafely(String type) {
        if (!possibleValues.contains(type)) return Option.none();
        return Option.of(wrap(type));
    }

    public ItemMaterialWrapper wrap(String material) {
        ensureValueExistence(material);
        return ItemMaterialWrapper.of(material, possibleValues::contains).get();
    }

}
