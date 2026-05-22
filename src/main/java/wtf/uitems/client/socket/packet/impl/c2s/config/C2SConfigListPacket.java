package wtf.uitems.client.socket.packet.impl.c2s.config;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SConfigListPacket implements C2SPacket {

    private final int packetType;

    public C2SConfigListPacket(final int packetType) {
        this.packetType = packetType;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeInt(packetType);
    }

    @Override
    public int id() {
        return 7;
    }

}
