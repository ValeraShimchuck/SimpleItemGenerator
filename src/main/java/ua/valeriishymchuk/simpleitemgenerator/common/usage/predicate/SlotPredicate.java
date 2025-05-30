package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public abstract class SlotPredicate {

    private static final boolean DEBUG = false;

    boolean negate;

    private static void printDebug(String msg) {
        if (DEBUG) System.out.println(msg);
    }

    public static SlotPredicate slot(boolean negate, int slot) {
        return new StaticSlotPredicate(negate, slot);
    }

    public static SlotPredicate slot(int slot) {
        return slot(false, slot);
    }

    public static SlotPredicate range(boolean negate, int slot1, int slot2) {
        if (slot1 == slot2) return slot(negate, slot1);
        return new SlotRangePredicate(negate, slot1, slot2);
    }

    public static SlotPredicate range(int slot1, int slot2) {
        return range(false, slot1, slot2);
    }

    public static SlotPredicate equipment(boolean negate, EquipmentSlot slot) {
        return new EquipmentSlotPredicate(negate, slot);
    }

    public static SlotPredicate equipment(EquipmentSlot slot) {
        return equipment(false, slot);
    }

    public static SlotPredicate union(boolean negate, Collection<SlotPredicate> predicates) {
        if (predicates.size() == 1) return predicates.iterator().next();
        return new UnionSlotPredicate(negate, predicates);
    }

    public static SlotPredicate union(Collection<SlotPredicate> predicates) {
        return union(false, predicates);
    }

    public static SlotPredicate union(boolean negate, SlotPredicate... predicates) {
        return union(negate, io.vavr.collection.List.of(predicates).toJavaList());
    }

    public static SlotPredicate union(SlotPredicate... predicates) {
        return union(false, predicates);
    }

    public static SlotPredicate any() {
        return AnySlotPredicate.INSTANCE;
    }

    public static SlotPredicate negate(SlotPredicate predicate) {
        return new WrapperPredicate(!predicate.isNegate(), predicate);
    }

    public static Input input(int clickedSlot, @Nullable EquipmentSlot equipmentSlot, boolean isOccupied) {
        return new Input(clickedSlot, equipmentSlot, isOccupied);
    }

    public PredicateResult matches(Input input) {
        PredicateResult basicMatch = _matches(input);
        List<PipelineDebug> debugs = new ArrayList<>();
        if (negate) {
            debugs.add(
                    PipelineDebug.root("Negating result, was " + basicMatch.result + " became " + (!basicMatch.result))
                            .appendAllAndReturnSelf(basicMatch.pipelineDebugs)
            );
        } else {
            debugs.addAll(basicMatch.pipelineDebugs);
        }
        return new PredicateResult(
                basicMatch.result ^ negate,
                debugs
        );
    }

    protected abstract PredicateResult _matches(Input input);

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class PredicateResult {
        boolean result;
        List<PipelineDebug> pipelineDebugs;
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class Input {
        int clickedSlot;
        @Getter(AccessLevel.NONE)
        @Nullable
        EquipmentSlot equipmentSlot;
        boolean isOccupied;

        public Option<EquipmentSlot> getEquipmentSlot() {
            return Option.of(equipmentSlot);
        }
    }

    private static class WrapperPredicate extends SlotPredicate {

        SlotPredicate predicate;

        private WrapperPredicate(boolean negate, SlotPredicate predicate) {
            super(negate);
            this.predicate = predicate;
        }

        @Override
        protected PredicateResult _matches(Input input) {
            PredicateResult result = predicate.matches(input);
            return new PredicateResult(
                    result.result,
                    result.pipelineDebugs
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class AnySlotPredicate extends SlotPredicate {

        private static final AnySlotPredicate INSTANCE = new AnySlotPredicate();

        private AnySlotPredicate() {
            super(false);
        }

        @Override
        protected PredicateResult _matches(Input input) {
            return new PredicateResult(
                    true,
                    Collections.singletonList(
                            PipelineDebug.root("Any slot - true")
                    )
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class StaticSlotPredicate extends SlotPredicate {

        int slot;

        private StaticSlotPredicate(boolean negate, int slot) {
            super(negate);
            this.slot = slot;
        }

        @Override
        public PredicateResult _matches(Input input) {
            return new PredicateResult(
                    input.clickedSlot == slot,
                    Collections.singletonList(
                            PipelineDebug.root("Static slot, clicked slot " + input.clickedSlot
                                    + " configured slot " + slot + " result: " + (input.clickedSlot == slot)
                            )
                    )
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class SlotRangePredicate extends SlotPredicate {

        int start;
        int end;

        private SlotRangePredicate(boolean negate, int slot1, int slot2) {
            super(negate);
            this.start = Math.min(slot1, slot2);
            this.end = Math.max(slot1, slot2);
        }

        @Override
        public PredicateResult _matches(Input input) {
            boolean result = input.clickedSlot >= start && input.clickedSlot <= end;
            return new PredicateResult (
                    result,
                    Collections.singletonList(
                            PipelineDebug.root(
                                    "Slot range, "
                                            + start + " <= "
                                            + input.clickedSlot + " <= "
                                            + end + ", result: " + result
                            )
                    )
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class EquipmentSlotPredicate extends SlotPredicate {

        EquipmentSlot slot;

        private EquipmentSlotPredicate(boolean negate, EquipmentSlot slot) {
            super(negate);
            this.slot = slot;
        }

        @Override
        public PredicateResult _matches(Input input) {
            boolean result = input.getEquipmentSlot().map(slot -> slot == this.slot)
                    .getOrElse(false);
            return new PredicateResult(
                    result,
                    Collections.singletonList(
                            PipelineDebug.root(
                                    "Equipment slot predicate, input " + input.getEquipmentSlot()
                                            .map(Enum::name)
                                            .getOrElse("null") + ", configured " + slot + " result " + result
                            )
                    )
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class UnionSlotPredicate extends SlotPredicate {

        List<SlotPredicate> predicates;

        private UnionSlotPredicate(boolean negate, Collection<SlotPredicate> predicates) {
            super(negate);
            this.predicates = new ArrayList<>(predicates);
        }

        @Override
        public PredicateResult _matches(Input input) {
            boolean anyMatch = false;
            boolean anyNonNegateMatch = false;
            List<PipelineDebug> pipelineDebugs = new ArrayList<>();
            for (SlotPredicate predicate : predicates) {
                int indexOf = predicates.indexOf(predicate);
                PredicateResult predicateResult = predicate.matches(input);
                if (predicate.isNegate() && !predicateResult.result) return new PredicateResult(
                        false,
                        Collections.singletonList(
                                PipelineDebug.root(
                                        "Predicate is negated and its result is not true - " + indexOf
                                ).appendAllAndReturnSelf(predicateResult.pipelineDebugs)
                        )
                );
                if (!predicate.isNegate()) {
                    if (predicateResult.result) anyMatch = true;
                    anyNonNegateMatch = true;
                    pipelineDebugs.add(PipelineDebug.root(
                            "Found match: " + predicateResult.result + " anyMatch: " + anyMatch
                    ).appendAllAndReturnSelf(predicateResult.getPipelineDebugs()));
                } else {
                    pipelineDebugs.add(PipelineDebug.root(
                            "Negate predicate, skipping it, data so far anyMatch: " + anyMatch
                                    + " nonNegateMatch: " + anyNonNegateMatch
                    ).appendAllAndReturnSelf(predicateResult.getPipelineDebugs()));
                }
            }
            if (!anyMatch && !anyNonNegateMatch) {
                pipelineDebugs.add(PipelineDebug.root(
                        "No match, also there is no negates, so its true"
                ));
                return new PredicateResult(
                        true,
                        pipelineDebugs
                );
            }
            pipelineDebugs.add(PipelineDebug.root(
                    "Unification is done, result " + anyMatch
            ));
            return new PredicateResult(
                    anyMatch,
                    pipelineDebugs
            );
        }
    }

}
