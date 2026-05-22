package wtf.uitems.client.feature.module.impl.visual;

import net.minecraft.util.Identifier;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.socket.ClientSocket;

public final class CapeModule extends Module {

    private final ModeProperty<CapeType> type = new ModeProperty<>("Type", CapeType.UITEMS);

    public CapeModule() {
        super("Cape", "Gives you a cape of your choosing.", ModuleCategory.VISUAL);
        this.type.addUpdateListener(() -> ClientSocket.getInstance().syncAccount());
        this.addProperties(type);
    }

    public CapeType getType() {
        return type.getValue();
    }

    @Override
    public String getSuffix() {
        return this.getType().toString();
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        ClientSocket.getInstance().syncAccount();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        ClientSocket.getInstance().syncAccount();
    }

    public enum CapeType {
        UITEMS("Uitems");

        private final String name, slug;
        private final Identifier identifier;

        CapeType(final String name) {
            this.name = name;
            this.slug = name.replace(' ', '-').toLowerCase();
            this.identifier = Identifier.of("uitems", "capes/" + this.slug + ".png");
        }

        public String getSlug() {
            return slug;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        @Override
        public String toString() {
            return name;
        }

        public static CapeType fromSlug(final String slug) {
            for (final CapeType type : values()) {
                if (type.slug.equals(slug)) {
                    return type;
                }
            }
            return null;
        }
    }

}
