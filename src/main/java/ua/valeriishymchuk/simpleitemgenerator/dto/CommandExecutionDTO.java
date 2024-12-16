package ua.valeriishymchuk.simpleitemgenerator.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class CommandExecutionDTO {

    boolean executeAsConsole;
    String command;

    @Override
    public String toString() {
        return command;
    }
}
