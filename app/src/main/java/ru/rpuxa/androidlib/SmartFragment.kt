package ru.rpuxa.androidlib

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.io.NotSerializableException
import java.io.Serializable
import java.lang.IllegalStateException

abstract class SmartFragment : Fragment() {

    abstract fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val args = ArrayList<Any>()
        var i = 0
        do {
            val arg = arguments.get("$$$i")
            if (arg != null)
                args.add(arg)
            i++
        } while (arg != null)
        if (args.isNotEmpty()) {
            val constructor = javaClass.methods.find { it.getAnnotation(Constructor::class.java) != null }
                    ?: throw IllegalStateException("Constructor not found")
            constructor(this, *args.toTypedArray())
        }

        return onCreateView(inflater, container)
    }

    companion object {

        inline fun <reified T : SmartFragment> create(vararg args: Any): T {
            val fragment = T::class.java.constructors[0].newInstance() as SmartFragment
            return create(fragment, args) as T
        }

        fun create(fragment: SmartFragment, args: Array<out Any>) = fragment.apply {
            arguments =
                    Bundle().apply {
                        args.forEachIndexed { i, v ->
                            putSerializable("$$$i", (v as? Serializable)
                                    ?: throw NotSerializableException())
                        }
                    }
        }
    }

    protected annotation class Constructor
}