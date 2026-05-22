package wtf.uitems.client.socket.packet.api;

import wtf.uitems.utility.socket.buffer.BufferWriter;

public interface C2SPacket extends Packet {
    default void serialize(final BufferWriter writer) throws Exception {
    }
}
