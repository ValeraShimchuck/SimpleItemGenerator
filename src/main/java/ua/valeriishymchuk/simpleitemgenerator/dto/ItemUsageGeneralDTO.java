package ua.valeriishymchuk.simpleitemgenerator.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ItemUsageGeneralDTO {
    Player player;
    ItemStack itemStack;
    long currentTick;
    int slot;
}
