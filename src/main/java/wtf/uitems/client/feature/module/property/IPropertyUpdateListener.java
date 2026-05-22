package wtf.uitems.client.feature.module.property;

public interface IPropertyUpdateListener<T> {
    void onChange(T prevValue, T value);
}
