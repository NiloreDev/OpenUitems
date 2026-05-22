package wtf.uitems.client.socket.data;

import wtf.uitems.client.socket.ClientSocket;
public final class VariableCache {

    private final ClientSocket socket;

    public VariableCache(final ClientSocket socket) {
        this.socket = socket;
    }

    public void clear() {
    }

    public String getString(final String key) {
        // Verification bypassed: always return null so callers get default values
        return null;
    }

    public boolean getBoolean(final String key) {
        return false;
    }

    public int getInt(final String key) {
        return 0;
    }

    public double getDouble(final String key) {
        return 0.0D;
    }

    public long getLong(final String key) {
        return 0L;
    }

}
