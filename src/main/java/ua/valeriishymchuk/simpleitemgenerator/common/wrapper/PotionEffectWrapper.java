package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.potion.PotionEffect;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PotionEffectWrapper {

    PotionEffectTypeWrapper effectType;
    int duration;
    int amplifier;
    boolean ambient;
    boolean particles;
    boolean icon;

    @UsesMinecraft
    public PotionEffect toBukkit() {
        return new PotionEffect(
                effectType.toBukkit(),
                duration,
                amplifier,
                ambient,
                particles,
                icon
        );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PotionEffectWrapper that = (PotionEffectWrapper) o;
        return duration == that.duration && amplifier == that.amplifier && ambient == that.ambient && particles == that.particles && icon == that.icon && Objects.equals(effectType, that.effectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectType, duration, amplifier, ambient, particles, icon);
    }
}
