package wtf.uitems.event.impl.client;

import wtf.uitems.client.feature.module.property.Property;

public record PropertyUpdateEvent(Property<?> property) {
}
