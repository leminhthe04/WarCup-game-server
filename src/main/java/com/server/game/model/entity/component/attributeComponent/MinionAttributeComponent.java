package com.server.game.model.entity.component.attributeComponent;


import lombok.Getter;

@Getter
public class MinionAttributeComponent extends AttributeComponent {
    protected float detectionRange;
    protected Integer healingPower; // Optional for healers
    protected Float healingRange; // Optional for healers
    protected int cost;

    public MinionAttributeComponent(
        int defense, 
        float detectionRange, 
        Integer healingPower, 
        Float healingRange, 
        int cost
    ) {
        super(defense);
        this.detectionRange = detectionRange;
        this.healingPower = healingPower;
        this.healingRange = healingRange;
        this.cost = cost;
    }
}
