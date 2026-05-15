package com.server.game.factory;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.util.MinionEnum;

import jakarta.annotation.PostConstruct;

import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.repository.mongo.MinionDBRepository;
import com.server.game.model.entity.Minion;
import com.server.game.resource.modelInfo.MinionInfo;

import lombok.AccessLevel;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinionFactory {

    GameStateService gameStateService;
    SlotStateService slotStateService;
    MinionDBRepository minionDBRepository;

    // Store minion instance to create minionInstance2 instances
    private final Map<MinionEnum, MinionInfo> minionDBCache = new HashMap<>();

    @PostConstruct
    private void initCache() {
        // Preload all minionDBs into the cache
        List<MinionInfo> allMinionDBs = minionDBRepository.findAll();
        for (MinionInfo minionDB : allMinionDBs) {
            minionDBCache.put(MinionEnum.fromShort(minionDB.getId()), minionDB);
        }
    }

    private MinionInfo getMinionDBById(MinionEnum minionEnum) {
        return minionDBCache.get(minionEnum);
    }

    public Set<MinionInfo> getAllMinions() {
        return new HashSet<MinionInfo>(minionDBCache.values());
    }

    public Minion createMinion(SlotState slotState, MinionEnum minionType) {
        GameState gameState = slotState.getGameState();
        if (gameState == null) {
            return null;
        }

        MinionInfo minionDB = this.getMinionDBById(minionType);
        if (minionDB == null) {
            log.warn("DB has no minion type={}", minionType);
            return null;
        }

        if (gameState.peekGold(slotState) < minionDB.getCost()) {
            log.warn("Not enough gold to spawn minion, current gold: {}, minion cost: {}",
                    gameState.peekGold(slotState), minionDB.getCost());
            return null;
        }

        Minion minionInstance = new Minion(
                minionDB,
                slotState);

        gameState.spendGold(slotState, minionDB.getCost());

        gameStateService.addEntityTo(gameState, minionInstance);
        slotStateService.addMinion(slotState, minionInstance);

        return minionInstance;
    }
}
