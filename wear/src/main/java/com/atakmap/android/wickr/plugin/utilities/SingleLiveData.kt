package com.atakmap.android.wickr.plugin.utilities

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom MutableLiveData class that only emits a value once.
 */
class SingleLiveData<T> : MutableLiveData<T>() {

    private val pendingValue = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(
            owner
        ) { stateData ->
            if (pendingValue.compareAndSet(true, false)) {
                observer.onChanged(stateData)
            }
        }
    }

    @MainThread
    override fun setValue(stateData: T?) {
        pendingValue.set(true)
        super.setValue(stateData)
    }

    override fun postValue(stateData: T?) {
        pendingValue.set(true)
        super.postValue(stateData)
    }
}
