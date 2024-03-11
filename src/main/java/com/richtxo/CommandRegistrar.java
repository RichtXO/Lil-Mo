package com.richtxo;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandRegistrar {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final RestClient restClient;
    private static final String COMMAND_FOLDER = "commands/";

    public CommandRegistrar(RestClient restClient) {
        this.restClient = restClient;
    }

    protected void registerCmds() throws IOException {
        final JacksonResources mapper = JacksonResources.create();

        final ApplicationService appService = restClient.getApplicationService();
        final long appID = restClient.getApplicationId().block();
        final long guildID = Long.parseLong(System.getenv("TEST_SERVER"));
        List<ApplicationCommandRequest> commands = new ArrayList<>();

        for (String json : listFiles()){
            ApplicationCommandRequest request = mapper.getObjectMapper()
                    .readValue(json, ApplicationCommandRequest.class);
            commands.add(request);
        }

        appService.bulkOverwriteGlobalApplicationCommand(appID, commands)
                .doOnNext(cmd -> LOGGER.debug("Successfully registered Global Command: " + cmd.name()))
                .doOnError(err -> LOGGER.error("Failed to register global commands: " + err))
                .subscribe();
        appService.bulkOverwriteGuildApplicationCommand(appID, guildID, commands)
                .doOnNext(cmd -> LOGGER.debug("Successfully registered Guild command: " + cmd.name()))
                .doOnError(err -> LOGGER.error("Failed to register guild commands: " + err))
                .subscribe();
    }

    private List<String> listFiles() throws IOException {
        // Confirming command folder exists
        URL url = CommandRegistrar.class.getClassLoader().getResource(COMMAND_FOLDER);
        Objects.requireNonNull(url, COMMAND_FOLDER + " could not be found!");

        // For Windows environment
        Path path = Path.of(url.getPath().replace("/C:","C:"));
        File[] jsons = new File(path.toString()).listFiles();
        List<String> list = new ArrayList<>();

        assert jsons != null;
        for (File json : jsons){
            String jsonToString = fileToString(COMMAND_FOLDER + json.getName());
            list.add(Objects.requireNonNull(jsonToString, "Command file not found: " + json.getName()));
        }

        return list;
    }

    private static String fileToString(String fileName) throws IOException {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        try(InputStream inputStream = loader.getResourceAsStream(fileName)) {
            if (inputStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
