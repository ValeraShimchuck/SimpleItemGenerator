package ua.valeriishymchuk.simpleitemgenerator.common.registry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.EnchantmentWrapper;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.ItemFlagWrapper;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.PotionEffectTypeWrapper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class WrappedRegistries {

    ItemMaterialRegistry itemMaterialRegistry;
    WrappedRegistry<PotionEffectTypeWrapper> potionEffectTypesRegistry;
    WrappedRegistry<ItemFlagWrapper> itemFlagWrapperRegistry;
    WrappedRegistry<EnchantmentWrapper> enchantmentsWrapperRegistry;

}
