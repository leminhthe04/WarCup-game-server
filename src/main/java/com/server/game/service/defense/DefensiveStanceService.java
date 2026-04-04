package com.server.game.service.defense;

import com.server.game.factory.AttackContextFactory;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.Minion;
import com.server.game.service.attack.AttackService;
import com.server.game.service.move.MoveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefensiveStanceService {
    private final MoveService moveService;
    private final AttackService attackService;
    private final AttackContextFactory attackContextFactory;

    public void updateDefensiveStances(GameState gameState) {
        gameState.getEntities().stream()
            .filter(e -> e instanceof Minion)
            .map(e -> (Minion) e)
            .forEach(minion -> {
                // First check if minion should re-enable defensive stance
                minion.checkAndEnableDefensiveStance();
                // Then process defense logic
                processMinionDefense(minion, gameState);
            });
    }

    private void processMinionDefense(Minion minion, GameState gameState) {
        // Skip if not in defensive stance, but allow processing if attacking defensively
        if (!minion.isInDefensiveStance()) {
            return;
        }

        // --- Check if minion is outside defense range and needs to return ---
        if (!minion.isWithinOwnDefenseRange()) {
            // If minion is outside defense range, clear target and return to base
            if (minion.getDefensiveTarget() != null) {
                log.debug("Troop {} is outside defense range, clearing target and returning to base", minion.getStringId());
                minion.setDefensiveTarget(null);
                // Clear any attack context when returning to base
                minion.getAttackComponent().setAttackContext(null);
            }
            // Force return to defense position
            moveService.setMove(minion, minion.getDefensePosition(), true);
            log.trace("Troop {} returning to defense position {} (outside defense range)", 
                minion.getStringId(), minion.getDefensePosition());
            return;
        }

        // --- Handle existing target ---
        if (minion.getDefensiveTarget() != null) {
            Entity target = minion.getDefensiveTarget();
            // Use detection range for consistency, not defense range
            boolean targetInRange = minion.getCurrentPosition().distance(target.getCurrentPosition()) <= minion.getDetectionRange();
            
            if (!target.isAlive() || !targetInRange) {
                minion.setDefensiveTarget(null);
                minion.getAttackComponent().setAttackContext(null); // Clear attack
                moveService.setMove(minion, minion.getDefensePosition(), true);
                log.trace("Troop {} disengaging, target left detection range. Returning to {}.", minion.getStringId(), minion.getDefensePosition());
            } else {
                // Target is valid, continue attacking (don't exit early for attacking state)
                if (!minion.isAttacking()) {
                    attackService.setAttack(attackContextFactory.createAttackContext(
                        gameState.getGameId(), minion.getStringId(), target.getStringId(), gameState.getCurrentTick()
                    ));
                }
            }
            return;
        }

        // --- Find new target ---
        findNearestEnemyInDetectionRange(minion, gameState).ifPresent(enemy -> {
            log.trace("Minion {} detected new enemy {} in detection range.", minion.getStringId(), enemy.getStringId());
            minion.setDefensiveTarget(enemy);
            // Immediately start attacking the new target
            attackService.setAttack(attackContextFactory.createAttackContext(
                gameState.getGameId(), minion.getStringId(), enemy.getStringId(), gameState.getCurrentTick()
            ));
        });

        // --- Return to post if idle and away ---
        if (minion.getDefensiveTarget() == null && !minion.isMoving()) {
            if (minion.getCurrentPosition().distance(minion.getDefensePosition()) > 0.5f) {
                log.trace("Troop {} is idle and away from post. Returning to {}.", minion.getStringId(), minion.getDefensePosition());
                moveService.setMove(minion, minion.getDefensePosition(), true);
            }
        }
    }

    private Optional<Entity> findNearestEnemyInDetectionRange(Minion minion, GameState gameState) {
        return gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot() != null) // Ensure entity has an owner slot
            .filter(e -> e.getOwnerSlot().getSlot() != minion.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> minion.getCurrentPosition().distance(e.getCurrentPosition()) <= minion.getDetectionRange())
            .min(Comparator.comparing(e -> minion.getCurrentPosition().distance(e.getCurrentPosition())));
    }
}