package wtf.uitems.client.feature.helper.impl.player.swing;

import wtf.uitems.client.feature.helper.IHelper;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.utility.misc.time.Stopwatch;

public final class SwingDelay implements IHelper {

    private final Stopwatch swingStopwatch = new Stopwatch();

    public static void reset() {
        instance.swingStopwatch.reset();
    }

    private static SwingDelay instance;

    public static void setInstance() {
        instance = new SwingDelay();
        EventDispatcher.subscribe(instance);
    }

    public static boolean isSwingAvailable(final CPSProperty cpsProperty, final boolean reset) {
        if (instance.swingStopwatch.hasTimeElapsed(cpsProperty.getNextClick())) {
            if (reset) {
                cpsProperty.resetClick();
                reset();
            }
            return true;
        }
        return false;
    }

    public static boolean isSwingAvailable(final CPSProperty cpsProperty) {
        return isSwingAvailable(cpsProperty, true);
    }
}
