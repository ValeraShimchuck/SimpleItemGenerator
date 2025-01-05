package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@ToString
@With
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
