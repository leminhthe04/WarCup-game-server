package com.server.game.resource.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.resource.modelInfo.ChampionInfo;
import com.server.game.resource.modelInfo.GameMapGridInfo;
import com.server.game.resource.modelInfo.GameMapInfo;
import com.server.game.resource.modelInfo.MinionInfo;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;


@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class JsonReader {

    ObjectMapper objectMapper;

    public GameMapInfo readGameMapFromJson(String mapName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/map/" + mapName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/map/" + mapName + ".json");
                log.info(">>> File not found: resources/game/map/" + mapName + ".json");
                return null;
            }
            return objectMapper.readValue(is, GameMapInfo.class);

        } catch (IOException e) {
            // e.printStackTrace();
            log.info(">>> Error reading map JSON: " + e.getMessage());
            return null;
        }
    }

    public GameMapGridInfo readGameMapGridFromJson(String mapName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/map/" + mapName + "_grid.json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/map/" + mapName + "_grid.json");
                log.info(">>> File not found: resources/game/map/" + mapName + "_grid.json");
                return null;
            }
            return objectMapper.readValue(is, GameMapGridInfo.class);

        } catch (IOException e) {
            // e.printStackTrace();
            log.info(">>> Error reading map grid JSON: " + e.getMessage());
            return null;
        }
    }   

    public ChampionInfo readChampionFromJson(String championName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/champion/" + championName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/champion/" + championName + ".json");
                log.info(">>> File not found: resources/game/champion/" + championName + ".json");
                return null;
            }
            return objectMapper.readValue(is, ChampionInfo.class);

        } catch (IOException e) {
            // e.printStackTrace();
            log.info(">>> Error reading champion JSON: " + e.getMessage());
            return null;
        }
    }

    public MinionInfo readTroopFromJson(String troopName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/minion/" + troopName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/minion/" + troopName + ".json");
                log.info(">>> File not found: resources/game/minion/" + troopName + ".json");
                return null;
            }
            return objectMapper.readValue(is, MinionInfo.class);

        } catch (IOException e) {
            // e.printStackTrace();
            log.info(">>> Error reading minion JSON: " + e.getMessage());
            return null;
        }
    }
}
