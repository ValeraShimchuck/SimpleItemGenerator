package ua.valeriishymchuk.simpleitemgenerator.common.tick;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TickerTime {

    BukkitTaskScheduler scheduler;
    @Getter
    @NonFinal long tick = 0;
    List<Consumer<Long>> tasks = new ArrayList<>();

    public void addTask(Consumer<Long> task) {
        tasks.add(task);
    }

    public void start() {
        scheduler.runTaskTimer(() -> {
            tick++;
            tasks.forEach(t -> t.accept(tick));
        }, 1, 1);
    }
}
