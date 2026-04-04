package com.server.game.resource.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.server.game.repository.mongo.ChampionDBRepository;
import com.server.game.repository.mongo.GameMapRepository;
import com.server.game.repository.mongo.MinionDBRepository;
import com.server.game.resource.modelInfo.ChampionInfo;
import com.server.game.resource.modelInfo.GameMapGridInfo;
import com.server.game.resource.modelInfo.GameMapInfo;
import com.server.game.resource.modelInfo.MinionInfo;
import com.server.game.resource.reader.JsonReader;
import com.server.game.service.gameMapGrid.GameMapGridService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class Writer {

    JsonReader jsonReader;
    GameMapRepository mapRepository;
    ChampionDBRepository championDBRepository;
    MinionDBRepository troopDBRepository;

    GameMapGridService gameMapGridService;


    private final List<String> mapNames = new ArrayList<>(Arrays.asList(
        "map_2"
    ));

    private final List<String> championNames = new ArrayList<>(Arrays.asList(
        "Axe",
        "Knight",
        "Archer",
        "Wizard"
    ));
    
    private final List<String> troopNames = new ArrayList<>(Arrays.asList(
        "Axis",
        "Crossbawl",
        "Healer",
        "Shadow"
    ));
    

    @EventListener(ApplicationReadyEvent.class)
    public void writeMaps() {
        for (String mapName : mapNames) {
            // if (mapRepository.existsByName(mapName)
            //     && false
            //  ) { // remove && false when not debugging
            //     log.info("Map already exists: " + mapName);
            //     continue;
            // }
            this.writeMap(mapName); 
        }
    }


    private void writeMap(String mapName) {
        GameMapInfo map = jsonReader.readGameMapFromJson(mapName);
        if (map == null) {
            log.info("Failed to read map from JSON.");
            return;
        }
        mapRepository.save(map);
        log.info("Map saved successfully: " + map.getName());
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void writeMapGrids() {
        for (String mapName : mapNames) {
            // if (gameMapGridService.existsByName(mapName)
            //     && false
            //  ) { // remove && false when not debugging
            //     log.info("Game map already exists: " + mapName);
            //     continue;
            // }
            this.writeGameMapGrid(mapName);
        }
    }


    private void writeGameMapGrid(String mapName) {
        GameMapGridInfo mapGrid = jsonReader.readGameMapGridFromJson(mapName);
        if (mapGrid == null) {
            log.info("Failed to read map grid from JSON.");
            return;
        }
        gameMapGridService.saveGameMapGrid(mapGrid);
        log.info("Game map grid saved successfully: " + mapGrid.getId());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void writeChampions() {
        for (String championName : championNames) {
            // if (championDBRepository.existsByName(championName) 
            //     && false
            // ) { // remove && false when not debugging
            //     log.info("Champion already exists: " + championName);
            //     continue;
            // }
            this.writeChampion(championName.toLowerCase());
        }
    }

    private void writeChampion(String championName) {
        ChampionInfo champion = jsonReader.readChampionFromJson(championName);
        if (champion == null) {
            log.info("Failed to read champion from JSON.");
            return;
        }
        championDBRepository.save(champion);
        log.info("Champion saved successfully: " + champion.getName());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void writeTroops() {
        for (String troopName : troopNames) {
            // if (troopDBRepository.existsByName(troopName) 
            //     && false
            // ) { // remove && false when not debugging
            //     log.info("Troop already exists: " + troopName);
            //     continue;
            // }
            this.writeTroop(troopName.toLowerCase());
        }
    }

    private void writeTroop(String troopName) {
        MinionInfo minion = jsonReader.readTroopFromJson(troopName);
        if (minion == null) {
            log.info("Failed to read minion from JSON.");
            return;
        }
        troopDBRepository.save(minion);
        log.info("Troop saved successfully: " + minion.getName());
    }
}
