package ua.valeriishymchuk.simpleitemgenerator.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@ToString
public class SlotChangeDTO {
    boolean isOccupied;
    SlotPredicate.Input slot;
    long tick;
    ItemCopy itemStack;
}
