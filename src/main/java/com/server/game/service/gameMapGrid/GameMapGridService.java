package com.server.game.service.gameMapGrid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.server.game.repository.mongo.GameMapGridCompressRepository;
import com.server.game.resource.modelInfo.GameMapGridCompressInfo;
import com.server.game.resource.modelInfo.GameMapGridInfo;
import com.server.game.util.Util;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Slf4j
public class GameMapGridService {

    GameMapGridCompressRepository gameMapGridCompressRepository;

    
    private GameMapGridCompressInfo compressGameMapGrid(GameMapGridInfo gameMapGrid) {
        List<String> gridCompressed = new ArrayList<>();
        for (boolean[] row : gameMapGrid.getGrid()) {
            gridCompressed.add(Util.compressBooleanArray(row));
        }

        return new GameMapGridCompressInfo(
            gameMapGrid.getId(),
            gameMapGrid.getName(),
            gameMapGrid.getCornerA(),
            gameMapGrid.getCornerB(),
            gameMapGrid.getNRows(),
            gameMapGrid.getNCols(),
            gameMapGrid.getCellSize(),
            gridCompressed
        );
    }

    private GameMapGridInfo decompressGameMapGrid(GameMapGridCompressInfo gameMapGridCompress) {
        Integer nRows = gameMapGridCompress.getNRows();
        Integer nCols = gameMapGridCompress.getNCols();
        boolean[][] grid = new boolean[nRows][nCols];
        for (int i = 0; i < nRows; i++) {
            grid[i] = Util.decompressBooleanArray(gameMapGridCompress.getGridCompressed().get(i), nCols);
        }

        return new GameMapGridInfo(
            gameMapGridCompress.getId(),
            gameMapGridCompress.getName(),
            gameMapGridCompress.getCornerA(),
            gameMapGridCompress.getCornerB(),
            gameMapGridCompress.getNRows(),
            gameMapGridCompress.getNCols(),
            gameMapGridCompress.getCellSize(),
            grid
        );
    }



    public void saveGameMapGrid(GameMapGridInfo gameMapGrid) {
        GameMapGridCompressInfo gameMapGridCompress = compressGameMapGrid(gameMapGrid);
        gameMapGridCompressRepository.save(gameMapGridCompress);
    }

    public boolean existsById(short id) {
        return gameMapGridCompressRepository.existsById(id);
    }

    public boolean existsByName(String name) {
        return gameMapGridCompressRepository.existsByName(name);
    }

    public GameMapGridInfo getGameMapGridById(short id) {
        GameMapGridCompressInfo gameMapGridCompress = gameMapGridCompressRepository.findById(id)
            .orElse(null);
        if (gameMapGridCompress == null) {
            log.info("GameMapGridCompress with id " + id + " not found.");
            return null;
        }
        return decompressGameMapGrid(gameMapGridCompress);
    }

}
