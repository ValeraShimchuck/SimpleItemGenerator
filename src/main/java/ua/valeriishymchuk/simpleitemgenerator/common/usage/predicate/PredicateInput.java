package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
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

    public Option<Location> getLocation() {
        return Option.of(location);
    }

    public Option<ClickButton> getButton() {
        return Option.of(clickButton);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @Getter
    @RequiredArgsConstructor
    public static class Amount {
        int totalAmount;
        int stackAmount;
    }

}
