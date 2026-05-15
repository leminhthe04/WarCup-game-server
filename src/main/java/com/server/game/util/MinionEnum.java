package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MinionEnum {
    AXIS((short) 0),
    SHADOW((short) 1),
    CROSSBAWL((short) 2),
    HEALER((short) 3);

    private final short minionId;

    public short toShort() {
        return this.minionId;
    }

    public static MinionEnum fromShort(short id) {
        for (MinionEnum minion : values()) {
            if (minion.getMinionId() == id) {
                return minion;
            }
        }
        throw new IllegalArgumentException("Unknown MinionId: " + id);
    }
    
    public static MinionEnum fromString(String name) {
        for (MinionEnum minion : values()) {
            if (minion.name().equalsIgnoreCase(name)) {
                return minion;
            }
        }
        throw new IllegalArgumentException("Unknown Minion name: " + name);
    }
}
