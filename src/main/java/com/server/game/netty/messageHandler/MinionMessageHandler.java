package com.server.game.netty.messageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.entity.Entity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.Minion;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.minion.MinionMovingReceive;
import com.server.game.netty.receiveObject.minion.MinionSpawnReceive;
import com.server.game.netty.sendObject.minion.MinionSpawnSend;
import com.server.game.netty.sendObject.minion.MinionCooldownSend;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.minion.MinionService;
import com.server.game.service.move.MoveService;
import com.server.game.util.MinionEnum;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinionMessageHandler {

    MinionService minionService;
    GameCoordinator gameCoordinator;
    GameStateService gameStateService;
    MoveService moveService;

    // Cooldown tracking: gameId:slot:minionType -> timestamp when cooldown ends
    private final Map<String, Long> rateLimiter = new ConcurrentHashMap<>();
    private static final long TROOP_SPAWN_LIMIT_MS = 3000; // TODO: 3 seconds cooldown for minion spawn

    /**
     * xử lý việc người chơi spawn quân đội
     */
    @MessageMapping(MinionSpawnReceive.class)
    public void handleMinionSpawnRequest(MinionSpawnReceive request, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short requestingSlotNumber = ChannelManager.getSlotByChannel(channel);

        if (gameId == null || requestingSlotNumber == null) {
            log.warn("Game ID or slot not found for channel when processing minion spawn");
            return;
        }

        // Convert minionId to MinionEnum
        MinionEnum minionType;
        try {
            minionType = MinionEnum.fromShort(request.getMinionId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid minion ID: {}", request.getMinionId(), e);
            return;
        }

        // Check cooldown before spawning
        String cooldownKey = gameId + ":" + requestingSlotNumber + ":" + minionType;
        long currentTime = System.currentTimeMillis();
        Long cooldownEndTime = rateLimiter.get(cooldownKey);

        if (cooldownEndTime != null && currentTime < cooldownEndTime) {
            long remainingCooldown = (cooldownEndTime - currentTime) / 1000; // Convert to seconds
            log.info("Minion spawn rejected due to cooldown. Slot: {}, MinionType: {}, Remaining: {}s",
                    requestingSlotNumber, minionType, remainingCooldown);
            return;
        }

        GameState gameState = gameCoordinator.getGameState(gameId);
        if (gameState == null) {
            log.warn("No game state found for game ID: {}", gameId);
            return;
        }

        SlotState requestingSlot = gameStateService.getSlotStateFromSlotNumber(gameState, requestingSlotNumber);

        Minion newMinion = minionService.createMinion(requestingSlot, minionType);

        if (newMinion == null) {
            log.warn("Failed to create minion instance for type: {}", minionType);
            return;
        }

        // Set cooldown after successful spawn
        rateLimiter.put(cooldownKey, currentTime + TROOP_SPAWN_LIMIT_MS);

        // Send cooldown message to client
        MinionCooldownSend cooldownMessage = new MinionCooldownSend(
                request.getMinionId(), (short) (TROOP_SPAWN_LIMIT_MS / 1000));
        channel.writeAndFlush(cooldownMessage);

        ChannelFuture future = this.broadcastMinionSpawn(newMinion, channel);
        if (future != null) {
            future.addListener(f -> {
                if (f.isSuccess()) {
                    minionService.afterMinionSpawning(newMinion);
                } else {
                    log.warn("Error when broadcasting minion spawn");
                }
            });
        }

        log.info("Minion spawned successfully: {}, cooldown set for {} seconds",
                minionType, TROOP_SPAWN_LIMIT_MS / 1000);
    }

    @MessageMapping(MinionMovingReceive.class)
    public void handleMinionsMovingRequest(MinionMovingReceive request, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short requestingSlot = ChannelManager.getSlotByChannel(channel);

        if (gameId == null || requestingSlot == null) {
            log.warn("Game ID or slot not found for channel when processing minion position");
            return;
        }

        GameState gameState = gameCoordinator.getGameState(gameId);
        if (gameState == null) {
            log.warn("No game state found for game ID: {}", gameId);
            return;
        }

        // Process each minion position and spread them out
        List<String> minionIds = request.getMinionIds();
        if (minionIds == null || minionIds.isEmpty()) {
            log.warn("No minion IDs provided in request");
            return;
        }

        // Get the original position from the request
        Vector2 originalMoveToPosition = new Vector2(request.getX(), request.getY());

        // Generate spread positions for all minions around the original position
        List<Vector2> spreadMoveToPositions = spreadMinionPositions(
                minionIds, originalMoveToPosition, gameState);

        // Apply the spread positions to each minion
        for (int i = 0; i < minionIds.size() && i < spreadMoveToPositions.size(); i++) {
            String minionId = minionIds.get(i);
            Vector2 moveToPosition = spreadMoveToPositions.get(i);

            // Verify the minion belongs to the requesting slot for security
            Entity entity = gameState.getEntityByStringId(minionId);
            if (entity == null || !(entity instanceof Minion)) {
                log.warn("Minion {} not found or invalid type", minionId);
                continue;
            }

            Minion minion = (Minion) entity;
            if (minion.getOwnerSlot().getSlotNumber() != requestingSlot) {
                log.warn("Player {} attempted to move minion {} owned by slot {}",
                        requestingSlot, minionId, minion.getOwnerSlot().getSlotNumber());
                continue;
            }

            moveService.setMove(minion, moveToPosition, true);

            log.debug("Moved minion {} to spread position {}", minionId, moveToPosition);
        }

        log.info("Processed {} minion positions with collision avoidance for game {}", minionIds.size(), gameId);
    }

    /**
     * Clean up expired cooldowns for a specific game to prevent memory leaks
     */
    public void cleanupGameCooldowns(String gameId) {
        long currentTime = System.currentTimeMillis();
        rateLimiter.entrySet()
                .removeIf(entry -> entry.getKey().startsWith(gameId + ":") && entry.getValue() <= currentTime);
        log.debug("Cleaned up expired cooldowns for game: {}", gameId);
    }

    /**
     * Get remaining cooldown time for a specific minion type and slot
     * 
     * @return remaining cooldown in milliseconds, or 0 if no cooldown
     */
    public long getRemainingCooldown(String gameId, short slot, MinionEnum minionType) {
        String cooldownKey = gameId + ":" + slot + ":" + minionType;
        Long cooldownEndTime = rateLimiter.get(cooldownKey);

        if (cooldownEndTime == null) {
            return 0;
        }

        long remaining = cooldownEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    // private Vector2 getMinionPositionForSlot(GameState gameState, short
    // ownerSlot) {
    // try {
    // GameMapInfo gameMap = gameState.getGameMap();
    // if (gameMap == null) {
    // log.warn("GameMap not found in GameState");
    // return null;
    // }

    // SlotInfo slotInfo = gameMap.getSlot2SlotInfo().get(ownerSlot);
    // if (slotInfo == null) {
    // log.warn("SlotInfo not found for slot: {}", ownerSlot);
    // return null;
    // }

    // List<Vector2> minionPositions = slotInfo.getMinionPositions();
    // if (minionPositions == null || minionPositions.size() < 4) {
    // log.warn("minion_positions requires at least 4 points to define the
    // rectangle.");
    // return null;
    // }

    // // Assuming the points define a rectangle, find the min/max X and Y
    // float minX = Float.MAX_VALUE, maxX = -1000.f;
    // float minY = Float.MAX_VALUE, maxY = -1000.f;

    // // Calculate the min and max for both X and Y coordinates
    // for (Vector2 pos : minionPositions) {
    // log.info("Minion position: {}", pos);
    // minX = Math.min(minX, pos.x());
    // maxX = Math.max(maxX, pos.x());
    // minY = Math.min(minY, pos.y());
    // maxY = Math.max(maxY, pos.y());
    // }

    // log.info("Minion position bounds: minX={}, maxX={}, minY={}, maxY={}", minX,
    // maxX, minY, maxY);
    // float randomX = minX + (float) (Math.random() * (maxX - minX));
    // float randomY = minY + (float) (Math.random() * (maxY - minY));
    // log.info("Random minion position: ({}, {})", randomX, randomY);

    // return new Vector2(randomX, randomY);
    // } catch (Exception e) {
    // log.error("Error getting minion positions for slot {}: {}", ownerSlot,
    // e.getMessage(), e);
    // }

    // return null;
    // }

    private ChannelFuture broadcastMinionSpawn(Minion minion, Channel channel) {

        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(minion.getGameId());
        if (gameChannels == null || gameChannels.isEmpty()) {
            log.warn("No active channels found for game ID: {}", minion.getGameId());
            return null;
        }

        // Calculate rotation based on position to the center point (0,0)
        // float rotate = (float) Math.atan2(position.y(), position.x());
        float rotate = gameStateService.getSpawnRotate(minion.getOwnerSlot());

        // Create the MinionSpawnSend object
        MinionSpawnSend minionSpawnSend = new MinionSpawnSend(minion, rotate);

        // Broadcast the minion spawn message to all players in the game
        ChannelFuture lastFuture = null;
        for (Channel playerChannel : gameChannels) {
            if (playerChannel.isActive()) {
                lastFuture = playerChannel.writeAndFlush(minionSpawnSend);
            } else {
                log.warn("Inactive channel found for game ID: {}, slot: {}", minion.getGameState().getGameId(),
                        minion.getOwnerSlot().getSlotNumber());
            }
        }

        return lastFuture == null
                ? channel.newSucceededFuture()
                : lastFuture;
    }

    /**
     * Arranges minion positions in a spiral pattern around the center position
     * The first position remains unchanged, subsequent positions spiral outward
     * with a minimum distance of 2 cells between each position
     */
    private List<Vector2> spreadMinionPositions(List<String> minionIds, Vector2 centerPosition, GameState gameState) {
        final float CELL_DISTANCE = 2.0f; // 2 cells distance between positions

        List<Vector2> spreadPositions = new ArrayList<>();

        if (minionIds.isEmpty()) {
            return spreadPositions;
        }

        // First position remains the same (center position)
        spreadPositions.add(centerPosition);

        log.debug("Center position (first minion): {}", centerPosition);

        // If only one minion, return early
        if (minionIds.size() == 1) {
            return spreadPositions;
        }

        // Generate spiral positions for the remaining minions
        // Spiral directions: Left -> Up -> Right -> Down (counter-clockwise)
        Vector2[] directions = {
                new Vector2(-CELL_DISTANCE, 0), // Left
                new Vector2(0, CELL_DISTANCE), // Up
                new Vector2(CELL_DISTANCE, 0), // Right
                new Vector2(0, -CELL_DISTANCE) // Down
        };

        Vector2 currentPosition = centerPosition;
        int directionIndex = 0; // Start with Left direction
        int stepsInCurrentDirection = 1; // How many steps to take in current direction
        int stepsTaken = 0; // Steps taken in current direction
        int stepsBeforeDirectionChange = 1; // After how many steps to change direction

        // Place remaining minions in spiral pattern
        for (int i = 1; i < minionIds.size(); i++) {
            // Move to next position in current direction
            Vector2 direction = directions[directionIndex];
            currentPosition = currentPosition.add(direction);
            spreadPositions.add(currentPosition);

            log.trace("Minion {} positioned at {} (direction: {})",
                    i, currentPosition, getDirectionName(directionIndex));

            stepsTaken++;

            // Check if we need to change direction
            if (stepsTaken >= stepsInCurrentDirection) {
                directionIndex = (directionIndex + 1) % 4; // Move to next direction
                stepsTaken = 0;

                // After completing Left and Right directions, increase steps
                if (directionIndex == 2 || directionIndex == 0) { // Right or Left
                    stepsBeforeDirectionChange++;
                    stepsInCurrentDirection = stepsBeforeDirectionChange;
                } else { // Up or Down
                    stepsInCurrentDirection = stepsBeforeDirectionChange;
                }
            }
        }

        log.debug("Generated {} spread positions in spiral pattern", spreadPositions.size());
        return spreadPositions;
    }

    /**
     * Helper method to get direction name for logging
     */
    private String getDirectionName(int directionIndex) {
        switch (directionIndex) {
            case 0:
                return "LEFT";
            case 1:
                return "UP";
            case 2:
                return "RIGHT";
            case 3:
                return "DOWN";
            default:
                return "UNKNOWN";
        }
    }
}
