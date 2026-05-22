package wtf.uitems.client.socket.packet.impl.s2c.config;

import net.minecraft.util.Formatting;
import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.utility.data.SaveUtility;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.socket.buffer.BufferReader;
public final class S2CConfigLoadPacket implements S2CPacket {

    private final String configName, configData;

    public S2CConfigLoadPacket(final BufferReader reader) throws Exception {
        this.configName = reader.readString();
        this.configData = reader.readString();
    }

    @Override
    public void handle() throws Exception {
        if (SaveUtility.loadConfig(configData)) {
            ChatUtility.success(Formatting.YELLOW + configName + Formatting.GRAY + " has been successfully loaded!");
        } else {
            ChatUtility.error("Your config could not be loaded.");
        }
    }

    @Override
    public int id() {
        return 8;
    }
}
