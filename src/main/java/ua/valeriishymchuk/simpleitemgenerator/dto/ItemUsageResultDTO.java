package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@ToString
public class ItemUsageResultDTO {

    @Nullable
    Component message;
    @Getter
    List<CommandExecutionDTO> commands;
    @Getter
    boolean shouldCancel;

    public Option<Component> getMessage() {
        return Option.of(message);
    }
}
