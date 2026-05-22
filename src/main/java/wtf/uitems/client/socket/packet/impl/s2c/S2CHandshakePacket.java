package wtf.uitems.client.socket.packet.impl.s2c;

import wtf.uitems.client.OpalClient;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.client.socket.packet.impl.c2s.C2SHandshakePacket;
import wtf.uitems.client.socket.packet.types.HandshakeAuthStatus;
import wtf.uitems.client.socket.packet.types.HandshakeStage;
import wtf.uitems.utility.security.SessionUtility;
import wtf.uitems.utility.socket.buffer.BufferReader;
import wtf.uitems.utility.socket.client.MetadataUtility;
import wtf.uitems.utility.socket.user.LocalUser;
import wtf.uitems.utility.socket.user.UserRole;
public final class S2CHandshakePacket implements S2CPacket {

    private final int stage;
    private int authResponseType;

    private int numericId;
    private String userName, userRole;

    public S2CHandshakePacket(final BufferReader reader) throws Exception {
        this.stage = reader.readInt();
        if (this.stage == HandshakeStage.AUTH_RESPONSE) {
            this.authResponseType = reader.readInt();
            if (this.authResponseType == HandshakeAuthStatus.ACCEPTED) {
                this.numericId = reader.readInt();
                this.userName = reader.readString();
                this.userRole = reader.readString();
            }
        }
    }

    @Override
    public void handle() {
        final ClientSocket socket = ClientSocket.getInstance();

        switch (stage) {
            case HandshakeStage.READY -> {
                if (SessionUtility.getKeystoreToken() == null && !SessionUtility.requestHandoffAuthorizationOrStop()) {
                    return;
                }

                socket.sendPacket(new C2SHandshakePacket(
                        MetadataUtility.getAgent(), ReleaseInfo.CHANNEL.toString(),
                        SessionUtility.getKeystoreToken(), ""
                ));
            }
            case HandshakeStage.AUTH_RESPONSE -> {
                switch (this.authResponseType) {
                    case HandshakeAuthStatus.INVALID_SESSION -> {
                        if (SessionUtility.requestHandoffAuthorizationOrStop()) {
                            socket.close();
                            socket.reconnect();
                        }
                    }
                    case HandshakeAuthStatus.ACCEPTED -> {
                        OpalClient.getInstance().setUser(new LocalUser(numericId, userName, UserRole.fromName(userRole)));
                        socket.setBlockMainThread(false);
                        socket.setAuthenticated(true);
                    }
                }
            }
        }
    }

    @Override
    public int id() {
        return 1;
    }

}
