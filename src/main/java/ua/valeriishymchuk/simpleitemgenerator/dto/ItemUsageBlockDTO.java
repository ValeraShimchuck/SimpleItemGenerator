package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ItemUsageBlockDTO {
    Player player;
    Action action;
    ItemStack item;
    @Getter(AccessLevel.NONE)
    @Nullable Block clickedBlock;
    @Getter(AccessLevel.NONE)
    @Nullable BlockFace clickedFace;
    int slot;
    long currentTick;

    public Option<Block> getClickedBlock() {
        return Option.of(clickedBlock);
    }

    public Option<BlockFace> getClickedFace() {
        return Option.of(clickedFace);
    }
}
