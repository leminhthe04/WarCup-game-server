package com.server.game.factory;

import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.building.Tower;
import com.server.game.resource.modelInfo.SlotInfo.TowerDB;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class TowerFactory {
    

    public Tower createTower(GameState gameState, SlotState ownerSlot, TowerDB towerDB) {
        return new Tower(ownerSlot, gameState, towerDB);
    }
}
