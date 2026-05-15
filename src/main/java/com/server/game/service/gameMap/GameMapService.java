package com.server.game.service.gameMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.server.game.model.map.component.Vector2;
import com.server.game.repository.mongo.GameMapRepository;
import com.server.game.resource.modelInfo.GameMapInfo;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Slf4j
public class GameMapService {

    GameMapRepository gameMapRepository;

    public GameMapInfo getGameMapById(short id) {
        return gameMapRepository.findById(id).orElseGet(() -> {
            log.info("GameMap with id " + id + " not found.");
            return null;
        });
    }


    public Vector2 getSpawnPosition(Short gameMapId, short slot) {
        GameMapInfo gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            log.info("GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getSpawnPosition(slot);
    }


    public Float getInitialRotate(Short gameMapId, short slot) {
        GameMapInfo gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            log.info("GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getInitialRotate(slot);
    }

}
