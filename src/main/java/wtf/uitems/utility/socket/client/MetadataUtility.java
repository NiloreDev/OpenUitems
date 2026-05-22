package wtf.uitems.utility.socket.client;

import net.fabricmc.loader.api.FabricLoader;
import wtf.uitems.client.ReleaseInfo;
public final class MetadataUtility {

    private MetadataUtility() {
    }

    public static String getAgent() {
        return FabricLoader.getInstance().getModContainer("uitems").orElseThrow().getMetadata().getId() + "/" + ReleaseInfo.VERSION;
    }

}
