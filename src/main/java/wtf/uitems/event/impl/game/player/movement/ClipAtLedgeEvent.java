package wtf.uitems.event.impl.game.player.movement;


public final class ClipAtLedgeEvent {

    private boolean updated, clip;

    public boolean isUpdated() {
        return updated;
    }

    public boolean isClip() {
        return clip;
    }

    public void setClip(final boolean clip) {
        this.updated = true;
        this.clip = clip;
    }

}
