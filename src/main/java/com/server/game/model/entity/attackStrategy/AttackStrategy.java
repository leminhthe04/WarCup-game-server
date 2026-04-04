package com.server.game.model.entity.attackStrategy;

import com.server.game.model.entity.context.AttackContext;

public interface AttackStrategy {
    boolean performAttack(AttackContext ctx);
}