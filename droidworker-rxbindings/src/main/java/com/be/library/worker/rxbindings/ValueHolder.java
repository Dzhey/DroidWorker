package com.be.library.worker.rxbindings;

/**
 * @author Eugene Byzov gdzhey@gmail.com
 *         Created on 08-Sep-16.
 */
class ValueHolder<T> {

    private Provider<T> mValueProvider;
    private boolean mIsSet;

    public static <T> ValueHolder<T> of(Provider<T> provider) {
        return new ValueHolder<>(provider);
    }

    public static <T> ValueHolder<T> of(T value) {
        return new ValueHolder<>(new ValueProvider<>(value));
    }

    public static <T> ValueHolder<T> of() {
        return new ValueHolder<>(new ValueProvider<T>(null));
    }

    private ValueHolder() {
    }

    private ValueHolder(Provider<T> provider) {
        mValueProvider = provider;
        mIsSet = true;
    }

    public T getValue() {
        return mValueProvider.get();
    }

    public void setValue(T value) {
        mValueProvider = new ValueProvider<>(value);
        mIsSet = true;
    }

    public boolean isSet() {
        return mIsSet;
    }
}
