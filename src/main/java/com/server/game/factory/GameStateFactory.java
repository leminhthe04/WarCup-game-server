package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.service.gameMap.GameMapService;
import com.server.game.service.gameMapGrid.GameMapGridService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;

import com.server.game.model.entity.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.resource.modelInfo.GameMapGridInfo;
import com.server.game.resource.modelInfo.GameMapInfo;

import lombok.AccessLevel;


@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameStateFactory {

    GameStateService gameStateService;
    GameMapService gameMapService;
    GameMapGridService gameMapGridService;
    SlotStateFactory slotStateFactory;

    // From the channel, get all components needed to build the game state
    public GameState createGameState(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        Short gameMapId = numPlayers; // GameMap id is determined by the number of players

        String gameId = ChannelManager.getGameIdByChannel(channel);
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionEnum(gameId);


        GameMapInfo gameMap = gameMapService.getGameMapById(gameMapId);
        GameMapGridInfo gameMapGrid = gameMapGridService.getGameMapGridById(gameMapId);

        GameState gameState = new GameState(gameId, gameMap, gameMapGrid, slot2ChampionId, 
            gameStateService, slotStateFactory);


        return gameState;
    }

    // For testing purposes, build a GameState with given parameters
    public GameState createGameState(String gameId, Map<Short, ChampionEnum> slot2ChampionId) {
        
        Short gameMapId = (short) slot2ChampionId.size(); // GameMap id is determined by the number of players
        
        GameMapInfo gameMap = gameMapService.getGameMapById(gameMapId);
        GameMapGridInfo gameMapGrid = gameMapGridService.getGameMapGridById(gameMapId);

        GameState gameState = new GameState(gameId, gameMap, gameMapGrid, slot2ChampionId, 
            gameStateService, slotStateFactory);

        return gameState;
    }
}
