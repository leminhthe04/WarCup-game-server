// package com.server.game.service.defense;

// import com.server.game.factory.AttackContextFactory;
// import com.server.game.model.entity.Entity;
// import com.server.game.model.entity.GameState;
// import com.server.game.model.entity.Minion;
// import com.server.game.model.map.component.Vector2;
// import com.server.game.service.attack.AttackService;
// import com.server.game.service.move.MoveService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// import org.springframework.stereotype.Service;

// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.List;
// import java.util.Objects;
// import java.util.Optional;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class DefensiveStanceService {

//     private final MoveService moveService;
//     private final AttackService attackService;
//     private final AttackContextFactory attackContextFactory;

//     public void updateDefensiveStances(GameState gameState) {

//         if (gameState == null) {
//             return;
//         }

//         try {

//             // Snapshot to avoid concurrent modification / inconsistent iteration
//             List<Entity> entitiesSnapshot = new ArrayList<>(gameState.getEntities());

//             entitiesSnapshot.stream()
//                     .filter(Objects::nonNull)
//                     .filter(Minion.class::isInstance)
//                     .map(Minion.class::cast)
//                     .forEach(minion -> {
//                         try {

//                             if (!isValidMinion(minion)) {
//                                 return;
//                             }

//                             // Re-enable defensive stance if needed
//                             minion.checkAndEnableDefensiveStance();

//                             // Process defensive AI
//                             processMinionDefense(minion, entitiesSnapshot);

//                         } catch (Throwable t) {

//                             try {
//                                 log.error(
//                                         "Error while processing minion defense. gameId={}, minionId={}",
//                                         gameState.getGameId(),
//                                         safeEntityId(minion),
//                                         t);
//                             } catch (Throwable ignored) {
//                             }
//                         }
//                     });

//         } catch (Throwable t) {

//             try {
//                 log.error(
//                         "Error in updateDefensiveStances. gameId={}",
//                         gameState.getGameId(),
//                         t);
//             } catch (Throwable ignored) {
//             }
//         }
//     }

//     private void processMinionDefense(
//             Minion minion,
//             List<Entity> entitiesSnapshot) {

//         // Defensive stance disabled
//         if (!minion.isInDefensiveStance()) {
//             return;
//         }

//         // Invalid state
//         if (!hasValidPosition(minion)) {
//             clearCombatState(minion);
//             return;
//         }

//         // -----------------------------------------
//         // Outside defense range -> return home
//         // -----------------------------------------
//         if (!safeIsWithinOwnDefenseRange(minion)) {

//             if (minion.getDefensiveTarget() != null) {

//                 log.debug(
//                         "Minion {} outside defense range. Clearing target.",
//                         safeEntityId(minion));

//                 clearCombatState(minion);
//             }

//             safeMoveToDefensePosition(minion);

//             return;
//         }

//         // -----------------------------------------
//         // Existing target handling
//         // -----------------------------------------
//         Entity target = minion.getDefensiveTarget();

//         if (target != null) {

//             if (!isValidCombatTarget(minion, target)) {

//                 clearCombatState(minion);

//                 safeMoveToDefensePosition(minion);

//                 log.trace("Minion {} disengaged from invalid target.",
//                         safeEntityId(minion));

//                 return;
//             }

//             double distance = minion.getCurrentPosition().distance(target.getCurrentPosition());

//             boolean targetInRange = distance <= minion.getDetectionRange();

//             if (!target.isAlive() || !targetInRange) {

//                 clearCombatState(minion);

//                 safeMoveToDefensePosition(minion);

//                 log.trace(
//                         "Minion {} target out of range/dead.",
//                         safeEntityId(minion));

//                 return;
//             }

//             // Continue attacking
//             if (!minion.isAttacking()) {

//                 try {

//                     attackService.setAttack(
//                             attackContextFactory.createAttackContext(
//                                     minion,
//                                     target));

//                 } catch (Throwable t) {

//                     log.warn(
//                             "Failed to set attack context. attacker={}, target={}",
//                             safeEntityId(minion),
//                             safeEntityId(target),
//                             t);
//                 }
//             }

//             return;
//         }

//         // -----------------------------------------
//         // Find new target
//         // -----------------------------------------
//         findNearestEnemyInDetectionRange(
//                 minion,
//                 entitiesSnapshot).ifPresent(enemy -> {

//                     try {

//                         minion.setDefensiveTarget(enemy);

//                         attackService.setAttack(
//                                 attackContextFactory.createAttackContext(
//                                         minion,
//                                         enemy));

//                         log.trace(
//                                 "Minion {} detected enemy {}",
//                                 safeEntityId(minion),
//                                 safeEntityId(enemy));

//                     } catch (Throwable t) {

//                         log.warn(
//                                 "Failed to engage target. attacker={}, target={}",
//                                 safeEntityId(minion),
//                                 safeEntityId(enemy),
//                                 t);

//                         clearCombatState(minion);
//                     }
//                 });

//         // -----------------------------------------
//         // Return to defense post if idle
//         // -----------------------------------------
//         if (minion.getDefensiveTarget() == null
//                 && !minion.isMoving()) {

//             Vector2 currentPos = minion.getCurrentPosition();
//             Vector2 defensePos = minion.getDefensePosition();

//             if (currentPos == null || defensePos == null) {
//                 return;
//             }

//             double distance = currentPos.distance(defensePos);

//             if (distance > 0.5f) {

//                 log.trace(
//                         "Minion {} returning to defense post.",
//                         safeEntityId(minion));

//                 safeMoveToDefensePosition(minion);
//             }
//         }
//     }

//     private Optional<Entity> findNearestEnemyInDetectionRange(
//             Minion minion,
//             List<Entity> entitiesSnapshot) {

//         if (!hasValidPosition(minion)) {
//             return Optional.empty();
//         }

//         Vector2 minionPos = minion.getCurrentPosition();

//         return entitiesSnapshot.stream()
//                 .filter(Objects::nonNull)
//                 .filter(Entity::isAlive)
//                 .filter(this::hasValidPosition)
//                 .filter(e -> e.getOwnerSlot() != null)
//                 .filter(e -> minion.getOwnerSlot() != null)
//                 .filter(e -> e.getOwnerSlot().getSlotNumber() != minion.getOwnerSlot().getSlotNumber())
//                 .filter(e -> {

//                     double distance = minionPos.distance(e.getCurrentPosition());

//                     return distance <= minion.getDetectionRange();
//                 })
//                 .min(
//                         Comparator.comparingDouble(e -> minionPos.distance(e.getCurrentPosition())));
//     }

//     // =========================================================
//     // Utility methods
//     // =========================================================

//     private boolean isValidMinion(Minion minion) {

//         return minion != null
//                 && minion.isAlive()
//                 && hasValidPosition(minion)
//                 && minion.getOwnerSlot() != null;
//     }

//     private boolean isValidCombatTarget(
//             Minion attacker,
//             Entity target) {

//         return target != null
//                 && target.isAlive()
//                 && hasValidPosition(attacker)
//                 && hasValidPosition(target)
//                 && target.getOwnerSlot() != null
//                 && attacker.getOwnerSlot() != null
//                 && target.getOwnerSlot().getSlotNumber() != attacker.getOwnerSlot().getSlotNumber();
//     }

//     private boolean hasValidPosition(Entity entity) {

//         return entity != null
//                 && entity.getCurrentPosition() != null;
//     }

//     private boolean safeIsWithinOwnDefenseRange(Minion minion) {

//         try {
//             return minion.isWithinOwnDefenseRange();
//         } catch (Throwable t) {

//             log.warn("Failed checking defense range for minion={}",
//                     safeEntityId(minion),
//                     t);

//             return false;
//         }
//     }

//     private void safeMoveToDefensePosition(Minion minion) {

//         try {

//             Vector2 defensePosition = minion.getDefensePosition();

//             if (defensePosition == null) {
//                 return;
//             }

//             moveService.setMove(
//                     minion,
//                     defensePosition,
//                     true);

//         } catch (Throwable t) {

//             log.warn(
//                     "Failed moving minion back to defense position. minion={}",
//                     safeEntityId(minion),
//                     t);
//         }
//     }

//     private void clearCombatState(Minion minion) {

//         try {

//             minion.setDefensiveTarget(null);

//             if (minion.getAttackComponent() != null) {
//                 minion.getAttackComponent().setAttackContext(null);
//             }

//         } catch (Throwable t) {

//             log.warn(
//                     "Failed clearing combat state for minion={}",
//                     safeEntityId(minion),
//                     t);
//         }
//     }

//     private String safeEntityId(Entity entity) {

//         try {

//             if (entity == null) {
//                 return "null";
//             }

//             return entity.getStringId();

//         } catch (Throwable t) {
//             return "unknown";
//         }
//     }
// }