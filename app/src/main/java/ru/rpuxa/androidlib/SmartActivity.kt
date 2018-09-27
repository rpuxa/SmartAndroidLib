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

    /**
     * Вместо setContentView(...)
     */
    protected abstract val contentView: Int?



    /**
     * Вызывается при создании активити
     */
    protected abstract fun created()

    /**
     * Вызывается при завершении активити вызванным startActivityForResult()
     */
    protected open fun resultActivity(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (contentView != null)
            setContentView(contentView!!)
        if (intent.extras != null)
            setArgs(intent.extras)
        created()
    }

    /**
     * Запустить не SmartActivity с аргументами
     */
    protected inline fun <reified A : Activity> startActivity(vararg args: Pair<String, *>) =
            startActivity(putToIntent(args).setClass(this, A::class.java))

    /**
     * Запустить SmartActivity с аргументами
     */
    protected inline fun <reified A : SmartActivity> startSmartActivity(vararg args: Any) =
            startActivity<A>(*compactArgs(args))


    protected inline fun <reified A : Activity> startActivityForResult(requestCode: Int, vararg args: Pair<String, *>) =
            startActivityForResult(putToIntent(args).setClass(this, A::class.java), requestCode)


    protected inline fun <reified A : SmartActivity> startSmartActivityForResult(vararg args: Any) {
        val request = rand.nextInt() ushr 16
        codes[request] = A::class
        startActivityForResult(putToIntent(compactArgs(args)).setClass(this, A::class.java), request)
    }

    /**
     * Завершить SmartActivity с аргументами
     */
    protected fun finishSmart(vararg args: Any, resultOk: Boolean = true) {
        setResult(if (resultOk) Activity.RESULT_OK else Activity.RESULT_CANCELED, putToIntent(compactArgs(args)))
        finish()
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

            (javaClass.methods.find { method ->
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

    val codes = HashMap<Int, KClass<*>>()

    val rand = Random()


    protected fun putToIntent(args: Array<out Pair<String, *>>) = Intent().apply {
        args.forEach { (k, v) -> putExtra(k, (v as? Serializable) ?: throw NotSerializableException()) }
    }

    protected fun compactArgs(args: Array<*>) = Array(args.size) { "$it" to args[it] }


    @Target(AnnotationTarget.FIELD)
    protected annotation class Arg(val number: Int)

    @Target(AnnotationTarget.FUNCTION)
    protected annotation class ResultFrom(val activity: KClass<*>)
}