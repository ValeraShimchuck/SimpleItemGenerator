package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.block.Block;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.BlockFaceWrapper;

import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class RayTraceBlockResult extends RayTraceResult {

    Block hitBlock;
    BlockFaceWrapper side;

    public RayTraceBlockResult(@NonNull Block hitBlock, @NonNull Location hitLocation, BlockFaceWrapper side) {
        super(Objects.requireNonNull(hitLocation));
        this.hitBlock = Objects.requireNonNull(hitBlock);
        this.side = side;
    }


    public @NonNull Location getHitLocation() {
        return hitLocation;
    }
}
