package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.service.move.MoveService;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.GoldMine;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.minion.MinionService;

import lombok.AccessLevel;


@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoldMineFactory {

    GameStateService gameStateService;
    ChampionFactory championFactory;
    MinionService troopService;
    SlotStateService slotStateService;
    MoveService moveService;

    public GoldMine createGoldMine(GameState gameState, boolean isSmallGoldMine, Vector2 position) {
        if (gameState == null) {
            return null;
        }

        Integer goldCapacity = isSmallGoldMine 
        ? gameState.getGameMap().getSmallGoldMineCapacity() 
        : gameState.getGameMap().getLargeGoldMineCapacity();

        Integer initHP = isSmallGoldMine 
        ? gameState.getGameMap().getSmallGoldMineInitialHP() 
        : gameState.getGameMap().getLargeGoldMineInitialHP();

        GoldMine goldMine = new GoldMine(gameState, goldCapacity, initHP, position);
        gameStateService.addEntityTo(gameState, goldMine);
        
        return goldMine;
    }
}
