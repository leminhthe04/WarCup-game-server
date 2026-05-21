package com.server.game.service.gameState;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;
import com.server.game.model.entity.building.Burg;
import com.server.game.model.entity.building.Tower;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlotStateService {

    public List<Tower> getTowers(SlotState slotState) {
        if (slotState == null) {
            log.warn("SlotState is null when getting Towers");
            return List.of();
        }

        Set<Tower> setTowers = slotState.getTowers();
        if (setTowers == null) {
            return List.of();
        }
        return setTowers.stream()
            // Sắp xếp ưu tiên x tăng dần, sau đó y tăng dần
            .sorted(Comparator.comparing((Tower t) -> t.getCurrentPosition().x())
                              .thenComparing(t -> t.getCurrentPosition().y()))
            .toList();

    }

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
        // log.info("Added minion {} to slot state {}", minion.getStringId(), slotState.getSlotNumber());
    }

    public boolean removeMinion(SlotState slotState, Minion minion) {
        if (slotState == null || minion == null) {
            log.warn("SlotState or TroopInstance is null when removing minion");
            return false;
        }

        boolean removed = slotState.getMinions().remove(minion);
        if (removed) {
            // log.info("Removed minion {} from slot state {}", minion.getStringId(), slotState.getSlotNumber());
        } else {
            log.warn("Failed to remove minion {} from slot state {} - minion not found", minion.getStringId(),
                    slotState.getSlotNumber());
        }
        return removed;
    }
}
