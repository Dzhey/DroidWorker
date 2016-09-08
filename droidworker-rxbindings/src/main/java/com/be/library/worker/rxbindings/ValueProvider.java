package com.be.library.worker.rxbindings;

/**
 * @author Eugene Byzov gdzhey@gmail.com
 *         Created on 09-Sep-16.
 */
class ValueProvider<T> implements Provider<T> {

    private final T mValue;

    ValueProvider(T value) {
        mValue = value;
    }

    @Override
    public T get() {
        return mValue;
    }
}
