package wtf.uitems.client.socket.packet.impl.s2c;

import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.utility.socket.buffer.BufferReader;
public final class S2CVariableResolvePacket implements S2CPacket {

    private final String key, value;

    public S2CVariableResolvePacket(final BufferReader reader) throws Exception {
        this.key = reader.readString();
        this.value = reader.readString(false);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int id() {
        return 11;
    }

}
