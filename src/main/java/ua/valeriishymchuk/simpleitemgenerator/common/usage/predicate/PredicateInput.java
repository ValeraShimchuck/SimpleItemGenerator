package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@ToString
public class PredicateInput {

    Player player;
    @Getter(AccessLevel.NONE)
    @Nullable Location location;
    @Getter(AccessLevel.NONE)
    @Nullable ClickButton clickButton;
    ClickAt clickAt;
    Amount amount;
    long currentTick;
    SlotPredicate.Input slot;
    Trigger trigger;

    public Option<Location> getLocation() {
        return Option.of(location);
    }

    public Option<ClickButton> getButton() {
        return Option.of(clickButton);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @Getter
    @RequiredArgsConstructor
    @ToString
    public static class Amount {
        int totalAmount;
        int stackAmount;
    }

    public enum Trigger {
        TICK,
        INVENTORY_CLICK,
        ENTITY_CLICK,
        WORLD_CLICK,
        DROP_ITEM
    }

}
