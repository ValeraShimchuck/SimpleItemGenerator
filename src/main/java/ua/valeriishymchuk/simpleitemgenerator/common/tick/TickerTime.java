package ua.valeriishymchuk.simpleitemgenerator.common.tick;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TickerTime {

    BukkitTaskScheduler scheduler;
    @Getter
    @NonFinal long tick = 0;

    public void start() {
        scheduler.runTaskTimer(() -> tick++, 1, 1);
    }
}
