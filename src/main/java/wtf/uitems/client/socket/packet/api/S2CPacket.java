package wtf.uitems.client.socket.packet.api;

public interface S2CPacket extends Packet {
    default void handle() throws Exception {
    }
}
