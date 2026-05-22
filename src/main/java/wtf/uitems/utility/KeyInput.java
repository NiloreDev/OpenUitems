package wtf.uitems.utility;

import org.lwjgl.glfw.GLFW;

public record KeyInput(int key, int modifiers) {
    public boolean hasCtrl() {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    public boolean hasShift() {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    public boolean isSelectAll() {
        return hasCtrl() && key == GLFW.GLFW_KEY_A;
    }

    public boolean isCopy() {
        return hasCtrl() && key == GLFW.GLFW_KEY_C;
    }

    public boolean isPaste() {
        return hasCtrl() && key == GLFW.GLFW_KEY_V;
    }

    public boolean isCut() {
        return hasCtrl() && key == GLFW.GLFW_KEY_X;
    }
}
