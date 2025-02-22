package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class WithdrawItemDTO {

    Component senderMessage;
    @Getter(AccessLevel.NONE)
    @Nullable Component receiverMessage;
    boolean success;

    public Option<Component> getReceiverMessage() {
        return Option.of(receiverMessage);
    }
}
