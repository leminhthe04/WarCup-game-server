package com.server.game.service.building;

import com.server.game.model.entity.Champion;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.building.Building;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.factory.AttackContextFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingService {

    private final GameStateService gameStateService;
    private final AttackContextFactory attackContextFactory;
    private final AttackService attackService;

    public void updateDetections(GameState gameState) {
        Set<Building> buildings = gameStateService.getAllBuildings(gameState);
        buildings.forEach(building -> this.updateDetectionOf(building));
    }

    private void updateDetectionOf(Building building) {
        if (building == null || !building.isAlive()) {
            return;
        }

        List<Entity> sortedEnemiesInDetectionRange = building.getSortedEnemiesInDetectionRange();

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

        boolean needUpdatingAttackEnemy = !building.isAttacking(Champion.class) // if is not attacking champion
            && highestPrioEntity != null
            && (!building.isAttacking() // is not attacking or is attacking but lower prio entity 
                || highestPrioEntity.getNpcPriorityEnum().higher(
                                building.getCurrentAttackEntity().getNpcPriorityEnum()));

        if (!needUpdatingAttackEnemy) {
            return;
        }

        // 1. Assign new attacking entity
        attackService.setAttack(
            attackContextFactory.createAttackContext(building, highestPrioEntity)
        );

        log.info("Building id={} detected entity id={} and is set to attack this entity", building.getStringId(), highestPrioEntity.getStringId());
    }
}
