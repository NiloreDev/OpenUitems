package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import wtf.uitems.client.Constants;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.JoinWorldEvent;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiBotModule extends Module {
    private static final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private static final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private static final Set<Integer> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();

    private final NumberProperty respawnTimeValue = new NumberProperty("Respawn Time", 2500.0, 0.0, 10000.0, 100.0);
    private final NumberProperty minTicks = new NumberProperty("Min Ticks", 20.0, 0.0, 100.0, 1.0);

    public AntiBotModule() {
        super("AntiBot", "Prevents bots from attacking you.", ModuleCategory.COMBAT);
        addProperties(respawnTimeValue, minTicks);
    }

    public static boolean isBedWarsBot(Entity entity) {
        AntiBotModule module = OpalClient.getInstance().getModuleRepository().getModule(AntiBotModule.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }
        
        if (module.respawnTimeValue.getValue().floatValue() >= 1.0F && respawnTime.containsKey(entity.getUuid())) {
             if ((float) (System.currentTimeMillis() - respawnTime.get(entity.getUuid())) < module.respawnTimeValue.getValue().floatValue()) {
                 return true;
             }
        }
        
        return false;
    }

    public static boolean isBot(Entity entity) {
        AntiBotModule module = OpalClient.getInstance().getModuleRepository().getModule(AntiBotModule.class);
        if (module == null || !module.isEnabled()) return false;
        
        // Tab list check
        boolean inTab = false;
        if (Constants.mc.getNetworkHandler() != null) {
            PlayerListEntry entry = Constants.mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
            if (entry != null) inTab = true;
        }
        
        if (!inTab) return true;
        
        // Entity ID detection (General bot detection logic)
        if (ids.contains(entity.getId())) return true;
        
        // Watchdog/Heypixel bot detection: flying, spinning, weird movement
        if (entity.age < module.minTicks.getValue().intValue()) {
            // Very young entities that are flying/spinning are likely bots
            // 1. Off ground but vertical velocity is near zero (floating)
            // 2. High up in the air relative to player
            if (!entity.isOnGround() && Math.abs(entity.getVelocity().y) < 0.1 && entity.getY() > Constants.mc.player.getY() + 1.0) {
                return true;
            }
            
            // 3. Invisible players that are spawning around
            if (entity.isInvisible()) {
                return true;
            }
        }
        
        // 4. Bots often have suspicious names or are not players but have player type
        if (entity.getType() == EntityType.PLAYER) {
            String name = entity.getName().getString();
            // Check for common bot name patterns (e.g. random strings, CIT-, or empty)
            if (name.isEmpty() || name.contains("CIT-") || name.length() > 16 || name.contains(" ")) {
                return true;
            }
            
            // 5. Check UUID version (some bots use version 2 or other non-standard versions)
            if (entity.getUuid().version() != 4 && entity.getUuid().version() != 1) {
                return true;
            }
        }

        return false;
    }

    public static String getDisplayNameForUuid(UUID uuid) {
        return uuidDisplayNames.get(uuid);
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (Constants.mc.world == null) return;
        Packet<?> packet = event.getPacket();

        // BedWars Bot detection
        if (packet instanceof PlayerListS2CPacket infoPacket) {
            if (infoPacket.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry entry : infoPacket.getEntries()) {
                    respawnTime.put(entry.profileId(), System.currentTimeMillis());
                }
            }
        } else if (packet instanceof EntityAnimationS2CPacket animatePacket) {
            Entity entity = Constants.mc.world.getEntityById(animatePacket.getEntityId());
            if (entity != null && animatePacket.getAnimationId() == 0) { // 0 is swing
                respawnTime.remove(entity.getUuid());
            }
        }

        // General Bot detection
        if (packet instanceof PlayerListS2CPacket infoPacket) {
            if (infoPacket.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER) || infoPacket.getActions().contains(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)) {
                for (PlayerListS2CPacket.Entry entry : infoPacket.getEntries()) {
                    if (entry.displayName() != null) {
                        UUID uuid = entry.profileId();
                        uuids.put(uuid, System.currentTimeMillis());
                        uuidDisplayNames.put(uuid, entry.displayName().getString());
                    }
                }
            }
        } else if (packet instanceof EntitySpawnS2CPacket spawnPacket) {
            if (spawnPacket.getEntityType() == EntityType.PLAYER) {
                if (uuids.containsKey(spawnPacket.getUuid())) {
                    String displayName = uuidDisplayNames.get(spawnPacket.getUuid());
                    entityIdDisplayNames.put(spawnPacket.getEntityId(), displayName);
                    uuids.remove(spawnPacket.getUuid());
                    ids.add(spawnPacket.getEntityId());
                }
            }
        } else if (packet instanceof EntitiesDestroyS2CPacket destroyPacket) {
            for (int entityId : destroyPacket.getEntityIds()) {
                if (ids.contains(entityId)) {
                    ids.remove(entityId);
                    entityIdDisplayNames.remove(entityId);
                }
            }
        }
    }

    @Subscribe
    public void onWorldLoad(JoinWorldEvent event) {
        uuidDisplayNames.clear();
        entityIdDisplayNames.clear();
        ids.clear();
        uuids.clear();
        respawnTime.clear();
    }

    @Subscribe
    public void onPreTick(PreGameTickEvent event) {
        for (Map.Entry<UUID, Long> entry : uuids.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > 500L) {
                uuids.remove(entry.getKey());
                uuidDisplayNames.remove(entry.getKey());
            }
        }
    }
}
