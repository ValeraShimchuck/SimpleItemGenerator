package ua.valeriishymchuk.itemgenerator.entity;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@With
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
    public static class Command {
        boolean executeAsConsole;
        String command;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
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
            return getSide().map(side -> side == clickType.side).getOrElse(true) &&
                    getAt().map(at -> at == clickType.at).getOrElse(true);
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
