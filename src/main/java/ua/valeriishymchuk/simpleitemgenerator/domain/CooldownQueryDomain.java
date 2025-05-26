package ua.valeriishymchuk.simpleitemgenerator.domain;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class CooldownQueryDomain {
    boolean isOnCooldown;
    boolean isFreeze;
    @Getter(AccessLevel.NONE)
    @Nullable
    Long remainingCooldownTime;
    @Getter(AccessLevel.NONE)
    @Nullable
    Long lastUsage;

    public Option<Long> getRemainingCooldownTime() {
        return Option.of(remainingCooldownTime);
    }

    public Option<Long> getLastUsage() {
        return Option.of(lastUsage);
    }
}
