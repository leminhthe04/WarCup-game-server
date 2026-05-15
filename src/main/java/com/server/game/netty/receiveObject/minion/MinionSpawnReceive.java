package com.server.game.netty.receiveObject.minion;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.TROOP_SPAWN_RECEIVE)
@Component
public class MinionSpawnReceive implements TLVDecodable {
    short minionId;
    
    @Override
    public void decode(byte[] value) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);
            this.minionId = dis.readShort();
        } catch (Exception e) {
            throw new  RuntimeException("Cannot decode " + this.getClass().getSimpleName(), e);
        }
    }
}
