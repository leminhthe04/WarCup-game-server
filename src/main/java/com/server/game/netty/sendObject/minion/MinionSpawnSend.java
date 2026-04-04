package com.server.game.netty.sendObject.minion;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.model.entity.Minion;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinionSpawnSend implements TLVEncodable {
    String troopId; // Unique identifier for the minion
    short troopType;
    short ownerSlot;
    Vector2 spawnPosition; 
    float rotate;
    int maxHP;
    long timestamp;

    public MinionSpawnSend(Minion minion, float rotate) {
        this.troopId = minion.getStringId();
        this.troopType = minion.getMinionEnum().toShort();
        this.ownerSlot = minion.getOwnerSlot().getSlotNumber();
        this.spawnPosition = minion.getCurrentPosition();
        this.rotate = rotate;
        this.maxHP = minion.getMaxHP();
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.TROOP_SPAWN_SEND;
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            byte[] troopIdBytes = troopId != null ? troopId.getBytes("UTF-8") : new byte[0];
            short troopIdLength = (short) troopIdBytes.length;
            dos.writeShort(troopIdLength);
            if (troopIdLength > 0) {
                dos.write(troopIdBytes);
            }
            dos.writeShort(troopType);
            dos.writeShort(ownerSlot);
            dos.writeFloat(spawnPosition.x());
            dos.writeFloat(spawnPosition.y());
            dos.writeFloat(rotate);
            dos.writeInt(maxHP);
            dos.writeLong(timestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
