package com.server.game.service.gameState;

import org.springframework.stereotype.Service;

import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlotStateService {
    public void addMinion(SlotState slotState, Minion minion) {
        if (slotState == null || minion == null) {
            log.warn("SlotState or TroopInstance is null when adding minion");
            return;
        }

        slotState.addMinion(minion);
        log.info("Added minion {} to slot state {}", minion.getStringId(), slotState.getSlot());
    }

    public boolean removeMinion(SlotState slotState, Minion minion) {
        if (slotState == null || minion == null) {
            log.warn("SlotState or TroopInstance is null when removing minion");
            return false;
        }

        boolean removed = slotState.getMinions().remove(minion);
        if (removed) {
            log.info("Removed minion {} from slot state {}", minion.getStringId(), slotState.getSlot());
        } else {
            log.warn("Failed to remove minion {} from slot state {} - minion not found", minion.getStringId(), slotState.getSlot());
        }
        return removed;
    }
}
