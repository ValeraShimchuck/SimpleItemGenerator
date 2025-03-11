package ua.valeriishymchuk.simpleitemgenerator.common.usage;

import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.support.WorldGuardSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@ToString
@With
public class Predicate {

    @Nullable
    ClickButton button;
    @Nullable
    ClickAt at;
    @Nullable Map<String, Boolean> stateFlags;
    @Nullable Amount amount;
    @Nullable List<String> permissions;
    @Nullable List<Integer> timeTick;
    @Nullable List<Integer> slots;


    public Option<ClickButton> getButton() {
        return Option.of(button);
    }

    public List<Integer> getSlots() { return Option.of(slots).getOrElse(Collections.emptyList()); }

    public Option<ClickAt> getAt() {
        return Option.of(at);
    }

    public Map<String, Boolean> getStateFlags() {
        return Option.of(stateFlags).getOrElse(Collections.EMPTY_MAP);
    }

    public List<String> getPermissions() { return Option.of(permissions).getOrElse(Collections.emptyList()); }

    public Option<Amount> getAmount() {
        return Option.of(amount);
    }

    public List<Integer> getTimeTick() {
        return Option.of(timeTick).getOrElse(Collections.emptyList());
    }

    private <T> boolean isEmptyOrAnyMatch(List<T> list, java.util.function.Predicate<T> predicate) {
        if (list.isEmpty()) return true;
        return list.stream().anyMatch(predicate);
    }

    private boolean checkTime(PredicateInput input) {
        if (getTimeTick().isEmpty() && input.getButton().isEmpty()) return false;
        return isEmptyOrAnyMatch(getTimeTick(), tick -> input.getCurrentTick() % tick == 0);
    }

    public boolean test(PredicateInput input) {
        Location location = input.getLocation().getOrElse(input.getPlayer().getLocation());
        boolean tickPass = !getTimeTick().isEmpty() || input.getButton().isDefined();
        if (!tickPass) return false;
        return getButton().map(side1 -> input.getButton()
                        .map(button -> button == side1).getOrElse(false)).getOrElse(true) &&
                //isEmptyOrAnyMatch(getTimeTick(), tick -> input.getCurrentTick() % tick == 0) &&
                checkTime(input) &&
                isEmptyOrAnyMatch(getSlots(), slot -> slot == input.getSlot() || (slot == -1 && input.getPlayer().getInventory().getHeldItemSlot() == input.getSlot())) &&
                getAt().map(at1 -> at1 == input.getClickAt()).getOrElse(true) &&
                getAmount().map(amount1 -> amount1.test(input.getAmount())).getOrElse(true) &&
                getPermissions().stream().allMatch(s -> input.getPlayer().hasPermission(s)) &&
                HashMap.ofAll(getStateFlags()).forAll(t -> {
                    return WorldGuardSupport.checkState(input.getPlayer(), location, t._1)
                            .get() == t._2;
                });
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public static class Amount {
        Integer totalAmount;
        Integer stackAmount;

        public Option<Integer> getTotalAmount() {
            return Option.of(totalAmount);
        }

        public Option<Integer> getStackAmount() {
            return Option.of(stackAmount);
        }

        public boolean test(PredicateInput.Amount input) {
            return getStackAmount().map(stackAmount1 -> stackAmount1 <= input.getStackAmount()).getOrElse(true) &&
                    getTotalAmount().map(totalAmount1 -> totalAmount1 <= input.getTotalAmount()).getOrElse(true);
        }

    }

}
