package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public abstract class SlotPredicate {

    boolean negate;

    public boolean matches(Input input) {
        return _matches(input) ^ negate;
    }

    protected abstract boolean _matches(Input input);


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

    public static Input input(int clickedSlot, @Nullable EquipmentSlot equipmentSlot) {
        return new Input(clickedSlot, equipmentSlot);
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class Input {
        int clickedSlot;
        @Getter(AccessLevel.NONE)
        @Nullable
        EquipmentSlot equipmentSlot;

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
        protected boolean _matches(Input input) {
            return predicate.matches(input);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class AnySlotPredicate extends SlotPredicate {

        private static final AnySlotPredicate INSTANCE = new AnySlotPredicate();

        private AnySlotPredicate() {
            super(false);
        }
        @Override
        protected boolean _matches(Input input) {
            return true;
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
        public boolean _matches(Input input) {
            return input.clickedSlot == slot;
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
        public boolean _matches(Input input) {
            return (input.clickedSlot >= start && input.clickedSlot <= end);
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
        public boolean _matches(Input input) {
            return input.getEquipmentSlot().map(slot -> slot == this.slot).getOrElse(false);
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
        public boolean _matches(Input input) {
            boolean anyMatch = false;
            boolean anyNonNegateMatch = false;
            for (SlotPredicate predicate : predicates) {
                if (predicate.isNegate() && !predicate.matches(input)) return false;
                if (!predicate.isNegate()) {
                    if (predicate.matches(input)) anyMatch = true;
                    anyNonNegateMatch = true;
                }
            }
            if (!anyMatch && !anyNonNegateMatch) return true;
            return anyMatch;
        }
    }

}
