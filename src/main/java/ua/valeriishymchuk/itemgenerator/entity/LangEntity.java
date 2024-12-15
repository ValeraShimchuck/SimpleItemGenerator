package ua.valeriishymchuk.itemgenerator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.itemgenerator.common.component.RawComponent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class LangEntity {

    RawComponent giveItemSuccessfully = new RawComponent("Custom item %key% was successfully given to %player%.");
    RawComponent itemDoesntExist = new RawComponent("Custom item %key% doesn't exist.");
    RawComponent reloadSuccessfully = new RawComponent("Configs was successfully reloaded.");
    RawComponent reloadUnsuccessfully = new RawComponent("Configs wasn't reloaded. Check console.");
    RawComponent invalidItem = new RawComponent("This custom item is invalid. Report this case to the server admins. Item key %key%");
    RawComponent senderNotPlayer = new RawComponent("Use argument player to use this command. Or try execute it as a player");


}
