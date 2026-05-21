package com.server.game.netty.messageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.MoveContextFactory;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.context.MoveContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.PositionReceive;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class MoveMessageHandler {

    private final MoveContextFactory moveContextFactory;

    private final GameStateService gameStateService;
    private final MoveService moveService;

    @MessageMapping(PositionReceive.class)
    public void handleMoveMessage(PositionReceive receiveObject, Channel channel) {
        

        String gameId = ChannelManager.getGameIdByChannel(channel);
        GameState gameState = gameStateService.getGameStateById(gameId);
        
        Entity mover = gameStateService.getEntityByStringId(gameState, receiveObject.getStringId());
        // if (mover instanceof Minion minion) {
        //     minion.setDefensePosition(receiveObject.getPosition());
        //     minion.setInDefensiveStance(false);
        // }

        if (mover == null) {
            return;
        }

        log.info("Receive a [move] request in gameId={}, slot={}", 
            gameId, mover.getOwnerSlot().getSlotNumber());

        

        MoveContext ctx = moveContextFactory.createMoveContext(
            gameState,
            mover,
            receiveObject.getPosition(),
            receiveObject.getTimestamp()
        );

        moveService.setMove(ctx, true);
    }

}