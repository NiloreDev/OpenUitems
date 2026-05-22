package wtf.uitems.client.socket.packet.impl.s2c;

import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.socket.buffer.BufferReader;

import static wtf.uitems.client.Constants.mc;
public final class S2CTitlePacket implements S2CPacket {

    private final String message;
    private final int fadeInTicks, stayTicks, fadeOutTicks;

    public S2CTitlePacket(final BufferReader reader) throws Exception {
        this.message = reader.readString();
        this.fadeInTicks = reader.readInt();
        this.stayTicks = reader.readInt();
        this.fadeOutTicks = reader.readInt();
    }

    @Override
    public void handle() throws Exception {
        mc.inGameHud.setTitleTicks(fadeInTicks, stayTicks, fadeOutTicks);
        mc.inGameHud.setTitle(ChatUtility.translateAlternateColorCodes(message));
    }

    @Override
    public int id() {
        return 13;
    }
}
