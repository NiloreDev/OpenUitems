package wtf.uitems.client.socket.packet.impl.c2s;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SHandshakePacket implements C2SPacket {

    private final String agent, releaseChannel, sessionToken, hardwareId;

    public C2SHandshakePacket(final String agent, final String releaseChannel, final String sessionToken, final String hardwareId) {
        this.agent = agent;
        this.releaseChannel = releaseChannel;
        this.sessionToken = sessionToken;
        this.hardwareId = hardwareId;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.agent);
        writer.writeString(this.releaseChannel);
        writer.writeString(this.sessionToken);
        writer.writeString(this.hardwareId);
    }

    @Override
    public int id() {
        return 2;
    }

}
