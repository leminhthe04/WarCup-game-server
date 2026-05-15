package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum NPCPriorityEnum {
    CHAMPION_ATKG_CHAMPION((short) 8),
    MINION_ATKG_CHAMPION((short) 7),
    MINION_ATKG_MINION((short) 6),
    BUILDING_ATKG_MINION((short) 5),
    CHAMPION_ATKG_MINION((short) 4),
    BUILDING((short) 3),
    
    // champions and minions attacking building
    // have same prio as free champions and minions
    CHAMPION_ATKG_BUILDING((short) 1),  
    MINION_ATKG_BUILDING((short) 2),  
    
    MINION_FREE((short) 2),
    CHAMPION_FREE((short) 1),
    ;

    private final short npcPrio;

    public short toShort() {
        return this.npcPrio;
    }

    public static NPCPriorityEnum fromShort(short p) {
        for (NPCPriorityEnum prio : values()) {
            if (prio.getNpcPrio() == p) {
                return prio;
            }
        }
        throw new IllegalArgumentException("Unknown NPC priority: " + p);
    }
}
