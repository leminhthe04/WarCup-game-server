package com.server.game.model.entity;

import java.util.UUID;

import com.server.game.factory.SkillFactory;
import com.server.game.model.entity.attackStrategy.ChampionAttackStrategy;
import com.server.game.model.entity.building.Building;
import com.server.game.model.entity.component.AttackComponent;
import com.server.game.model.entity.component.HealthComponent;
import com.server.game.model.entity.component.MovingComponent;
import com.server.game.model.entity.component.attributeComponent.ChampionAttributeComponent;
import com.server.game.model.entity.component.skillComponent.DurationSkillComponent;
import com.server.game.model.entity.component.skillComponent.SkillComponent;
import com.server.game.model.entity.context.AttackContext;
import com.server.game.model.entity.context.CastSkillContext;
import com.server.game.model.entity.entityIface.SkillReceivable;
import com.server.game.resource.modelInfo.ChampionInfo;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;
import com.server.game.util.NPCPriorityEnum;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = false, exclude = "skillComponent")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Slf4j
public final class Champion extends DependentEntity implements SkillReceivable {

    final ChampionEnum championEnum;
    final String name;
    final String role;

    @Delegate
    final ChampionAttributeComponent attributeComponent;
    @Delegate
    final HealthComponent healthComponent;
    @Delegate
    final MovingComponent movingComponent;
    @Delegate
    final SkillComponent skillComponent;
    @Delegate
    final AttackComponent attackComponent;

    public Champion(ChampionInfo championDB, SlotState ownerSlot, SkillFactory skillFactory) {

        super("champion_" + UUID.randomUUID().toString(), ownerSlot);

        this.championEnum = ChampionEnum.fromShort(championDB.getId());
        this.name = championDB.getName();
        this.role = championDB.getRole();
        this.attributeComponent = new ChampionAttributeComponent(
                championDB.getStats().getDefense(),
                championDB.getStats().getGoldMineDamage());
        this.movingComponent = new MovingComponent(
                this,
                gameState.getSpawnPosition(ownerSlot),
                championDB.getStats().getMoveSpeed());
        this.healthComponent = new HealthComponent(
                championDB.getStats().getInitHP());
        this.skillComponent = skillFactory.createSkillFor(
                this,
                championDB.getAbility());
        this.attackComponent = new AttackComponent(
                this,
                championDB.getStats().getAttack(),
                championDB.getStats().getAttackSpeed(),
                championDB.getStats().getAttackRange(),
                new ChampionAttackStrategy());

        this.addAllComponents();
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(ChampionAttributeComponent.class, attributeComponent);
        this.addComponent(MovingComponent.class, movingComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(SkillComponent.class, skillComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

    @Override
    public void beforeUpdatePosition() {
        // log.info("Call beforeUpdatePosition for champion, call super method...");
        super.beforeUpdatePosition();
    }

    @Override
    public void afterUpdatePosition() {
        // log.info("Call afterUpdatePosition for champion, check in playground and call
        // super method...");

        this.checkInPlayground();

        super.afterUpdatePosition();

    }

    private void checkInPlayground() {

        boolean nextInPlayground = this.checkInPlayground(
                this.getGameState().getGameMap().getPlayground());

        if (nextInPlayground != this.isInPlayground()) {
            this.toggleInPlaygroundFlag(); // Toggle the state

            this.getGameStateService()
                    .sendInPlaygroundUpdateMessage(
                            this.getGameState(),
                            this.getOwnerSlot(),
                            this.isInPlayground());
        }
    }

    @Override // from Attackable implemented by Entity
    public boolean receiveAttack(AttackContext ctx) {

        // 2. Process the attack and calculate damage
        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);

        // 3. Send health update for the target
        ctx.addActualDamage(actualDamage);
        ctx.getGameStateService().sendHealthUpdate(
                ctx.getGameId(), this, actualDamage, ctx.getTimestamp());

        // 4. Check if champion died and handle death/respawn logic
        if (this.isAlive()) {
            return true;
        }

        this.handleDeath(ctx.getAttacker());

        return true;
    }

    @Override // from SkillReceivable interface
    public void receiveSkillDamage(CastSkillContext ctx) {
        // 2. Process the skill damage and calculate actual damage
        Integer actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);

        // 3. Send health update for the target
        ctx.addActualDamage(actualDamage);
        ctx.getGameStateService().sendHealthUpdate(
                ctx.getGameId(), this, actualDamage, ctx.getTimestamp());

        if (this.isAlive()) {
            return;
        }

        this.handleDeath(ctx.getCaster());
    }

    @Override
    protected void handleDeath(Entity killer) {
        log.info("Champion {} is dead, handling death logic...", this.getName());

        GameStateService gameStateService = this.getGameStateService();

        gameStateService.setChampionDead(this);

        gameStateService.setStopAttacking(this);
        gameStateService.setStopMoving(this, true);

        gameStateService.handleStolingGold(this, killer);

        gameStateService.scheduleChampionRespawn(this.ownerSlot, (short) 3);
        gameStateService.sendEntityDeathMessage(this.getGameState(), this.getStringId());
    }

    public void useSkill(CastSkillContext ctx) {
        this.skillComponent.use(ctx);
    }

    public void updateDurationSkill() {
        if (this.skillComponent instanceof DurationSkillComponent durationSkillComponent) {
            durationSkillComponent.updatePerTick();
        }
    }

    @Override
    public NPCPriorityEnum getNpcPriorityEnum() {
        if (!this.didPerformAttack()) {
            return NPCPriorityEnum.CHAMPION_FREE;
        }

        Entity currentAttackEntity = this.getCurrentAttackEntity();

        if (currentAttackEntity instanceof Champion) {
            return NPCPriorityEnum.CHAMPION_ATKG_CHAMPION;
        }
        if (currentAttackEntity instanceof Minion) {
            return NPCPriorityEnum.CHAMPION_ATKG_MINION;
        }
        if (currentAttackEntity instanceof Building) {
            return NPCPriorityEnum.CHAMPION_ATKG_BUILDING;
        }

        return NPCPriorityEnum.CHAMPION_FREE; // fall back

    };
}
