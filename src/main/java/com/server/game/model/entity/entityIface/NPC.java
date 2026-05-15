package com.server.game.model.entity.entityIface;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.CircleShape;
import com.server.game.model.map.shape.Shape;
import com.server.game.service.gameState.GameStateService;

public interface NPC {
    
    float getDetectionRange();
    Vector2 getCurrentPosition();
    GameState getGameState();
    GameStateService getGameStateService();
    SlotState getOwnerSlot();
    float distanceTo(Entity other);
    
    default Set<Entity> getEnemiesInDetectionRange() {

        Shape scope = new CircleShape(this.getCurrentPosition(), this.getDetectionRange());
        return this.getGameStateService().getEnemiesInScope(
                this.getGameState(), scope, this.getOwnerSlot());

    }

    public default List<Entity> getSortedEnemiesInDetectionRange() {

        return this.getEnemiesInDetectionRange().stream()
                .sorted(Comparator.comparingInt((Entity e) -> e.getNpcPriorityEnum().toShort())
                        .thenComparingDouble(e -> this.distanceTo(e)))
                .toList();

    }
}
