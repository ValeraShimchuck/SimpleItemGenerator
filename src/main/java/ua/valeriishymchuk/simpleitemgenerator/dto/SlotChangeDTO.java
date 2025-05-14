package ua.valeriishymchuk.simpleitemgenerator.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class SlotChangeDTO {
    boolean isOccupied;
    int slot;
    ItemCopy itemStack;
}
