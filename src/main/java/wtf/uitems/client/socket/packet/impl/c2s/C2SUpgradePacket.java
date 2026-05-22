package wtf.uitems.client.socket.packet.impl.c2s;

import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.utility.misc.StringUtility;
import wtf.uitems.utility.socket.buffer.BufferWriter;
public final class C2SUpgradePacket implements C2SPacket {

    private final String encryptedAESKey;

    public C2SUpgradePacket(final String encryptedAESKey) {
        this.encryptedAESKey = encryptedAESKey;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(StringUtility.rotate(23, encryptedAESKey));
    }

    @Override
    public int id() {
        return 1;
    }

}
