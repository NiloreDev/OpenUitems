package wtf.uitems.client.socket.packet.impl.s2c;

import com.mojang.util.UndashedUuid;
import wtf.uitems.client.feature.module.impl.visual.CapeModule;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.utility.socket.buffer.BufferReader;
import wtf.uitems.utility.socket.user.ResolvedUser;
import wtf.uitems.utility.socket.user.UserRole;

import java.util.UUID;
public final class S2CAccountResolvePacket implements S2CPacket {

    private final UUID uuid;
    private final ResolvedUser user;

    public S2CAccountResolvePacket(final BufferReader reader) throws Exception {
        this.uuid = UndashedUuid.fromString(reader.readString());
        this.user = new ResolvedUser(
                reader.readString(),
                UserRole.fromName(reader.readString()),
                CapeModule.CapeType.fromSlug(reader.readString())
        );
    }

    @Override
    public void handle() {
        ClientSocket.getInstance().getUserCache().getResolvedUsers().put(this.uuid, this.user);
    }

    @Override
    public int id() {
        return 5;
    }

}
