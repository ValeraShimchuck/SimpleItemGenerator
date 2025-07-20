package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;

import java.util.Collections;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@ToString
@With
public class ItemUsageResultDTO {

    @Nullable
    WrappedComponent message;
    @Getter
    List<CommandExecutionDTO> commands;
    @Getter
    boolean shouldCancel;
    @Getter
    UsageEntity.Consume consume;
    @Getter
    PipelineDebug pipelineDebug;

    public Option<WrappedComponent> getMessage() {
        return Option.of(message);
    }

    public static final ItemUsageResultDTO EMPTY = new ItemUsageResultDTO(
            null,
            Collections.emptyList(),
            false,
            UsageEntity.Consume.NONE,
            PipelineDebug.root("EMPTY")
    );

    public static final ItemUsageResultDTO CANCELLED = EMPTY.withShouldCancel(true)
            .withPipelineDebug(PipelineDebug.root("CANCELED"));

}
