package wtf.uitems.event.impl.press;

import wtf.uitems.utility.KeyInput;

public final class MousePressEvent extends LWJGLInteractionEvent {

    public MousePressEvent(final int mouseKeyCode) {
        super(new KeyInput(mouseKeyCode, 0));
    }

}
