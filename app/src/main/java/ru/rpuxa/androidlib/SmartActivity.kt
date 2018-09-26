package ru.rpuxa.androidlib

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import java.io.NotSerializableException
import java.io.Serializable
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

abstract class SmartActivity : AppCompatActivity() {

    protected abstract val contentView: Int

    protected abstract fun created()

    protected open fun resultActivity(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView)
        readMessages()
        if (intent.extras != null)
            setArgs(intent.extras)
        created()
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val kClass = codes[requestCode]
            if (kClass == null) {
                resultActivity(requestCode, resultCode, data)
                return
            }
            val arr = ArrayList<Any?>()

            var i = 0
            while (true) {
                val e = data!!.extras["$i"] ?: break
                arr.add(e)
                i++
            }

            if (arr.isEmpty())
                return

            (
                    javaClass.methods
                            .find { method ->
                                method.annotations.forEach {
                                    if (it is ResultFrom && it.activity == kClass)
                                        return@find true
                                }
                                false
                            } ?: throw IllegalStateException("Method resultFrom $kClass not found!")
                    ).invoke(this, *arr.toTypedArray())
        }
    }

    private fun setArgs(bundle: Bundle) {
        var i = 0
        while (true) {
            val arg = bundle.get("$i") ?: return
            getSetter(i).set(this, arg)
            i++
        }
    }

    private fun getSetter(i: Int): Field {
        for (field in javaClass.fields) {
            field.declaredAnnotations.find {
                it is Arg && it.number == i
            } ?: continue
            return field
        }
        throw IllegalStateException("field with number $i not found")
    }

    inline fun <reified A : Activity> startActivity(vararg args: Pair<String, *>?) =
            startActivity(putToIntent(args).setClass(this, A::class.java))


    inline fun <reified A : SmartActivity> startSmartActivity(vararg args: Any) =
            startActivity<A>(*compactArgs(args))

    inline fun <reified A : Activity> startActivityForResult(requestCode: Int, vararg args: Pair<String, *>?) =
            startActivityForResult(putToIntent(args).setClass(this, A::class.java), requestCode)

    val codes = HashMap<Int, KClass<*>>()
    val rand = Random()

    inline fun <reified A : SmartActivity> startSmartActivityForResult(vararg args: Any) {
        val request = rand.nextInt() ushr 16
        codes[request] = A::class
        startActivityForResult(putToIntent(compactArgs(args)).setClass(this, A::class.java), request)
    }

    protected fun finishSmart(vararg args: Any, resultOk: Boolean = true) {
        setResult(if (resultOk) Activity.RESULT_OK else Activity.RESULT_CANCELED, putToIntent(compactArgs(args)))
        finish()
    }


    fun putToIntent(args: Array<out Pair<String, *>?>): Intent {
        val i = Intent(this, Class::class.java)
        for (arg in args) {
            val (k, v) = arg!!
            if (v is Serializable)
                i.putExtra(k, v)
            else
                throw NotSerializableException()
        }
        return i
    }

    fun compactArgs(args: Array<*>): Array<Pair<String, *>?> {
        val arr = arrayOfNulls<Pair<String, *>>(args.size)
        for ((i, element) in args.withIndex()) {
            arr[i] = "$i" to element
        }
        return arr
    }


    private fun readMessages() {
        Thread {
            while (true) {
                while (deque.isEmpty())
                    if (isDestroyed)
                        return@Thread
                val (type, text, isShort) = deque.pollFirst()
                runOnUiThread {
                    when (type) {
                        TOAST -> Toast.makeText(this, text, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    companion object Actions {
        private val TOAST = 0
        //private val MESSAGE = 0

        private val deque = ArrayDeque<Msg>()

        fun toast(text: String, isShort: Boolean = true) {
            deque.addLast(Msg(TOAST, text, isShort))
        }
    }

    private data class Msg(val type: Int, val text: String, val isShort: Boolean)

    @Target(AnnotationTarget.FIELD)
    protected annotation class Arg(val number: Int)

    @Target(AnnotationTarget.FUNCTION)
    protected annotation class ResultFrom(val activity: KClass<*>)
}