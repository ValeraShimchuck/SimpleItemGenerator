package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class RayTraceEntityResult extends RayTraceResult {

    Entity entity;

    public RayTraceEntityResult(@NonNull  Entity entity, @NonNull Location hitLocation) {
        super(hitLocation);
        this.entity = entity;
    }

    public @NonNull Location getHitLocation() {
        return hitLocation;
    }

}
