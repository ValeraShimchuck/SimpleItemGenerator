package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GiveItemDTO {

    @Getter
    Component message;
    @Getter
    Component dropMessage;
    @Nullable
    ItemStack itemStack;

    public Option<ItemStack> getItemStack() {
        return Option.of(itemStack);
    }
}
