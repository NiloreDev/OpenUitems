package wtf.uitems.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor()
    int getItemUseCooldown();

    @Accessor()
    void setItemUseCooldown(int ticks);

    @Invoker
    void callDoItemUse();

    @Mutable
    @Accessor("session")
    void setSession(Session session);

}
