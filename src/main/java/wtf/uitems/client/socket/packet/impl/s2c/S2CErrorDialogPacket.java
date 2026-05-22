package wtf.uitems.client.socket.packet.impl.s2c;

import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.utility.misc.system.DialogUtility;
import wtf.uitems.utility.socket.buffer.BufferReader;
public final class S2CErrorDialogPacket implements S2CPacket {

    private final String title, description;

    public S2CErrorDialogPacket(final BufferReader reader) throws Exception {
        this.title = reader.readString();
        this.description = reader.readString();
    }

    @Override
    public void handle() {
        DialogUtility.notify("ok", "error", this.title, this.description);
        Runtime.getRuntime().halt(1);

        throw new RuntimeException(this.title + ": " + this.description);
    }

    @Override
    public int id() {
        return 2;
    }

}
