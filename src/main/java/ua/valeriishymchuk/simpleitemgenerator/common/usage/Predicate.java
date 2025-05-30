package ua.valeriishymchuk.simpleitemgenerator.common.usage;

import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;
import ua.valeriishymchuk.simpleitemgenerator.common.support.WorldGuardSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@ToString
@With
public class Predicate {

    private static final boolean DEBUG = true;
    private static final boolean SLOT_DEBUG = false;

    @Nullable
    ClickButton button;
    @Nullable
    ClickAt at;
    @Nullable Map<String, Boolean> stateFlags;
    @Nullable Amount amount;
    @Nullable List<String> permissions;
    @Nullable List<Integer> timeTick;
    @Nullable SlotPredicate slots;
    @Nullable SlotPredicate prevSlots;



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return button == predicate.button && at == predicate.at && Objects.equals(stateFlags, predicate.stateFlags) && Objects.equals(amount, predicate.amount) && Objects.equals(permissions, predicate.permissions) && Objects.equals(timeTick, predicate.timeTick) && Objects.equals(slots, predicate.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(button, at, stateFlags, amount, permissions, timeTick, slots);
    }

    public Option<ClickButton> getButton() {
        return Option.of(button);
    }

    public Option<SlotPredicate> getSlots() { return Option.of(slots); }
    public Option<SlotPredicate> getPrevSlots() { return Option.of(prevSlots); }

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
        if (getTimeTick().isEmpty() && input.getTrigger() == PredicateInput.Trigger.TICK) return false;
        return isEmptyOrAnyMatch(getTimeTick(), tick -> input.getCurrentTick() % tick == 0);
    }

    private static void printDebug(String msg) {
        if (DEBUG) System.out.println(msg);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class TestResult {
        boolean testResult;
        List<PipelineDebug> debugs;
    }

    public TestResult test(PredicateInput input) {
        Location location = input.getLocation().getOrElse(input.getPlayer().getLocation());
        boolean tickPass = getTimeTick().isEmpty() && input.getTrigger() == PredicateInput.Trigger.TICK;
        if (tickPass) return new TestResult(
                false,
                Collections.singletonList(
                        PipelineDebug.root("Tick predicate is empty and its triggered by tick, skip...")
                )
        );
        List<PipelineDebug> pipelineDebugs = new ArrayList<>();
        boolean buttonMatch = getButton().map(side1 -> input.getButton()
                .map(button -> button == side1).getOrElse(false)).getOrElse(true);
        if (getButton().isDefined())
            pipelineDebugs.add(PipelineDebug.root("Button match - " + buttonMatch));
        boolean timeMatch = checkTime(input);
        if (!getTimeTick().isEmpty())
            pipelineDebugs.add(PipelineDebug.root("Time match - " + timeMatch));
        boolean matchSlots = getSlots().map(slots1 -> {
            if (!input.getSlot().isOccupied()) return false;
            SlotPredicate.PredicateResult result = slots1.matches(input.getSlot());
            PipelineDebug slotMatchPipelineDebug = PipelineDebug.root(
                    "Slots match - " + result.isResult()
            );
            pipelineDebugs.add(slotMatchPipelineDebug.appendAllAndReturnSelf(result.getPipelineDebugs()));
            return result.isResult();
        }).getOrElse(true);

        boolean matchPrevSlots = getPrevSlots().map(slots1 -> {
            if (input.getSlot().isOccupied()) return false;
            SlotPredicate.PredicateResult result = slots1.matches(input.getSlot());
            PipelineDebug slotMatchPipelineDebug = PipelineDebug.root(
                    "Previous slots match - " + result.isResult()
            );
            pipelineDebugs.add(slotMatchPipelineDebug.appendAllAndReturnSelf(result.getPipelineDebugs()));
            return result.isResult();
        }).getOrElse(true);
        boolean matchAt = getAt().map(at1 -> at1 == input.getClickAt()).getOrElse(true);
        if (getAt().isDefined())
            pipelineDebugs.add(PipelineDebug.root("Match at - " + matchAt));
        boolean matchAmount = getAmount().map(amount1 -> amount1.test(input.getAmount())).getOrElse(true);
        if (getAmount().isDefined())
            pipelineDebugs.add(PipelineDebug.root("Amount match - " + matchAmount));
        boolean matchPermission = getPermissions().stream().allMatch(s -> input.getPlayer().hasPermission(s));
        if (!getPermissions().isEmpty())
            pipelineDebugs.add(PipelineDebug.root("Permission match - " + matchPermission));
        boolean matchWg = HashMap.ofAll(getStateFlags()).forAll(t -> {
            return WorldGuardSupport.checkState(input.getPlayer(), location, t._1)
                    .get() == t._2;
        });
        if (!getStateFlags().isEmpty())
            pipelineDebugs.add(PipelineDebug.root("WG match - " + matchWg));
        boolean testResult = buttonMatch &&
                timeMatch &&
                matchSlots &&
                matchAt &&
                matchAmount &&
                matchPermission &&
                matchWg &&
                matchPrevSlots;
        return new TestResult(
                testResult,
                pipelineDebugs
        );
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

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Amount amount = (Amount) o;
            return Objects.equals(totalAmount, amount.totalAmount) && Objects.equals(stackAmount, amount.stackAmount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(totalAmount, stackAmount);
        }
    }

}
