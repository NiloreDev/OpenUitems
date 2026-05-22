package wtf.uitems.client;

public final class ReleaseInfo {
    public static final String NAME = "Uitems";
    public static final ReleaseChannel CHANNEL = ReleaseChannel.BETA;
    public static final String VERSION = "1.3";

    public enum ReleaseChannel {
        PUBLIC("public"),
        BETA("beta"),
        Release("release"),
        DEVELOPMENT("development");

        private final String name;

        ReleaseChannel(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
