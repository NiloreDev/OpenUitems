package wtf.uitems.event.impl.press;

import wtf.uitems.utility.KeyInput;

public class LWJGLInteractionEvent {

    private final KeyInput keyInput;

    protected LWJGLInteractionEvent(final KeyInput keyInput) {
        this.keyInput = keyInput;
    }

    public KeyInput getKeyInput() {
        return keyInput;
    }

    public int getInteractionCode() {
        return keyInput.key();
    }
}
