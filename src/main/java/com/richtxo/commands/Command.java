package com.richtxo.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;

public interface Command {

    /**
     * Gets command's name
     * @return String of command's name
     */
    default String getName() {
        return "COMMAND_NAME";
    }

    /**
     * Gets command's category
     * @return String of command's category
     */
    default String getCategory() {
        return "COMMAND_CATEGORY";
    }

    /**
     * Gets aliases of command
     * <br>
     * Array would return empty if none are present
     * @return String array of command's aliases if any
     */
    default String[] getAliases() {
        return new String[]{""};
    }

    /**
     * Gets info of command to be used in help menu
     * @return String of command's info
     */
    default String getCmdInfo() {
        return "COMMAND_INFO";
    }

    /**
     * Main command's function
     * @param event Discord's input events
     * @return Actions for the desired command from Discord's server
     */
    default Mono<Void> handle(ChatInputInteractionEvent event){
        return null;
    }
}
