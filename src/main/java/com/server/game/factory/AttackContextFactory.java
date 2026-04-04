package com.server.game.factory;

import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.context.AttackContext;
import com.server.game.service.gameState.GameStateService;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.stereotype.Component;

@Data
@Component
@AllArgsConstructor
public class AttackContextFactory {
    
    private final GameStateService gameStateService;


    public AttackContext createAttackContext(
        String gameId, String attackerStringId, String targetStringId, long timestamp) {

        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] GameState not found for gameId: " + gameId);
        }

        Entity attacker = gameState.getEntityByStringId(attackerStringId);
        Entity target = gameState.getEntityByStringId(targetStringId);

        if (attacker == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Attacker not found: " + attackerStringId);
        }
        if (target == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Target not found: " + targetStringId);
        }

        return new AttackContext(gameState, attacker, target, timestamp);
    }
}
