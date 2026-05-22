package wtf.uitems.client.socket.packet.impl.c2s.config;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SConfigUploadPacket implements C2SPacket {

    private final String configName, configData;

    public C2SConfigUploadPacket(final String configName, final String configData) {
        this.configName = configName;
        this.configData = configData;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(configName);
        writer.writeString(configData);
    }

    @Override
    public int id() {
        return 9;
    }

}
