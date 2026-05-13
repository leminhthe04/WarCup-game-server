package com.server.game.model.entity;

import java.util.HashSet;
import java.util.Set;

import com.server.game.model.entity.building.Burg;
import com.server.game.model.entity.building.Tower;
import com.server.game.model.entity.component.GoldComponent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


// @Data
@Setter
@Getter
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotState {
    final Short slotNumber;

    final GameState gameState;

    // Cannot use @Delegate here because Champion has heathComponent and attributeComponent
    // which have already had @Delegate annotations.
    Champion champion;

    Set<Tower> towers;

    Burg burg;

    // boolean eliminated = false;

    @Delegate
    final GoldComponent goldComponent;

    final Set<Minion> minions;

    public SlotState(GameState gameState, Short slotNumber, Champion champion, Set<Tower> towers, Burg burg, Integer initialGold) {
        this.gameState = gameState;
        this.slotNumber = slotNumber;
        this.champion = champion;
        this.towers = (towers != null) ? towers : new HashSet<>();
        this.burg = burg;
        this.goldComponent = new GoldComponent(initialGold);
        this.minions = new HashSet<>();
    }

    public boolean isEliminated() {
        return !this.getBurg().isAlive();
    }

    public void addMinion(Minion minion) {
        if (minion == null) {
            log.error(">>> [SlotState] Cannot add null minion");
            return;
        }
        this.minions.add(minion);
    }

    public boolean isChampionAlive(){
        return this.champion.isAlive();
    }

    public float getMoveSpeed() {
        return this.champion.getMoveSpeed();
    }

    public int getCurrentHP() {
        return this.champion.getCurrentHP();
    }

    public int getMaxHP() {
        return this.champion.getMaxHP();
    }

    public float getHealthPercentage() {
        return this.champion.getHealthPercentage();
    }

    public void setCurrentHP(int hp) {
        this.champion.setCurrentHP(hp);
    }

    public void setChampionDead() {
        this.setCurrentHP(0);
    }

    public void setChampionRevive() {
        this.setCurrentHP(this.getMaxHP());
    }

    public void addMinionInstance(Minion minionInstance){
        this.minions.add(minionInstance);
    }

    public int getMinionCount(){ return this.minions.size(); }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (!(other instanceof SlotState otherSlotState)) { return false; }
        if (this == other) { return true; }
        return this.slotNumber == otherSlotState.slotNumber;
    }

    /**
     * Get player status summary
     */
    public String getStatusSummary() {
        return String.format("Slot %d (%s): HP %d/%d, Gold: %d, Minions: %d, Alive: %s",
                slotNumber, champion.getChampionEnum(), getCurrentHP(), getMaxHP(), getCurrentGold(), getMinionCount(),
                champion.isAlive());
    }
}