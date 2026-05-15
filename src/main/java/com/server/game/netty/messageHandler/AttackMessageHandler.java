package com.server.game.netty.messageHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.AttackContextFactory;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.context.AttackContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.attack.AttackReceive;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AttackMessageHandler {
    GameStateService gameStateService;

    private final AttackContextFactory attackContextFactory;
    private final AttackService attackService;

    // Rate limiting: minimum time between position updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();

    @MessageMapping(AttackReceive.class)
    public void handleAttackMessage(AttackReceive receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String attackerStringId = receiveObject.getAttackerId();
        String targetStringId = receiveObject.getTargetId();

        // long clientTimestamp = receiveObject.getTimestamp();

        // Rate limiting check
        String playerKey = gameId + ":" + attackerStringId;
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerKey);

        if (lastUpdate != null && (currentTime - lastUpdate) < MIN_UPDATE_INTERVAL) {
            log.debug("Rate limit exceeded for player: {} - ignore attack position update", playerKey);
            return;
        }

        // Update the last update time
        lastUpdateTime.put(playerKey, currentTime);

        Entity attacker = gameStateService.getEntityByStringId(gameId, attackerStringId);
        Entity target = gameStateService.getEntityByStringId(gameId, targetStringId);

        if (attacker == null) {
            log.warn("Attacker not found: {}", attackerStringId);
            return;
        }

        if (target == null) {
            log.warn("Target not found: {}", targetStringId);
            return;
        }

        // Prevent attacking self
        if (attacker.equals(target)) {
            return;
        }

        // if (attacker instanceof Minion minion) {
        //     minion.setInDefensiveStance(false);
        //     minion.setDefensePosition(null);
        //     log.info("Minion {} defensive stance disabled", attackerStringId);
        // }

        AttackContext attackContext = attackContextFactory.createAttackContext(attacker, target);

        attackService.setAttack(attackContext);

        log.info("Attack context set for entity: {}", attackerStringId);
    }
}