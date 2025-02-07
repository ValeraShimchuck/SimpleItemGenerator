package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class RayTraceBlockResult extends RayTraceResult {

    Block hitBlock;
    BlockFace side;

    public RayTraceBlockResult(@NonNull Block hitBlock, @NonNull Location hitLocation, BlockFace side) {
        super(Objects.requireNonNull(hitLocation));
        this.hitBlock = Objects.requireNonNull(hitBlock);
        this.side = side;
    }


    public @NonNull Location getHitLocation() {
        return hitLocation;
    }
}
