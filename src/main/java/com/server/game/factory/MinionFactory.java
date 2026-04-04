package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.service.move.MoveService;
import com.server.game.util.MinionEnum;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;
import com.server.game.resource.modelInfo.MinionInfo;
import com.server.game.service.minion.MinionService;

import lombok.AccessLevel;


@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinionFactory {

    GameStateService gameStateService;
    ChampionFactory championFactory;
    MinionService troopService;
    SlotStateService slotStateService;
    MoveService moveService;

    public Minion createMinion(SlotState slotState, MinionEnum minionType) {
        GameState gameState = slotState.getGameState();
        if (gameState == null) {
            return null;
        }

        MinionInfo minionDB = troopService.getMinionDBById(minionType);
        if (minionDB == null) {
            return null;
        }

        if (gameState.peekGold(slotState) < minionDB.getCost()) {
            return null;
        }

        Minion minionInstance = new Minion(
            minionDB,
            slotState
        );

        gameState.spendGold(slotState, minionDB.getCost());

        gameStateService.addEntityTo(gameState, minionInstance);
        slotStateService.addMinion(slotState, minionInstance);
        
        return minionInstance;
    }
}
