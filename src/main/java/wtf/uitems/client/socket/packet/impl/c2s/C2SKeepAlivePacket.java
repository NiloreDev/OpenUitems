package wtf.uitems.client.socket.packet.impl.c2s;

import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.socket.buffer.BufferWriter;
import wtf.uitems.utility.socket.buffer.BufferWriterConsumer;
public final class C2SKeepAlivePacket implements C2SPacket {

    private final BufferWriterConsumer challenge;

    public C2SKeepAlivePacket(final BufferWriterConsumer challenge) {
        this.challenge = challenge;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeInt(ClientSocket.getInstance().getKeepAliveCount()); // client count
        this.challenge.accept(writer);
    }

    @Override
    public int id() {
        return 0;
    }

}
