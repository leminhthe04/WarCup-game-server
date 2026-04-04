package com.server.game.model.entity.attackStrategy;

import com.server.game.model.entity.context.AttackContext;

public class TowerAttackStrategy implements AttackStrategy {
    @Override
    public boolean performAttack(AttackContext ctx) {

        if (ctx.getAttacker().isAllies(ctx.getTarget())) {
            return false; // Tower does not attack allies
        }

        // 1. First send attack animation of the attacker
        ctx.getGameStateService().sendAttackAnimation(ctx);

        return ctx.getTarget().receiveAttack(ctx);
    }
}