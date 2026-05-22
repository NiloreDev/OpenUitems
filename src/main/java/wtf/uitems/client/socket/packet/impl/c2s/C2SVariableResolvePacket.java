package wtf.uitems.client.socket.packet.impl.c2s;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SVariableResolvePacket implements C2SPacket {

    private final String key;

    public C2SVariableResolvePacket(final String key) {
        this.key = key;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.key);
    }

    @Override
    public int id() {
        return 11;
    }

}
