package ua.valeriishymchuk.itemgenerator.entity;

import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@With
@ToString
public class UsageEntity {

    public static UsageEntity EMPTY = new UsageEntity(
            Collections.emptyList(),
            0,
            0,
            false,
            Collections.emptyList(),
            Collections.emptyList()
    );

    public static UsageEntity DEFAULT = EMPTY.withCancel(true);

    List<ClickType> predicates;
    long cooldownMillis;
    long cooldownFreezeTimeMillis;
    boolean cancel;
    List<Command> onCooldown;
    List<Command> commands;

    public boolean accepts(ClickType clickType) {
        if (predicates.isEmpty()) return true;
        return predicates.stream().anyMatch(t -> t.predicate(clickType));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    @With
    public static class Command {
        boolean executeAsConsole;
        String command;

        public Command replace(UnaryOperator<String> replacer) {
            return new Command(executeAsConsole, replacer.apply(command));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @ToString
    public static class ClickType {

        @Nullable
        UsageEntity.ClickButton side;
        @Nullable
        ClickAt at;

        public Option<ClickButton> getSide() {
            return Option.of(side);
        }

        public Option<ClickAt> getAt() {
            return Option.of(at);
        }

        public boolean predicate(ClickType clickType) {
            return getSide().map(side1 -> side1 == clickType.side).getOrElse(true) &&
                    getAt().map(at1 -> at1 == clickType.at).getOrElse(true);
        }

    }

    public enum ClickButton {
        RIGHT,
        LEFT,
        DROP;

        public ClickType asType() {
            return new ClickType(this, null);
        }

    }

    public enum ClickAt {
        AIR,
        PLAYER,
        ENTITY,
        BLOCK;
        public ClickType asType() {
            return new ClickType(null, this);
        }
    }

}
