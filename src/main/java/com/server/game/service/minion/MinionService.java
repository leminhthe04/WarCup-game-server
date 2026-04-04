package com.server.game.service.minion;

import com.server.game.factory.MinionFactory;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;
import com.server.game.model.entity.context.AttackContext;
import com.server.game.model.entity.context.MoveContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.entity.EntityDeathSend;
import com.server.game.repository.mongo.MinionDBRepository;
import com.server.game.resource.modelInfo.MinionInfo;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.factory.AttackContextFactory;
import com.server.game.factory.MoveContextFactory;
import com.server.game.service.move.MoveService;
import com.server.game.util.MinionEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.netty.channel.Channel;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinionService {

    private final GameStateService gameStateService;
    private final SlotStateService slotStateService;
    private final MinionFactory minionFactory;

    private final MoveContextFactory moveContextFactory;
    private final MoveService moveService;

    private final AttackContextFactory attackContextFactory;
    private final AttackService attackService;

    // Store minion instance to create minionInstance2 instances
    private final Map<MinionEnum, MinionInfo> minionDBCache = new HashMap<>();

    @PostConstruct
    public void initCache(MinionDBRepository minionDBRepository) {
        // Preload all minionDBs into the cache
        List<MinionInfo> allMinionDBs = minionDBRepository.findAll();
        for (MinionInfo minionDB : allMinionDBs) {
            minionDBCache.put(MinionEnum.fromShort(minionDB.getId()), minionDB);
        }
    }

    public MinionInfo getMinionDBById(MinionEnum minionEnum) {
        return minionDBCache.get(minionEnum);
    }

    public Set<MinionInfo> getAllMinions() {
        return new HashSet<MinionInfo>(minionDBCache.values());
    }

    /**
     * Add a minion instance to the game state.
     */
    public Minion createMinion(SlotState requestingSlot, MinionEnum minionType) {
        return minionFactory.createMinion(requestingSlot, minionType);
    }

    /**
     * Remove a minion instance (when it dies or is manually removed)
     */
    public boolean removeMinion(GameState gameState, String minionInstanceId) {
        Entity minionInstance = gameStateService.getEntityByStringId(gameState, minionInstanceId);
        if (minionInstance == null || !(minionInstance instanceof Minion)) {
            log.warn("Minion instance not found for ID: {}", minionInstanceId);
            return false;
        }

        Minion minion = (Minion) minionInstance;

        // Remove from SlotState first
        slotStateService.removeMinion(minion.getOwnerSlot(), minion);

        // Remove from GameState
        gameStateService.removeEntity(gameState, minionInstance);

        return true;
    }

    public void afterMinionSpawning(Minion newMinion) {
        // TODO
    }


    /**
     * Attack a target
     */
    // public void setAttackTarget(String gameId, String minionInstanceId, String targetId) {
    //     GameState gameState = gameStateService.getGameStateById(gameId);
    //     Entity minion = gameStateService.getEntityByStringId(gameState, minionInstanceId);

    //     if (minion == null || !(minion instanceof Minion)) {
    //         log.warn("Minion instance not found for ID: {}", minionInstanceId);
    //         return;
    //     }

    //     ((Minion) minion).setInDefensiveStance(false); // Disable defense on manual attack

    //     AttackContext attackContext = attackContextFactory.createAttackContext(gameId, minionInstanceId, targetId,
    //             System.currentTimeMillis());
    //     attackService.setAttack(attackContext);
    // }

    /**
     * Set move position for a minion instance
     */
    // public void setMovePosition(String gameId, String minionInstanceId, Vector2 position) {
    //     GameState gameState = gameStateService.getGameStateById(gameId);
    //     if (gameState == null) {
    //         log.warn("Game state not found for game ID: {}", gameId);
    //         return;
    //     }
    //     Entity minionInstance = gameStateService.getEntityByStringId(gameState, minionInstanceId);
    //     if (minionInstance == null) {
    //         log.warn("Minion instance not found for ID: {}", minionInstanceId);
    //         return;
    //     }

    //     Minion minion = (Minion) minionInstance;

    //     // Update defense position but do NOT enable defensive stance for manual moves
    //     minion.updateDefensePosition(position);
    //     minion.setInDefensiveStance(false); // Disable defensive stance on manual move

    //     MoveContext moveContext = moveContextFactory.createMoveContext(gameState, minionInstance, position,
    //             System.currentTimeMillis());
    //     moveService.setMove(moveContext, true);

    //     log.debug("Manual move set for minion {} to position {}. Defensive stance disabled.", minionInstanceId, position);
    // }

    /**
     * Check if a minion has died and handle death logic
     * 
     * @return true if minion died, false otherwise
     */
    public boolean checkAndHandleMinionDeath(GameState gameState, String minionInstanceId) {
        Entity minionEntity = gameStateService.getEntityByStringId(gameState, minionInstanceId);
        if (minionEntity == null || !(minionEntity instanceof Minion)) {
            log.warn("Minion instance not found for ID: {}", minionInstanceId);
            return false;
        }

        Minion minion = (Minion) minionEntity;
        if (minion.getCurrentHP() > 0) {
            return false; // Minion is still alive
        }

        log.info("Minion {} has died in game {}", minionInstanceId, gameState.getGameId());

        // Remove the minion from the game state
        this.removeMinion(gameState, minionInstanceId);

        // Send death message to all clients
        this.sendMinionDeathMessage(gameState.getGameId(), minionInstanceId);

        return true;
    }

    /**
     * Check all minions in a game for deaths and handle them
     * This is more efficient than checking minions one by one
     */
    public void checkAndHandleAllMinionDeaths(GameState gameState) {
        // Collect all dead minions to avoid concurrent modification
        var deadMinions = gameState.getEntities().stream()
                .filter(entity -> entity.getStringId().startsWith("minion_"))
                .filter(entity -> entity.getCurrentHP() <= 0)
                .map(Entity::getStringId)
                .toList();

        // Process each dead minion
        for (String minionId : deadMinions) {
            try {
                this.checkAndHandleMinionDeath(gameState, minionId);
            } catch (Exception e) {
                log.error("Error processing death for minion {} in game {}: {}", minionId, gameState.getGameId(),
                        e.getMessage(), e);
            }
        }

        if (!deadMinions.isEmpty()) {
            log.info("Processed {} minion deaths in game {}", deadMinions.size(), gameState.getGameId());
        }
    }

    /**
     * Send minion death message to all clients in the game
     */
    private void sendMinionDeathMessage(String gameId, String minionInstanceId) {
        EntityDeathSend deathMessage = new EntityDeathSend(minionInstanceId);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(deathMessage);
            log.info("Sent minion death message for gameId: {}, minionId: {}", gameId, minionInstanceId);
        } else {
            log.warn("No channel found for gameId: {} when sending minion death message", gameId);
        }
    }
}
