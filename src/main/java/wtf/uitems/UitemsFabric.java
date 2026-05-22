package wtf.uitems;

import net.fabricmc.api.ClientModInitializer;
import wtf.uitems.client.OpalClient;
public final class UitemsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OpalClient.setInstance();
    }

}
//LL
