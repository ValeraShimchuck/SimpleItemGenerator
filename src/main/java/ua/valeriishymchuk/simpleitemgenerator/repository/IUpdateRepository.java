package ua.valeriishymchuk.simpleitemgenerator.repository;

import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.concurrent.CompletableFuture;

public interface IUpdateRepository {

    CompletableFuture<SemanticVersion> getLatestPluginVersion();

}
