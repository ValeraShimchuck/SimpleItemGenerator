package ua.valeriishymchuk.simpleitemgenerator.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ItemUsageEntityDTO {
    Player player;
    boolean isRightClicked;
    Entity clicked;
    ItemStack item;
    SlotPredicate.Input slot;
    long currentTick;
}
