package wtf.uitems.event.subscriber;

public interface IEventSubscriber {
    default boolean isHandlingEvents() {
        return true;
    }
}
