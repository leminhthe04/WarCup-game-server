package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum NPCPriorityEnum {
    CHAMPION_ATKG_CHAMPION((short) 1), // 1 means highest prio
    MINION_ATKG_CHAMPION((short) 2),
    MINION_ATKG_MINION((short) 3),
    BUILDING_ATKG_MINION((short) 4),
    CHAMPION_ATKG_MINION((short) 5),
    BUILDING((short) 6),
    
    // champions and minions attacking building
    // have same prio as free champions and minions
    MINION_ATKG_BUILDING((short) 7),  
    CHAMPION_ATKG_BUILDING((short) 8),  
    
    MINION_FREE((short) 7),
    CHAMPION_FREE((short) 8),
    ;

    private final short npcPrio;

    public short toShort() {
        return this.npcPrio;
    }

    public boolean higher(NPCPriorityEnum other) {
        return this.npcPrio < other.npcPrio;
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
