package com.server.game.factory;

import com.server.game.model.entity.Champion;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.resource.modelInfo.ChampionInfo;
import com.server.game.service.champion.ChampionService;
import com.server.game.service.move.MoveService;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class ChampionFactory {
    
    ChampionService championService;
    SkillFactory skillFactory;
    MoveService moveService;

    public Champion createChampion(ChampionEnum championEnum, GameState gameState, SlotState ownerSlot) {
        ChampionInfo championDB = championService.getChampionDBById(championEnum);
        if (championDB == null) {
            return null;
        }
        return new Champion(championDB, ownerSlot, skillFactory);
    }
}
