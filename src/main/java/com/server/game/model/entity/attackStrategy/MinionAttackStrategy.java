package com.server.game.model.entity.attackStrategy;

import org.springframework.stereotype.Component;

import com.server.game.model.entity.context.AttackContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MinionAttackStrategy implements AttackStrategy {

    @Override
    public boolean performAttack(AttackContext ctx) {
        // target is a independent entity, like gold mine
        if (ctx.getTarget().getOwnerSlot() == null) {
            return false; // minion does not attack independent entities
        }

    
        if (ctx.getAttacker().isAllies(ctx.getTarget())) {
            return false; // Cannot attack allies
        }

        // 1. First send attack animation of the attacker
        ctx.getGameStateService().sendAttackAnimation(ctx);

        // 2. Then perform the attack logic
        return ctx.getTarget().receiveAttack(ctx);
    }
}


