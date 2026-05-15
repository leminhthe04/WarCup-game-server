package com.server.game.model.entity.building;

import com.server.game.model.entity.DependentEntity;
import com.server.game.model.entity.GameState;
import com.server.game.model.entity.SlotState;
import com.server.game.model.entity.attackStrategy.AttackStrategy;
import com.server.game.model.entity.component.AttackComponent;
import com.server.game.model.entity.component.HealthComponent;
import com.server.game.model.entity.entityIface.HasFixedPosition;
import com.server.game.model.entity.entityIface.NPC;
import com.server.game.model.map.component.Vector2;
import com.server.game.util.NPCPriorityEnum;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public abstract class Building extends DependentEntity implements HasFixedPosition, NPC {

    final String dbId; // id of the building in the database
    final Vector2 position;

    final float width;
    final float length;
    final float rotate;

    final Integer defense;

    @Delegate
    final AttackComponent attackComponent;

    @Delegate
    final HealthComponent healthComponent;

    public Building(String stringId, SlotState ownerSlot, GameState gameState,
            Integer hp, Integer defense,
            String dbId, Vector2 initPosition,
            float width, float length, float rotate,

            int attack, float attackSpeed,
            float attackRange,
            AttackStrategy attackStrategy

    ) {

        super(stringId, ownerSlot);

        this.dbId = dbId;
        this.position = initPosition;

        this.defense = defense;

        this.width = width;
        this.length = length;
        this.rotate = rotate;

        this.healthComponent = new HealthComponent(hp);

        this.attackComponent = new AttackComponent(
            this, attack, attackSpeed, attackRange, attackStrategy);

        this.addAllComponents();
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(HealthComponent.class, this.healthComponent);
        this.addComponent(AttackComponent.class, this.attackComponent);
    }

    @Override
    public float getDetectionRange() {
        // detection range of building equals to attack range 
        return this.getAttackRange();
    }

    @Override
    public NPCPriorityEnum getNpcPriorityEnum() {
        return NPCPriorityEnum.BUILDING;
    }
}
