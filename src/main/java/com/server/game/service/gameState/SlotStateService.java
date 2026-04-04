package com.server.game.service.gameState;

import org.springframework.stereotype.Service;

import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;
import com.server.game.model.entity.building.Burg;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlotStateService {

    public Burg getBurg(SlotState slotState) {
        if (slotState == null) {
            log.warn("SlotState is null when getting Burg");
            return null;
        }
        return slotState.getBurg();
    }

    public void addMinion(SlotState slotState, Minion minion) {
        if (slotState == null || minion == null) {
            log.warn("SlotState or TroopInstance is null when adding minion");
            return;
        }

        slotState.addMinion(minion);
        log.info("Added minion {} to slot state {}", minion.getStringId(), slotState.getSlotNumber());
    }

    public boolean removeMinion(SlotState slotState, Minion minion) {
        if (slotState == null || minion == null) {
            log.warn("SlotState or TroopInstance is null when removing minion");
            return false;
        }

        boolean removed = slotState.getMinions().remove(minion);
        if (removed) {
            log.info("Removed minion {} from slot state {}", minion.getStringId(), slotState.getSlotNumber());
        } else {
            log.warn("Failed to remove minion {} from slot state {} - minion not found", minion.getStringId(), slotState.getSlotNumber());
        }
        return removed;
    }
}
