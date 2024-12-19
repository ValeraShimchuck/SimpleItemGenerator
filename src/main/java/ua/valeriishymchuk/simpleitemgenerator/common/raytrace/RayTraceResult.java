package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class RayTraceResult implements IRayTraceResult {

    public static final RayTraceResult MISS = new RayTraceResult(null);

    @Nullable
    Location hitLocation;

    public Option<Location> getHitLocationOpt() { return Option.of(hitLocation); }

}
