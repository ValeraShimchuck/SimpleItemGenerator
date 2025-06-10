package ua.valeriishymchuk.simpleitemgenerator.entity;

import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.Predicate;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;
import ua.valeriishymchuk.simpleitemgenerator.domain.CooldownQueryDomain;
import ua.valeriishymchuk.simpleitemgenerator.dto.CommandExecutionDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ua.valeriishymchuk.simpleitemgenerator.common.placeholders.PlaceholdersHelper.replacePlayer;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@With
@ToString
public class UsageEntity {

    private static final Pattern TIME_PATTERN = Pattern.compile("%time_(?<timeunit>[a-z])(\\.(?<precision>\\d+)f)?%");


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

    public List<CommandExecutionDTO> prepareCommands(Player player, Map<String, String> placeholders) {
        return this.getCommands().stream()
                .map(command -> command.prepare(player, placeholders))
                .collect(Collectors.toList());
    }



    public ItemUsageResultDTO prepareCooldownCommands(
            CooldownQueryDomain queryDomain,
            Player player,
            Map<String, String> placeholders,
            PipelineDebug pipelineDebug
    ) {
        List<CommandExecutionDTO> commands = this.getOnCooldown().stream()
                .map(it -> it.prepareCooldown(
                        queryDomain.getRemainingCooldownTime().get(),
                        player,
                        placeholders
                ))
                .collect(Collectors.toList());
        return ItemUsageResultDTO.CANCELLED.withCommands(commands)
                .withPipelineDebug(pipelineDebug.appendAndReturnSelf("Preparing cooldown commands"));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class AcceptResult {
        boolean isAccepted;
        List<PipelineDebug> pipelineDebugs;
    }

    public AcceptResult accepts(PredicateInput input) {
        if (predicates.isEmpty()
                && input.getTrigger() != PredicateInput.Trigger.TICK
        ) return new AcceptResult(
                true,
                Collections.singletonList(PipelineDebug.root("Predicates are empty, so its true"))
        ) ;
        //boolean shouldLog = input.getTrigger() == PredicateInput.Trigger.INVENTORY_CLICK && !predicates.isEmpty();
        //if (predicates.isEmpty() && input.getButton().isDefined()) return true;
        List<PipelineDebug> debugs = new ArrayList<>();
        boolean isAccepted = predicates.stream().anyMatch(t -> {
            Predicate.TestResult result = t.test(input);
            debugs.add(PipelineDebug
                    .root("Predicate (" + result.isTestResult() + ") - " + predicates.indexOf(t))
                    .appendAllAndReturnSelf(result.getDebugs())
            );
            return result.isTestResult();
        });
        return new AcceptResult(
                isAccepted,
                debugs
        );
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


        private CommandExecutionDTO prepare( Player player, Map<String, String> placeholders) {
            String rawCommand = replacePlayer(command, player);
            AtomicReference<String> strAtomic = new AtomicReference<>(rawCommand);
            placeholders.forEach((placeholder, value) -> strAtomic.set(strAtomic.get().replace(placeholder, value)));
            return new CommandExecutionDTO(executeAsConsole, strAtomic.get());
        }

        private CommandExecutionDTO prepareCooldown(long milliseconds, Player player, Map<String, String> placeholders) {
            CommandExecutionDTO halfPreparedDto = prepare(player, placeholders);
            String rawCommand = halfPreparedDto.getCommand();
            String finalCommand = RegexUtils.replaceAll(TIME_PATTERN.matcher(rawCommand), matcher -> {
                String timeUnit = matcher.group("timeunit").toLowerCase();
                int precision = Option.of(matcher.group("precision")).map(Integer::parseInt).getOrElse(0);
                double value;
                switch (timeUnit) {
                    case "s":
                        value = milliseconds / 1000.0;
                        break;
                    case "m":
                        value = milliseconds / 1000.0 / 60.0;
                        break;
                    case "h":
                        value = milliseconds / 1000.0 / 60.0 / 60.0;
                        break;
                    case "t":
                        value = milliseconds / 50.0;
                        break;
                    default:
                        throw new IllegalStateException("Unknown time unit: " + timeUnit);
                }
                return String.format(Locale.ROOT, "%." + precision + "f", value);
            });
            return new CommandExecutionDTO(halfPreparedDto.isExecuteAsConsole(), finalCommand);
        }

    }

}
