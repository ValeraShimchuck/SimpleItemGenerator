package ua.valeriishymchuk.simpleitemgenerator.service;

import io.vavr.control.Option;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface IInfoService {

    Option<Component> getMessage(Player player);
    CompletableFuture<Option<Component>> getNewUpdateMessage(Player player);

}
