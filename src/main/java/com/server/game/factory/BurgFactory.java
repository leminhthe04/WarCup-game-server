package com.server.game.factory;

import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.building.Burg;
import com.server.game.resource.modelInfo.SlotInfo.BurgDB;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class BurgFactory {

    public Burg createBurg(GameState gameState, SlotState ownerSlot, BurgDB burgDB) {
        return new Burg(ownerSlot, gameState, burgDB);
    }
}
