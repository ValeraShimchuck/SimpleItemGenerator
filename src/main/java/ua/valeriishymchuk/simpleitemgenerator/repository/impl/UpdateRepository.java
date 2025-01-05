package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;
import ua.valeriishymchuk.simpleitemgenerator.repository.IUpdateRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class UpdateRepository implements IUpdateRepository {

    private static final URL API_URL;
    private static final Gson GSON = new Gson();

    static {
        try {
            API_URL = new URL("https://api.github.com/repos/ValeraShimchuck/SimpleItemGenerator/tags");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<SemanticVersion> getLatestPluginVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedReader bufferedReader = getBufferedReader();
                JsonArray result = GSON.fromJson(bufferedReader, JsonArray.class);
                bufferedReader.close();
                return StreamSupport.stream(result.spliterator(), false)
                        .map(je -> je.getAsJsonObject().get("name").getAsString())
                        .map(SemanticVersion::parse)
                        .max(Comparator.naturalOrder())
                        .orElseThrow(() -> new RuntimeException("Can't find any version"));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static @NotNull BufferedReader getBufferedReader() throws IOException {
        URLConnection connection = API_URL.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("Accept", "application/vnd.github+json");
        httpConnection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        httpConnection.setUseCaches(false);
        httpConnection.setDoOutput(true);
        return new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
    }
}
