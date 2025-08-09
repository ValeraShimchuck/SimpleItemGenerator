package ua.valeriishymchuk.simpleitemgenerator.common.bridge;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesBukkit;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.BlockFaceWrapper;

@UsesBukkit
public class BukkitBridge {

    @Contract("null -> null")
    public static BlockFace bridge(@Nullable BlockFaceWrapper wrapper) {
        if (wrapper == null) return null;
        return BlockFace.valueOf(wrapper.name());
    }

    @Contract("null -> null")
    public static BlockFaceWrapper bridge(@Nullable BlockFace blockFace) {
        if (blockFace == null) return null;
        return BlockFaceWrapper.valueOf(blockFace.name());
    }

}
