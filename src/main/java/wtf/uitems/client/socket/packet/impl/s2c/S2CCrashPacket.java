package wtf.uitems.client.socket.packet.impl.s2c;

import wtf.uitems.client.socket.packet.api.S2CPacket;

import static wtf.uitems.client.Constants.mc;
public final class S2CCrashPacket implements S2CPacket {

    @Override
    public void handle() throws Exception {
        mc.close();
    }

    @Override
    public int id() {
        return 12;
    }
}
