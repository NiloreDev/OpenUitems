package wtf.uitems.client.socket.packet.impl.c2s.config;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SConfigLoadPacket implements C2SPacket {

    private final String configName;

    public C2SConfigLoadPacket(final String configName) {
        this.configName = configName;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(configName);
    }

    @Override
    public int id() {
        return 8;
    }

}
