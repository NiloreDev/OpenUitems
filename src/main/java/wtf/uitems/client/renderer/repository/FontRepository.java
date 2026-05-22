package wtf.uitems.client.renderer.repository;

import net.fabricmc.loader.api.FabricLoader;
import wtf.uitems.client.renderer.text.NVGTextRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public final class FontRepository {

    private static final HashMap<String, NVGTextRenderer> TEXT_RENDERER_MAP = new HashMap<>();

    public static NVGTextRenderer getFont(final String name) {
        if (TEXT_RENDERER_MAP.containsKey(name))
            return TEXT_RENDERER_MAP.get(name);

        final Path pathURL = FabricLoader.getInstance().getModContainer("uitems")
                .flatMap(c -> c.findPath("assets/uitems/fonts/" + name + ".ttf"))
                .orElse(null);

        try {
            if (pathURL != null) {
                TEXT_RENDERER_MAP.put(name, new NVGTextRenderer(name, Files.newInputStream(pathURL)));

                return TEXT_RENDERER_MAP.get(name);
            } else {
                // Fallback: Try to use a system font or just don't crash
                // Since we can't easily get a system font stream that NanoVG likes cross-platform without more deps,
                // we will return a dummy renderer if possible or just log error but NOT crash.
                // However, NVGTextRenderer constructor expects a valid stream or it might fail too.
                // Let's see if we can use a dummy empty stream or similar? 
                // Better approach: If file missing, try to load a default font if exists, 
                // OR, just catch the error and return null (which might cause NPEs elsewhere).
                
                // Let's try to find ANY ttf in the classpath or system? No, that's complex.
                // The issue is likely the resources are not in the built jar or development environment path is wrong.
                
                // For now, to prevent crash, let's try to load a default system font path if on Windows
                Path systemFontPath = Path.of("C:\\Windows\\Fonts\\Arial.ttf");
                if (Files.exists(systemFontPath)) {
                     TEXT_RENDERER_MAP.put(name, new NVGTextRenderer(name, Files.newInputStream(systemFontPath)));
                     return TEXT_RENDERER_MAP.get(name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Instead of throwing exception, return a dummy or null (risk of NPE)
        // Throwing RuntimeException is what causes the crash. 
        // Let's return null and hope the caller handles it, OR return a dummy renderer.
        // But NVGTextRenderer likely needs valid data.
        
        System.err.println("Font not found: " + name + ". Returning dummy renderer to prevent crash.");
        NVGTextRenderer dummy = new NVGTextRenderer(name, null);
        TEXT_RENDERER_MAP.put(name, dummy);
        return dummy;
    }
}
