package ru.rpuxa.androidlib

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.ViewGroup

class LayoutSmartFragment : SmartFragment() {

    private var layout = 0

    @Constructor
    fun constructor(@LayoutRes id: Int) {
        layout = id
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?) =
            layoutInflater.inflate(layout, container)!!
}