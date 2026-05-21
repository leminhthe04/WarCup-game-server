package com.server.game.service.minion;

import org.springframework.stereotype.Service;
import com.server.game.factory.MinionFactory;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.building.Burg;
import com.server.game.model.entity.Minion;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.CircleShape;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.entity.EntityDeathSend;
import com.server.game.resource.modelInfo.MinionInfo;
import com.server.game.service.attack.AttackService;
import com.server.game.service.move.MoveService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.factory.AttackContextFactory;
import com.server.game.util.MinionEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import io.netty.channel.Channel;


@Slf4j
@Service
@RequiredArgsConstructor
public class MinionService {

    private final GameStateService gameStateService;
    private final SlotStateService slotStateService;
    private final MinionFactory minionFactory;
    private final AttackContextFactory attackContextFactory;
    private final AttackService attackService;
    private final MoveService moveService;

    public Set<MinionInfo> getAllMinions() {
        return minionFactory.getAllMinions();
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

    /**
     * By default, after spawning, minions will move to and attack the opponent's
     * burg.
     */
    public void setMoveFollowingStandardPath(Minion minion) {
        GameState gameState = minion.getGameState();

        List<SlotState> slotStates = gameStateService.getAllSlotStates(gameState);
        if (slotStates == null || slotStates.size() < 2) {
            throw new IllegalStateException("Not enough slot states found in game state for minion to attack");
        }

        List<SlotState> enemiesSlots = slotStates.stream()
                .filter(slotState -> !slotState.equals(minion.getOwnerSlot()))
                .toList();

        if (enemiesSlots == null || enemiesSlots.isEmpty()) {
            log.error("cannot find opponent slot to set minion attack");
        }

        // TODO: Handle if there are more than one opponent (in 3+ player games)
        SlotState enemySlot = enemiesSlots.get(0);

        // List<Tower> sortedTowers = slotStateService.getTowers(targetSlot);
        // // a first alive Tower in list above.
        // // if all targetSlot's towers are not alive, it's a Burg
        // Entity targetEntity = null;
        // for (Tower tower : sortedTowers) {
        // if (tower.isAlive()) {
        // targetEntity = tower;
        // break;
        // }
        // }

        // if (targetEntity == null) {
        // Burg targetBurg = slotStateService.getBurg(targetSlot);
        // targetEntity = targetBurg;
        // }

        Burg enemyBurg = slotStateService.getBurg(enemySlot);

        Vector2 moveTo = enemyBurg.getCurrentPosition();

        moveService.setMove(minion, moveTo, true);

        // log.info("Minion follow standard road, move toward to opponent's side. MinionId: {}",
        //         minion.getStringId());
    }

    /**
     * Attack a target
     */
    // public void setAttackTarget(Minion minion, Entity target) {
    // if (minion == null) {
    // log.warn("Minion instance not found");
    // return;
    // }

    // minion.setInDefensiveStance(false); // Disable defense on manual attack

    // AttackContext attackContext =
    // attackContextFactory.createAttackContext(minion, target);

    // attackService.setAttack(attackContext);
    // }

    /**
     * Set move position for a minion instance
     */
    // public void setMovePosition(String gameId, String minionInstanceId, Vector2
    // position) {
    // GameState gameState = gameStateService.getGameStateById(gameId);
    // if (gameState == null) {
    // log.warn("Game state not found for game ID: {}", gameId);
    // return;
    // }
    // Entity minionInstance = gameStateService.getEntityByStringId(gameState,
    // minionInstanceId);
    // if (minionInstance == null) {
    // log.warn("Minion instance not found for ID: {}", minionInstanceId);
    // return;
    // }

    // Minion minion = (Minion) minionInstance;

    // // Update defense position but do NOT enable defensive stance for manual
    // moves
    // minion.updateDefensePosition(position);
    // minion.setInDefensiveStance(false); // Disable defensive stance on manual
    // move

    // MoveContext moveContext = moveContextFactory.createMoveContext(gameState,
    // minionInstance, position,
    // System.currentTimeMillis());
    // moveService.setMove(moveContext, true);

    // log.debug("Manual move set for minion {} to position {}. Defensive stance
    // disabled.", minionInstanceId, position);
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

        // log.info("Minion {} has died in game {}", minionInstanceId, gameState.getGameId());

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
            // log.info("Processed {} minion deaths in game {}", deadMinions.size(), gameState.getGameId());
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
            // log.info("Sent minion death message for gameId: {}, minionId: {}", gameId, minionInstanceId);
        } else {
            log.warn("No channel found for gameId: {} when sending minion death message", gameId);
        }
    }


    public void checkStopChasing(GameState gameState) {
        Set<Minion> minions = gameStateService.getAllMinions(gameState);
        minions.forEach(minion -> this.checkStopChasingOf(minion));
    }

    private void checkStopChasingOf(Minion minion) {
        CircleShape chasingScope = minion.getChasingScope();
        
        boolean needStopChasing = chasingScope != null && 
            minion.distanceTo(chasingScope.getCenter()) > chasingScope.getRadius();

        if (!needStopChasing) { return; }

        minion.setChasingScope(null);

        // back to standard path after interrupting chasing enemy
        this.setMoveFollowingStandardPath(minion);
        log.info("Minion ID={} stops chasing", minion.getStringId());
    }

    public void updateDetections(GameState gameState) {
        Set<Minion> minions = gameStateService.getAllMinions(gameState);
        minions.forEach(minion -> this.updateDetectionOf(minion));
    }

    private void updateDetectionOf(Minion minion) {
        if (minion == null || !minion.isAlive() || !minion.inDetectionWindow()) {
            return;
        }

        List<Entity> sortedEnemiesInDetectionRange = minion.getSortedEnemiesInDetectionRange();

        // no enemies in minion's detection range
        if (sortedEnemiesInDetectionRange == null || sortedEnemiesInDetectionRange.isEmpty()) {
            return;
        }

        Entity highestPrioEntity = null;
        for (Entity enemy : sortedEnemiesInDetectionRange) {
            if (enemy != null && enemy.isAlive()) {
                highestPrioEntity = enemy;
                break;
            }
        }

        // if (highestPrioEntity != null)
        //     log.info("Highest prio entity id={}, prio={}", highestPrioEntity.getStringId(), highestPrioEntity.getNpcPriorityEnum());

        boolean needUpdatingAttackEnemy = highestPrioEntity != null &&
                (!minion.isAttacking() || // is not attacking or is attacking but lower prio entity
                        highestPrioEntity.getNpcPriorityEnum().higher(
                                minion.getCurrentAttackEntity().getNpcPriorityEnum()));

        // if (minion.isAttacking()) {
        //     log.info("Current minion's target id={}, prio={}", minion.getCurrentAttackEntity().getStringId(), minion.getCurrentAttackEntity().getNpcPriorityEnum());
        // }

        if (!needUpdatingAttackEnemy) {
            return;
        }

        // 1. Assign new attacking entity
        attackService.setAttack(
            attackContextFactory.createAttackContext(minion, highestPrioEntity)
        );

        // 2. Set up chasing range
        minion.setChasingScope(new CircleShape(
            minion.getCurrentPosition(), minion.getDetectionRange()));


        // log.info("Minion id={} detected entity id={} and is set to attack this entity", minion.getStringId(), highestPrioEntity.getStringId());

    }

    
}
