package ua.valeriishymchuk.simpleitemgenerator.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.Predicate;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;

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
            Consume.NONE,
            Collections.emptyList(),
            Collections.emptyList(),
            CooldownType.PER_ITEM
    );

    public static UsageEntity DEFAULT = EMPTY.withCancel(true);

    List<Predicate> predicates;
    long cooldownMillis;
    long cooldownFreezeTimeMillis;
    boolean cancel;
    Consume consume;
    List<Command> onCooldown;
    List<Command> commands;
    CooldownType cooldownType;

    public boolean accepts(PredicateInput input) {
        if (predicates.isEmpty() && input.getButton().isDefined()) return true;
        return predicates.stream().anyMatch(t -> t.test(input));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    @With
    public static class Consume {

        public static Consume NONE = new Consume(ConsumeType.NONE, 0);

        ConsumeType consumeType;
        int amount;

        public boolean isNone() {
            return consumeType == ConsumeType.NONE;
        }

        public boolean isAmount() {
            return consumeType == ConsumeType.AMOUNT;
        }

    }

    public enum ConsumeType {
        AMOUNT,
        NONE,
        STACK,
        ALL
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

}
