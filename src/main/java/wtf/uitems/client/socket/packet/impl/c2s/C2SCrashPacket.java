package wtf.uitems.client.socket.packet.impl.c2s;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SCrashPacket implements C2SPacket {

    private final String username;

    public C2SCrashPacket(final String username) {
        this.username = username.length() > 24 ? username.substring(0, 24) : username;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.username);
    }

    @Override
    public int id() {
        return 12;
    }
}
